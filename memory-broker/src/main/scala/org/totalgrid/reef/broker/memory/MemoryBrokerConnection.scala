/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.broker.memory

import java.util.UUID
import org.totalgrid.reef.broker._
import net.agileautomata.executor4s.Executor

import collection.immutable.Set
import org.totalgrid.reef.client.exception.ServiceIOException

class MemoryBrokerConnection(factory: MemoryBrokerConnectionFactory, exe: Executor) extends BrokerConnection {

  private var state = ConnectionState(Some(factory), Set.empty[String])
  private def update(modify: ConnectionState => ConnectionState) = mutex.synchronized(state = modify(state))

  case class ConnectionState(fac: Option[MemoryBrokerConnectionFactory], queues: Set[String]) {

    def factory = fac match {
      case Some(x) => x
      case None => throw new ServiceIOException("Connection is closed")
    }

    def declareQueue(name: String): ConnectionState = {
      factory.update(s => s.declareQueue(name, exe))
      this.copy(queues = queues + name)
    }

    def disconnect: ConnectionState = fac match {
      case Some(f) =>
        f.update(s => queues.foldLeft(s)((old, q) => old.dropQueue(q)))
        onDisconnect(true)
        ConnectionState(None, Set.empty[String])
      case None =>
        this
    }
  }

  def disconnect() = {
    update(_.disconnect)
    true
  }
  def isConnected() = state.fac.isDefined

  def declareQueue(queue: String = "*", autoDelete: Boolean = true, exclusive: Boolean = true): String = {
    val name = if (queue == "*") UUID.randomUUID().toString else queue
    update(_.declareQueue(name))
    name
  }

  def declareExchange(name: String, typ: String = "topic") =
    state.factory.update(_.declareExchange(name, typ))

  def bindQueue(queue: String, exchange: String, key: String = "#", unbindFirst: Boolean = false) =
    state.factory.update(_.bindQueue(queue, exchange, key, unbindFirst))

  def unbindQueue(queue: String, exchange: String, key: String = "#") =
    state.factory.update(_.unbindQueue(queue, exchange, key))

  def publish(exchange: String, key: String, b: Array[Byte], replyTo: Option[BrokerDestination] = None) =
    state.factory.update(state => state.publish(exchange, key, BrokerMessage(b, replyTo)))

  class Subscription(queue: String) extends BrokerSubscription {
    def close() = factory.update(_.dropQueue(queue))
    def start(consumer: BrokerMessageConsumer): BrokerSubscription = {
      state.factory.update(_.listen(queue, consumer))
      this
    }
    def getQueue: String = queue
  }

  def listen(queue: String): BrokerSubscription = {
    state.factory.update(_.declareQueue(queue, exe))
    new Subscription(queue)

  }

  def listen(): BrokerSubscription = listen(declareQueue())
}
