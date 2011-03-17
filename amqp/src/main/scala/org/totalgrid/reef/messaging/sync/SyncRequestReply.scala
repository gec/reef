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
package org.totalgrid.reef.messaging.sync

import org.totalgrid.reef.messaging._

import org.totalgrid.reef.api.Envelope

class ProtoSyncRequestReply(channel: BrokerChannel)
  extends SyncRequestReply[Envelope.ServiceRequest, Envelope.ServiceResponse](
    channel,
    ProtoSerializer.convertProtoToBytes,
    Envelope.ServiceResponse.parseFrom) with ProtoServiceChannel

/**
 * combines a response queue and a publisher into one class that provides implements the
 *  RequestReplyChannel interface primarily used by service clients
 */
class SyncRequestReply[S, R](
  channel: BrokerChannel,
  serialize: S => Array[Byte],
  deseralize: Array[Byte] => R)
    extends MsgPublisher(channel) with RequestReplyChannel[S, R] with MessageConsumer {

  /**
   * Close the underlying channel. No further requests or responses are possible.
   */
  override def close() = channel.close()

  /// where to send the received data, optional to break circular construction dependency, will blow
  /// up if used without setting the destination
  private var dest: Option[R => Unit] = None

  /// Here's the subscription that gets setup synchronously
  private val queue = QueuePatterns.getPrivateResponseQueue(channel, "amq.direct", this)

  /// Set's the publisher's reply to field
  this.setReplyTo(Destination("amq.direct", queue))

  def send(value: S, exchange: String, key: String): Unit = send(serialize(value), exchange, key) //call publishers send

  def setResponseDest(x: R => Unit) = dest = Some(x)

  def receive(bytes: Array[Byte], replyTo: Option[Destination]) = dest match {
    case Some(f) =>
      try {
        f(deseralize(bytes)) //forward the deserialized response somewhere else
      } catch { case ex: Exception => error(ex) }
    case None => error("Response callback has not been set")
  }

}
