package org.totalgrid.reef.broker.api

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
import org.totalgrid.reef.japi.client.ConnectionListener

object BrokerState extends Enumeration {
  type BrokerState = Value
  val Connected = Value("Connection is open")
  val Closed = Value("Connection closed locally")
  val Disconnected = Value("Connection disconnected unexpectedly")
}

trait BrokerConnection extends BrokerChannelPool {

  private var state = BrokerState.Closed

  /// Set of connection listeners, also used as a mutex
  private val mutex = new Object
  private var listeners = Set.empty[ConnectionListener]

  /**
   * query the state of the connection
   *  @return True if connected, false otherwise
   */

  final def isConnected: Boolean = state == BrokerState.Connected

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
  def newChannel(): BrokerChannel

  /// sets the connection listener
  final def addListener(listener: ConnectionListener) = mutex.synchronized {
    listeners += listener
  }

  final def removeListener(listener: ConnectionListener) = mutex.synchronized {
    listeners -= listener
  }

  final protected def setState(newState: BrokerState.Value) = {
    mutex.synchronized { state = newState }
    newState match {
      case BrokerState.Connected => listeners.foreach(_.onConnectionOpened())
      case BrokerState.Disconnected => listeners.foreach(_.onConnectionClosed(false))
      case BrokerState.Closed => listeners.foreach(_.onConnectionClosed(true))
    }
  }

}