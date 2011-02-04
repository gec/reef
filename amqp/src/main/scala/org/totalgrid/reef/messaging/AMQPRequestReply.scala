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

import org.totalgrid.reef.proto.Envelope._

/** trait used to present a simple interface to a request/reply interface as a 
 * simple async channel
 */
trait RequestReplyChannel[R, S] {
  def send(request: R, exchange: String, key: String)
  def setResponseDest(response: S => Unit)
  def close(): Unit
}

/**
 * @param exchange request exchange
 * @param key      request key
 * @param responseExchange exchange to bind queue for responses to, usually amq.direct.
 */
abstract class AMQPServiceRequestReply(responseExchange: String)
  extends AMQPRequestReply[ServiceRequest, ServiceResponse](
    responseExchange,
    ProtoSerializer.convertProtoToBytes,
    ServiceResponse.parseFrom) with ProtoServiceChannel

trait ProtoServiceChannel extends RequestReplyChannel[ServiceRequest, ServiceResponse]

/** combines a response queue and a publisher into one class that provides implements the
 * RequestReplyChannel interface primarily used by service clients
 */
abstract class AMQPRequestReply[S, R](responseExchange: String, serialize: S => Array[Byte], deseralize: Array[Byte] => R)
    extends AMQPPublisher(Nil) with RequestReplyChannel[S, R] with MessageConsumer {

  override def close(): Unit = throw new Exception("Unimplemented")

  /// where to send the received data, optional to break circular construction dependency, will blow
  /// up if used without setting the destination
  private var dest: Option[R => Unit] = None

  /// private queue we will recieve responses by setting the AMQP reply address
  private val responseQueue = new AMQPPrivateResponseQueueListener(responseExchange, this)
  responseQueue.observe { (online, queueName) => setReplyTo(new Destination(responseExchange, queueName)) }

  def send(value: S, exchange: String, key: String): Unit = {
    send(serialize(value), exchange, key)
  }

  def setResponseDest(x: R => Unit) {
    dest = Some(x)
  }

  /** Overrides the online function to setup the subscriber BEFORE the publisher.
   */
  override def online(b: BrokerChannel) {
    // This ordering is very important for avoiding race conditions!!
    responseQueue.online(b)
    super.online(b)
  }

  override def offline() {
    responseQueue.offline()
    super.offline()
  }

  def receive(bytes: Array[Byte], replyTo: Option[Destination]) {
    try {
      dest.get(deseralize(bytes))
    } catch { case ex: Exception => warn(ex) }
  }
}
