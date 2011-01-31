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

/** helper to package up ReplyTo address 
 */
case class Destination(exchange: String, key: String)

/** Anyone wanting to recieve messages from the bus need to implement this interface
 * TODO: factor MessageConsumer into a free function
 */
trait MessageConsumer {
  def receive(bytes: Array[Byte], replyTo: Option[Destination])
}

trait ChannelObserver {
  def online(broker: BrokerChannel)
  def offline()
}

trait BrokerConnectionListener {
  def closed() {}

  def opened() {}
}

/**
 * Primary class for interfacing with a broker, a new binding to a bus needs to implement all of the functions.
 * In AMQP, channels are parallel entities on a connection, any one channel needs to be manipulated only from a 
 * single thread.
 */
trait BrokerChannel {
  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit

  def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[Destination] = None)

  def listen(queue: String, mc: MessageConsumer)

  def close()
}

trait BrokerConnection {

  // query the state of the connection
  def isConnected: Boolean

  // connects to broker, throws an exception if unsuccessful
  def connect()

  /// cleanup and stop all channels on this connection, block until completed
  def close(): Unit

  /// create a new single-thread only interface object that provides low level access to the amqp broker
  def newBrokerChannel(): BrokerChannel

  /// sets the connection listener
  def setConnectionListener(l: Option[BrokerConnectionListener]) = { listener = l }

  /// option to hold the connection listener
  protected var listener: Option[BrokerConnectionListener] = None

}