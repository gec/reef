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
import org.totalgrid.reef.broker.api._
import net.agileautomata.executor4s.Executors

class MemoryBrokerConnection extends BrokerConnection {

  import MemoryBrokerState._

  private var state = State()
  private val exe = Executors.newScheduledSingleThread

  def shutdown() = exe.shutdown()

  def update(fun: MemoryBrokerState.State => MemoryBrokerState.State) = synchronized {
    state = fun(state)
  }

  def getState: MemoryBrokerState.State = state

  def throwIfDisconnected[A](fun: => A): A = {
    if (!this.isConnected) throw new Exception("Connection is closed")
    else fun
  }

  def connect(): Boolean = {
    this.setState(BrokerState.Connected)
    true
  }

  def disconnect(): Boolean = {
    this.setState(BrokerState.Closed)
    true
  }

  def newChannel(): BrokerChannel = throwIfDisconnected(new MemoryBrokerChannel)

  // same as other implementation, not thread safe. makes calls to STM State
  private class MemoryBrokerChannel extends BrokerChannel {

    def throwIfClosed[A](fun: => A): A = {
      if (!open) throw new Exception("Channel has been closed, operation not permitted")
      fun
    }

    private var queues: List[String] = Nil
    private var open = true

    def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String = throwIfClosed {
      val name = if (queue == "*") UUID.randomUUID().toString else queue
      update(s => s.declareQueue(name, exe))
      queues = name :: queues
      name
    }

    def declareExchange(name: String, typ: String = "topic"): Unit =
      throwIfClosed(update(_.declareExchange(name, typ)))

    def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false): Unit =
      throwIfClosed(update(_.bindQueue(queue, exchange, key, unbindFirst)))

    def unbindQueue(queue: String, exchange: String, key: String = "#"): Unit =
      throwIfClosed(update(_.unbindQueue(queue, exchange, key)))

    def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[Destination] = None) =
      throwIfClosed(update(state => state.publish(exchange, key, Message(b, replyTo))))

    def listen(queue: String, mc: MessageConsumer) =
      throwIfClosed(update(state => state.listen(queue, mc)))

    def close() = {
      val wasOpen = open
      open = false
      if (wasOpen) {
        this.onClose(true)
        update { s =>
          queues.foldLeft(s)((old, q) => old.dropQueue(q))
        }
      }
    }

    def isOpen: Boolean = false
  }
}

