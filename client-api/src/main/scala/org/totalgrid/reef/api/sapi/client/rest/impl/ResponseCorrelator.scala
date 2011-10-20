package org.totalgrid.reef.api.sapi.client.rest.impl

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
import scala.collection.mutable

import org.totalgrid.reef.api.japi.Envelope._
import java.util.UUID
import net.agileautomata.executor4s._
import org.totalgrid.reef.broker.{ BrokerMessageConsumer, BrokerMessage }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.api.japi.Envelope
import org.totalgrid.reef.api.sapi.client.{ ResponseTimeout, FailureResponse }

/**
 * Synchronizes and correlates the send/receive operations on a ProtoServiceChannel
 *
 * @param timeoutms The timeout in milliseconds for a service request
 * @param channel The ProtoServiceChannel on which requests and responses will be received
 *
 */
class ResponseCorrelator extends Logging with BrokerMessageConsumer {

  type Response = Either[FailureResponse, ServiceResponse]
  type ResponseCallback = Response => Unit

  // use uuid to map these
  private def nextUuid = UUID.randomUUID().toString

  /// mutable state
  private case class Record(timer: Cancelable, callback: ResponseCallback, executor: Executor)
  private val map = mutable.Map.empty[String, Record]

  def register(executor: Executor, interval: TimeInterval, callback: ResponseCallback): String = map.synchronized {
    val uuid = nextUuid
    val timer = executor.delay(interval)(onTimeout(uuid))
    map.put(uuid, Record(timer, callback, executor))
    uuid
  }

  def fail(uuid: String, response: FailureResponse) = map.synchronized {
    map.remove(uuid) match {
      case Some(Record(timer, callback, executor)) =>
        timer.cancel()
        callback(Left(response))
      case None =>
        logger.warn("Couldn't fail unknown uuid: " + uuid)
    }
  }

  // terminates all existing registered callbacks with an exception
  def close() = map.synchronized {
    map.values.foreach {
      case Record(timer, callback, executor) =>
        timer.cancel()
        executor.execute(callback(Left(FailureResponse(Envelope.Status.BUS_UNAVAILABLE, "Graceful close"))))
    }
    map.clear()
  }

  private def onTimeout(uuid: String) = map.synchronized {
    map.get(uuid) match {
      case Some(Record(timer, callback, executor)) =>
        map.remove(uuid)
        callback(Left(ResponseTimeout))
      case None =>
        logger.warn("Unexpected service response timeout w/ uuid: " + uuid)
    }
  }

  def onMessage(msg: BrokerMessage) = {
    try {
      onResponse(ServiceResponse.parseFrom(msg.bytes))
    } catch {
      case ex: Exception => logger.error("Failed to parse service response", ex)
    }
  }

  private def onResponse(rsp: ServiceResponse) = map.synchronized {
    map.get(rsp.getId) match {
      case Some(Record(timer, callback, _)) =>
        timer.cancel()
        map.remove(rsp.getId)
        callback(Right(rsp))
      case None =>
        logger.warn("Unexpected request response w/ uuid: " + rsp.getId)
    }
  }

}

