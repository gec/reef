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

import scala.collection.immutable
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.messaging.{ ServiceInfo, AMQPProtoFactory }
import org.totalgrid.reef.reactor.ReactActor

// Interface for services to acquire subscription handlers based on message type
trait ServiceEventPublishers {
  def getEventSink[T <: GeneratedMessage](klass: Class[T]): ServiceSubscriptionHandler
}

/**
 * mixin that manages the lazy creation and storing of subscription handlers (publishers) 
 * that the models use. This way the models can be constructed multiple times and all publications
 * go through the same publishingactors  
 */
abstract class ServiceEventPublisherMap(serviceInfo: Class[_] => ServiceInfo) extends ServiceEventPublishers {

  /**
   * concrete implementations define this to inject a specific ServiceSubscriptionHandler class
   */
  def createPublisher(exchange: String): ServiceSubscriptionHandler

  private var built = immutable.Map.empty[String, ServiceSubscriptionHandler]

  // Lazy instantiation of sub handlers
  private def getSink(exch: String): ServiceSubscriptionHandler = {
    built.get(exch).getOrElse {
      val pubsub = createPublisher(exch)
      built += (exch -> pubsub)
      pubsub
    }
  }
  def getEventSink[T <: GeneratedMessage](klass: Class[T]): ServiceSubscriptionHandler = {
    getSink(serviceInfo(klass).subExchange)
  }
}

/**
 * BusTied implementation of the ServiceEventPublishers interface that generates "real" pubslishers that send
 * to a message broker
 */
class ServiceEventPublisherRegistry(amqp: AMQPProtoFactory, serviceInfo: Class[_] => ServiceInfo) extends ServiceEventPublisherMap(serviceInfo) {

  def createPublisher(exchange: String): ServiceSubscriptionHandler = {
    val reactor = new ReactActor {}
    val pubsub = new PublishingSubscriptionActor(exchange, reactor)
    amqp.add(pubsub)
    amqp.addReactor(reactor)
    pubsub
  }

}

/**
 * Mock publisher class that just eats all publication and bind messages.
 */
class SilentEventPublishers extends ServiceEventPublishers {

  def getEventSink[T <: GeneratedMessage](klass: Class[T]): ServiceSubscriptionHandler = {
    new SilentServiceSubscriptionHandler
  }

}