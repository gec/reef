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
package org.totalgrid.reef.messaging.sync

import org.totalgrid.reef.messaging._

import org.totalgrid.reef.sapi.client.{ Subscription, Event }
import org.totalgrid.reef.sapi.service.AsyncService
import org.totalgrid.reef.sapi.{ AnyNodeDestination, Destination }
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.broker.{ BrokerChannel, CloseableChannel }

trait AMQPSyncFactory extends AMQPConnectionReactor with ClientSessionFactory {
  import AMQPMessageConsumers._
  /**
   * creates a correlator channel that can multiplex ServiceRequests to different exchanges and collect the inputs
   * and demultiplexes them back to the right clients.
   */
  def getServiceResponseCorrelator(timeoutms: Long): ServiceResponseCorrelator = {
    val channel = this.getChannel()
    val reqReply = new ProtoSyncRequestReply(channel)
    new ServiceResponseCorrelator(timeoutms, reqReply)
  }

  def prepareSubscription[A](deserialize: Array[Byte] => A): Subscription[A] = {
    val channel = getChannel()
    def getConsumer(consumer: Event[A] => Unit) = makeEventConsumer(deserialize, consumer)
    new SyncSubscription[A](channel, getConsumer)
  }

  def broadcast[A](channel: BrokerChannel, serialize: A => Array[Byte]): (A, String, String) => Unit = {
    def broadcaster(x: A, ex: String, key: String): Unit = {
      channel.publish(ex, key, serialize(x))
    }
    broadcaster _
  }

  /**
   * experimental synchronous bindService call
   * @param executor it is mandatory to dispatch incoming messages to an executor. If we handle the messsage on the
   *                 connection thread and make any sort of call to a broker channel it will deadlock the connection.
   *                 If using the MockBroker an InstantReactor can be passed in to keep tests single threaded
   */
  def bindService(exchange: String, service: AsyncService.ServiceFunction, executor: Executor, destination: Destination = AnyNodeDestination, competing: Boolean = false): CloseableChannel = {

    logger.info("bindService(): exchange: " + exchange + ", service: " + service + ", destination: " + destination + ", competing: " + competing)
    val channel = getChannel()

    val replyPublisher = broadcast[Envelope.ServiceResponse](channel, (x: Envelope.ServiceResponse) => x.toByteArray)
    val binding = dispatchToReactor(executor, makeServiceBinding(replyPublisher, service))

    new SyncServiceBinding(channel, exchange, destination, competing, binding)
  }
}
