/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.impl

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.broker.{ BrokerMessage, BrokerMessageConsumer }
import org.totalgrid.reef.client.proto.Envelope.ServiceResponse
import java.util.UUID
import scala.collection.mutable
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.proto.Envelope
import scala.Left

trait ResponseCorrelator extends BrokerMessageConsumer {
  def register(timeoutMs: Long, callback: Either[ResponseCorrelator.Failure, ServiceResponse] => Unit): String

  def fail(uuid: String, failure: ResponseCorrelator.Failure)

  def close()

}

object ResponseCorrelator {

  object Failure {
    def apply(status: Envelope.Status, msg: String) = new Failure(status, msg)
  }
  class Failure(val status: Envelope.Status, val msg: String)
  case object DisconnectFailure extends Failure(Envelope.Status.BUS_UNAVAILABLE, "Graceful close")
  case class TimeoutFailure(interval: TimeInterval) extends Failure(Envelope.Status.RESPONSE_TIMEOUT, "Timed out waiting for response after: " + interval)

  def apply(exe: Executor): ResponseCorrelator = new ResponseCorrelatorImpl(exe)

  class ResponseCorrelatorImpl(exe: Executor) extends ResponseCorrelator with BrokerMessageConsumer with Logging {

    type Result = Either[Failure, ServiceResponse]
    type ResultCallback = Result => Unit

    private def nextUuid = UUID.randomUUID().toString

    /// mutable state
    private case class Record(timer: Cancelable, callback: ResultCallback, interval: TimeInterval)
    private val map = mutable.Map.empty[String, Record]

    def register(timeoutMs: Long, callback: ResultCallback): String = map.synchronized {
      val uuid = nextUuid
      val interval = timeoutMs.milliseconds
      val timer = exe.schedule(interval)(onTimeout(uuid))
      map.put(uuid, Record(timer, callback, interval))
      uuid
    }

    def fail(uuid: String, failure: Failure) {
      map.synchronized(map.remove(uuid)) match {
        case Some(Record(timer, callback, _)) =>
          timer.cancel()
          doCallback(callback, Left(failure))
        case None =>
          logger.warn("Couldn't fail unknown uuid: " + uuid)
      }
    }

    // terminates all existing registered callbacks with an exception
    def close() {
      map.synchronized {
        if (map.size > 0) logger.warn("Closing response corrolater with some outstanding requests: " + map.size)
        // collect the callbacks to fail, then clear the map
        val values = map.values.toList
        map.clear()
        values
      }.foreach {
        case Record(timer, callback, _) =>
          timer.cancel()
          doCallback(callback, Left(DisconnectFailure))
      }
    }

    private def onTimeout(uuid: String) {
      map.synchronized(map.remove(uuid)) match {
        case Some(Record(timer, callback, interval)) =>
          doCallback(callback, Left(TimeoutFailure(interval)))
        case None =>
          logger.warn("Unexpected service response timeout w/ uuid: " + uuid)
      }
    }

    def onMessage(msg: BrokerMessage) {
      try {
        onResponse(ServiceResponse.parseFrom(msg.bytes))
      } catch {
        case ex: Exception => logger.error("Failed to parse service response", ex)
      }
    }

    private def onResponse(rsp: ServiceResponse) {
      map.synchronized(map.remove(rsp.getId)) match {
        case Some(Record(timer, callback, _)) =>
          timer.cancel()
          doCallback(callback, Right(rsp))
        case None =>
          logger.warn("Unexpected request response w/ uuid: " + rsp.getId)
      }
    }

    private def doCallback(callback: ResultCallback, result: Result) {
      exe.execute { callback(result) }
    }
  }

}
