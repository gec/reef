package org.totalgrid.reef.messaging.synchronous

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

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.japi.Envelope._
import java.util.UUID
import org.totalgrid.reef.broker.api._
import net.agileautomata.executor4s._

/**
 * Synchronizes and correlates the send/receive operations on a ProtoServiceChannel
 *
 * @param timeoutms The timeout in milliseconds for a service request
 * @param channel The ProtoServiceChannel on which requests and responses will be received
 *
 */
class ResponseCorrelator(executor: Executor) extends Logging with MessageConsumer {

  type Response = Option[ServiceResponse]
  type ResponseCallback = Response => Unit

  // use uuid to map these
  private def getNextUuid() = UUID.randomUUID().toString

  /// mutable state
  private val map = mutable.Map.empty[String, Tuple3[Cancelable, ResponseCallback, Long]]

  def register(callback: ResponseCallback, timeoutms: Long): String = map.synchronized {
    val uuid = getNextUuid()
    val timer = executor.delay(timeoutms.milliseconds)(onTimeout(uuid))
    map.put(uuid, (timer, callback, timeoutms))
    uuid
  }

  // terminates all existing registered callbacks with an exception
  def flush() = map.synchronized {
    map.foreach {
      case (_, (timer, callback, _)) =>
        timer.cancel()
        executor.execute(callback(None))
    }
    map.clear()
  }

  private def onTimeout(uuid: String) = map.synchronized {
    map.get(uuid) match {
      case Some((timer, callback, timeout)) =>
        map.remove(uuid)
        executor.execute(callback(None))
      case None =>
        logger.warn("Unexpected service response timeout w/ uuid: " + uuid)
    }
  }

  def receive(bytes: Array[Byte], replyTo: Option[Destination]) = {
    try {
      onResponse(ServiceResponse.parseFrom(bytes))
    } catch {
      case ex: Exception => logger.error("Failed to parse service response", ex)
    }
  }

  private def onResponse(rsp: ServiceResponse) = map.synchronized {
    map.get(rsp.getId) match {
      case Some((timer, callback, _)) =>
        timer.cancel()
        map.remove(rsp.getId)
        executor.execute(callback(Some(rsp)))
      case None =>
        logger.warn("Unexpected request response w/ uuid: " + rsp.getId)
    }
  }

}

