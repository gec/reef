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
package org.totalgrid.reef.api.sapi.client.rest.impl

import org.totalgrid.reef.api.sapi.client.{ Subscription, Event }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.{ BrokerMessageConsumer, BrokerMessage, BrokerSubscription }
import org.totalgrid.reef.api.japi.Envelope
import com.weiglewilczek.slf4s.Logging

/**
 * synchronous subscription object, allows canceling and a delayed starting
 */
final class DefaultSubscription[A](subscription: BrokerSubscription, executor: Executor, deserialize: Array[Byte] => A) extends Subscription[A] with Logging {

  override def id() = subscription.getQueue
  override def cancel() = subscription.close()

  override def start(callback: Event[A] => Unit): Subscription[A] = {
    val consumer = new BrokerMessageConsumer {
      def onMessage(msg: BrokerMessage) {
        try {
          val event = Envelope.ServiceNotification.parseFrom(msg.bytes)
          val value = deserialize(event.getPayload.toByteArray)
          executor.execute(callback(Event(event.getEvent, value)))
        } catch {
          case ex: Exception => logger.error("Unable to deserialize incoming event: " + msg)
        }
      }
    }
    subscription.start(consumer)
    this
  }

}