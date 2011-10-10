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
package org.totalgrid.reef.broker.memory

import java.util.UUID
import org.totalgrid.reef.broker.newapi._
import net.agileautomata.executor4s.Executor

final class MemoryBrokerConnectionFactory(exe: Executor) extends BrokerConnectionFactory {
  import MemoryBrokerState._
  private var state = State()

  def update(fun: MemoryBrokerState.State => MemoryBrokerState.State) = synchronized(state = fun(state))
  def getState: MemoryBrokerState.State = state

  def connect = new MemoryBrokerConnection

  class MemoryBrokerConnection extends BrokerConnection {

    private var open = true
    private var queues: List[String] = Nil

    def disconnect() = {
      open = false
      update { s =>
        queues.foldLeft(s)((old, q) => old.dropQueue(q))
      }
      true
    }

    def throwIfClosed[A](fun: => A): A = {
      if (!open) throw new Exception("Connection is closed")
      else fun
    }

    def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String = throwIfClosed {
      val name = if (queue == "*") UUID.randomUUID().toString else queue
      update(s => s.declareQueue(name, exe))
      queues = name :: queues
      name
    }

    def declareExchange(name: String, typ: String = "topic"): Unit = throwIfClosed {
      update(_.declareExchange(name, typ))
    }

    def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit = throwIfClosed {
      update(_.bindQueue(queue, exchange, key, unbindFirst))
    }

    def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit = throwIfClosed {
      update(_.unbindQueue(queue, exchange, key))
    }

    def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[BrokerDestination] = None) = throwIfClosed {
      update(state => state.publish(exchange, key, BrokerMessage(b, replyTo)))
    }

    class Subscription(queue: String) extends BrokerSubscription {
      def close() = update(_.dropQueue(queue))
      def start(consumer: BrokerMessageConsumer): BrokerSubscription = {
        update(_.listen(queue, consumer))
        this
      }
      def getQueue: String = queue
    }

    def listen(queue: String): BrokerSubscription = {
      update(_.declareQueue(queue, exe))
      new Subscription(queue)

    }

    def listen(): BrokerSubscription = listen(declareQueue())
  }

}

