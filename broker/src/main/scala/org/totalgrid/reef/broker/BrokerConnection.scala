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
package org.totalgrid.reef.broker

object BrokerState extends Enumeration {
  type BrokerState = Value
  val Connected = Value("Connection is open")
  val Closed = Value("Connection closed locally")
  val Disconnected = Value("Connection disconnected unexpectedly")
}

case class BrokerDestination(exchange: String, key: String)

case class BrokerMessage(bytes: Array[Byte], replyTo: Option[BrokerDestination])

trait BrokerConnection {

  /**
   * Idempotent, blocking disconnect function. All created channels are invalidated and closed.
   *
   * @return True if the attempt was successful, false otherwise
   */
  def disconnect(): Boolean

  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit

  def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit

  def publish(exchange: String, key: String, bytes: Array[Byte], replyTo: Option[BrokerDestination] = None): Unit

  /**
   * Listen to a named, non-exclusive system queue (competing consumer)
   */
  def listen(queue: String): BrokerSubscription

  /*
    * Listen to a newly created queue exclusive, ephemeral queue
    */
  def listen(): BrokerSubscription

  /// Set of connection listeners, also used as a mutex
  protected val mutex = new Object
  private var listeners = Set.empty[BrokerConnectionListener]

  final protected def onDisconnect(expected: Boolean) = listeners.foreach(_.onDisconnect(expected))

  final def addListener(listener: BrokerConnectionListener) = mutex.synchronized(listeners += listener)
  final def removeListener(listener: BrokerConnectionListener) = mutex.synchronized(listeners -= listener)
}