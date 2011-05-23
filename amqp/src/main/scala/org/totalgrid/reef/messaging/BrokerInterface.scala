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

import org.totalgrid.reef.api.IConnectionListener
import scala.collection.mutable.Set

/**
 * helper to package up ReplyTo address
 */
case class Destination(exchange: String, key: String)

/**
 * Anyone wanting to recieve messages from the bus need to implement this interface
 * TODO: factor MessageConsumer into a free function
 */
trait MessageConsumer {
  def receive(bytes: Array[Byte], replyTo: Option[Destination])
}

trait ChannelObserver extends BrokerChannelCloseListener {
  def online(broker: BrokerChannel)
}

trait BrokerChannelCloseListener {
  def onClosed(channel: BrokerChannel, expected: Boolean)
}

/**
 * Primary class for interfacing with a broker, a new binding to a bus needs to implement all of the functions.
 * In AMQP, channels are parallel entities on a connection, any one channel needs to be manipulated only from a
 * single thread.
 */
trait BrokerChannel extends BrokerChannelCloseProvider {
  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit

  def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[Destination] = None)

  def listen(queue: String, mc: MessageConsumer)

  def start()

  def close()
}

/**
 * thick trait for implementing add/remove/notify behavior for BrokerChannelCloseListeners
 */
trait BrokerChannelCloseProvider { self: BrokerChannel =>

  private var brokerClosedListeners = List.empty[BrokerChannelCloseListener]

  def addCloseListener(listener: BrokerChannelCloseListener) = brokerClosedListeners.synchronized {
    brokerClosedListeners = listener :: brokerClosedListeners
  }

  def removeCloseListener(listener: BrokerChannelCloseListener) = brokerClosedListeners.synchronized {
    brokerClosedListeners = brokerClosedListeners.filterNot(_ != listener)
  }

  /**
   * BrokerChannel implimentations need to call this callback when terminated
   */
  protected def onClose(expected: Boolean) = brokerClosedListeners.synchronized {
    brokerClosedListeners.foreach(_.onClosed(this, expected))
  }
}

trait BrokerConnection {

  private var connected = false

  /// Set of connection listeners, also used as a mutex
  private val listeners = Set.empty[IConnectionListener]

  /**
   * query the state of the connection
   *  @return True if connected, false otherwise
   */

  final def isConnected: Boolean = connected

  /**
   * Idempotent, blocking connect function
   *
   * @return True if the attempt was successful, false otherwise
   */
  def connect(): Boolean

  /**
   * Idempotent, blocking disconnect function. All created channels are invalidated and closed.
   *
   * @return True if the attempt was successful, false otherwise
   */
  def disconnect(): Boolean

  /// create a new single-thread only interface object that provides low level access to the amqp broker
  def newBrokerChannel(): BrokerChannel

  /// sets the connection listener
  final def addListener(listener: IConnectionListener) = listeners.synchronized {
    listeners += listener
  }

  final def removeListener(listener: IConnectionListener) = listeners.synchronized {
    listeners -= listener
  }

  final protected def setOpen() = listeners.synchronized {
    connected = true
    listeners.foreach(_.onConnectionOpened())
  }

  final protected def setClosed(expected: Boolean) = listeners.synchronized {
    connected = false
    listeners.foreach(_.onConnectionClosed(expected))
  }

}