/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging

import scala.actors.Actor._

import scala.collection.mutable

import org.totalgrid.reef.util.{ Logging, Timer }
import org.totalgrid.reef.protoapi.Envelope._

/**
 * Synchronizes and correlates the send/receive operations on a ProtoServiceChannel
 *
 * @param timeoutms The timeout in milliseconds for a service request
 * @param channel The ProtoServiceChannel on which requests and responses will be received
 *
 */
class ServiceResponseCorrelator(timeoutms: Long, channel: ProtoServiceChannel) extends Logging {

  type ResponseCallback = Option[ServiceResponse] => Unit

  channel.setResponseDest(receive)

  def close() = channel.close()

  private var seq = 0
  private def next: Int = { seq += 1; seq }

  /// mutable state
  private val map = mutable.Map.empty[String, Tuple2[Timer, ResponseCallback]]

  /**	Sends a request with an asynchronous callback
   *	 
   *	@param request ServiceRequest proto to send
   *	@param callback function to callback with an Option[ServiceResponse] 
   */
  def send(requestRaw: ServiceRequest.Builder, exchange: String, key: String, callback: ResponseCallback) {

    val request = map.synchronized {
      requestRaw.setId(next.toString) //set the correlation id
      val builtRequest = requestRaw.build
      val timer = Timer.delay(timeoutms) { timeout(builtRequest.getId) }
      map.put(builtRequest.getId, (timer, callback))
      builtRequest
    }

    channel.send(request, exchange, key)
  }

  private def timeout(id: String) = map.synchronized {
    map.get(id) match {
      case Some((timer, callback)) =>
        map.remove(id)
        callback(None)
      case None =>
    }
  }

  // TODO: send error back immediatley if response channel goes down
  private def receive(rsp: ServiceResponse) = map.synchronized {
    map.get(rsp.getId) match {
      case Some((timer, callback)) =>
        timer.cancel()
        map.remove(rsp.getId)
        callback(Some(rsp))
      case None =>
        warn("got unexpected request response: " + rsp.getId)
    }
  }

}

