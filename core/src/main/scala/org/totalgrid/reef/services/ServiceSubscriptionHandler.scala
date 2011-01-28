/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Envelope

import org.totalgrid.reef.messaging.{ BrokerObjectConsumerActor, BrokerChannel, BrokerObjectConsumer }
import org.totalgrid.reef.reactor.Reactable

trait ServiceSubscriptionHandler {
  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String)

  def bind(subQueue: String, key: String)
}

class SilentServiceSubscriptionHandler extends ServiceSubscriptionHandler {
  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String) = {}

  def bind(subQueue: String, key: String) = {}
}

trait PublishingSubscriptionHandler extends ServiceSubscriptionHandler with BrokerObjectConsumer with Logging {
  val exchange: String

  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String) = {
    sendTo((b: BrokerChannel) => {
      val msg = Envelope.ServiceNotification.newBuilder.setEvent(event).setPayload(resp.toByteString()).build
      info("Published event: " + event + " to " + exchange + " key = " + key)
      b.publish(exchange, key, msg.toByteArray(), None)
    })
  }

  def bind(subQueue: String, key: String) = {
    sendTo((b: BrokerChannel) => {
      info("Binding queue: " + subQueue + " to " + exchange + " key = " + key)
      b.bindQueue(subQueue, exchange, key)
    })
  }
}

class PublishingSubscriptionActor(exch: String, reactor: Reactable) extends BrokerObjectConsumerActor(reactor) with PublishingSubscriptionHandler with Logging {
  val exchange = exch

  override def onConnect(b: BrokerChannel) = {
    debug("declaring exchange: " + exchange)
    b.declareExchange(exch)
  }

}