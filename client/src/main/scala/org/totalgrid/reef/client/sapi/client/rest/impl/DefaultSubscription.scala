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
package org.totalgrid.reef.client.sapi.client.rest.impl

import org.totalgrid.reef.client.{ Subscription, SubscriptionEventAcceptor }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.broker.{ BrokerMessageConsumer, BrokerMessage, BrokerSubscription }
import org.totalgrid.reef.client.proto.Envelope

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.operations.scl.Event

/**
 * synchronous subscription object, allows canceling and a delayed starting
 */
final class DefaultSubscription[A](subscription: BrokerSubscription, executor: Executor, deserialize: Array[Byte] => A) extends Subscription[A] with Logging {

  override def getId() = subscription.getQueue
  override def cancel() = subscription.close()

  override def start(callback: SubscriptionEventAcceptor[A]): Subscription[A] = {
    val consumer = new BrokerMessageConsumer {
      def onMessage(msg: BrokerMessage) {
        try {
          val event = Envelope.ServiceNotification.parseFrom(msg.bytes)
          val value = deserialize(event.getPayload.toByteArray)
          executor.execute(try {
            callback.onEvent(Event[A](event.getEvent, value))
          } catch {
            case ex: Exception =>
              logger.error("Subscription event caused exception: " + ex.getMessage, ex)
          })
        } catch {
          case ex: Exception => logger.error("Unable to deserialize incoming event: " + ex.getMessage, ex)
        }
      }
    }
    subscription.start(consumer)
    this
  }

}