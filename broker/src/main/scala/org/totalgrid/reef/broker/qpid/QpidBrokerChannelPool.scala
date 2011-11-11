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
package org.totalgrid.reef.broker.qpid

import org.totalgrid.reef.broker._
import org.totalgrid.reef.clientapi.exceptions.ServiceIOException

trait QpidBrokerChannelPool extends BrokerConnection {

  protected def newChannel(): QpidWorkerChannel

  private var closed = false
  private val channels = collection.mutable.Queue.empty[QpidWorkerChannel]
  private val beingUsed = collection.mutable.Queue.empty[QpidWorkerChannel]

  protected def execute[A](fun: QpidWorkerChannel => A): A = {

    val channel = borrow()
    try {
      fun(channel)
    } finally {
      unborrow(channel)
    }
  }

  private def borrow(): QpidWorkerChannel = channels.synchronized {
    if (closed) throw new ServiceIOException("Connection closed")
    val channel = if (!channels.isEmpty) channels.dequeue()
    else newChannel()
    beingUsed.enqueue(channel)
    channel
  }

  private def unborrow(channel: QpidWorkerChannel): Unit = channels.synchronized {
    if (channel.isOpen) channels.enqueue(channel)
    beingUsed.dequeueFirst(_ == channel)
  }

  protected def closeWorkerChannels(): Unit = channels.synchronized {
    closed = true
    beingUsed.foreach { _.close() }
    channels.foreach { _.close() }
    channels.clear()
  }

  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String =
    execute(_.declareQueue(queue, autoDelete, exclusive))

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit =
    execute(_.declareExchange(exchange, exchangeType))

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit =
    execute(_.bindQueue(queue, exchange, key, unbindFirst))

  def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit =
    execute(_.unbindQueue(queue, exchange, key))

  def publish(exchange: String, key: String, bytes: Array[Byte], replyTo: Option[BrokerDestination] = None) =
    execute(_.publish(exchange, key, bytes, replyTo))

}