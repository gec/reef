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
package org.totalgrid.reef.messaging.serviceprovider

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.broker.{ BrokerObjectConsumer, BrokerChannel }
import org.totalgrid.reef.japi.Envelope

trait PublishingSubscriptionHandler extends ServiceSubscriptionHandler with BrokerObjectConsumer with Logging {
  val exchange: String

  private def describe(exchange: String, key: String) =
    exchange + ", w/ key = " + key

  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String) = {
    sendTo((b: BrokerChannel) => {
      val msg = Envelope.ServiceNotification.newBuilder.setEvent(event).setPayload(resp.toByteString()).build
      logger.debug("published event: " + event + " to " + describe(exchange, key))
      b.publish(exchange, key, msg.toByteArray(), None)
    })
  }

  def bind(subQueue: String, key: String) = {
    sendTo((b: BrokerChannel) => {
      b.bindQueue(subQueue, exchange, key)
      logger.debug("binding queue: " + subQueue + " to " + describe(exchange, key))
    })
  }
}

