/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging.serviceprovider

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.messaging.{ BrokerChannel, BrokerObjectConsumer }
import org.totalgrid.reef.api.Envelope

trait PublishingSubscriptionHandler extends ServiceSubscriptionHandler with BrokerObjectConsumer with Logging {
  val exchange: String

  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String) = {
    sendTo((b: BrokerChannel) => {
      val msg = Envelope.ServiceNotification.newBuilder.setEvent(event).setPayload(resp.toByteString()).build
      reefLogger.debug("published event: {} to {}, key = {}", Array[AnyRef](event, exchange, key))
      b.publish(exchange, key, msg.toByteArray(), None)
    })
  }

  def bind(subQueue: String, key: String) = {
    sendTo((b: BrokerChannel) => {
      b.bindQueue(subQueue, exchange, key)
      reefLogger.info("binding queue: {} to {} key = {}", Array[AnyRef](subQueue, exchange, key))
    })
  }
}

