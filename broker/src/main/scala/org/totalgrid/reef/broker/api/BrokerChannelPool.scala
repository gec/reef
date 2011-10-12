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
trait BrokerChannelPool {

  protected def newChannel(): BrokerChannel

  private val channels = collection.mutable.Queue.empty[BrokerChannel]

  protected def execute[A](fun: BrokerChannel => A): A = {

    val channel = borrow()
    try {
      fun(channel)
    } finally {
      unborrow(channel)
    }
  }

  private def borrow(): BrokerChannel = channels.synchronized {
    if (channels.size > 0) channels.dequeue()
    else newChannel()
  }

  private def unborrow(channel: BrokerChannel): Unit = channels.synchronized {
    if (channel.isOpen) channels.enqueue(channel)
  }

  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String =
    execute(_.declareQueue(queue, autoDelete, exclusive))

  def declareExchange(exchange: String, exchangeType: String = "topic"): Unit =
    execute(_.declareExchange(exchange, exchangeType))

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit =
    execute(_.bindQueue(queue, exchange, key, unbindFirst))

  def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit =
    execute(_.unbindQueue(queue, exchange, key))

  def publish(exchange: String, key: String, bytes: Array[Byte], replyTo: Option[Destination] = None) =
    execute(_.publish(exchange, key, bytes, replyTo))

  def listen(queue: String, mc: MessageConsumer): Unit = {
    execute(_.listen(queue, mc))
  }

}