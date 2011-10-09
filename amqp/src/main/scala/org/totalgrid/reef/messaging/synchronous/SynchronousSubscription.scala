package org.totalgrid.reef.messaging.synchronous

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
import org.totalgrid.reef.sapi.client.{ Subscription, Event }
import org.totalgrid.reef.messaging.{ AMQPMessageConsumers, QueuePatterns }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.api.{ Destination, MessageConsumer, BrokerChannel }

/**
 * synchronous subscription object, allows canceling and a delayed starting
 */
class SynchronousSubscription[A](channel: BrokerChannel, executor: Executor, deserialize: Array[Byte] => A) extends Subscription[A] {

  private val queueName = QueuePatterns.getLateBoundPrivateUnboundQueue(channel)

  override def id() = queueName
  override def cancel() = channel.close()

  def start(callback: Event[A] => Unit): Subscription[A] = {
    val consumer = AMQPMessageConsumers.makeEventConsumer(deserialize, callback)
    val proxy = new MessageConsumer {
      def receive(bytes: Array[Byte], replyTo: Option[Destination]) = executor.execute(consumer.receive(bytes, replyTo))
    }
    channel.listen(queueName, proxy)
    this
  }

}