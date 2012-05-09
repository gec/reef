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
package org.totalgrid.reef.client.impl

import org.totalgrid.reef.client.registration.{ EventPublisher, Service, ServiceRegistration }
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.types.TypeDescriptor
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.{ ServiceRegistry, SubscriptionBinding, Routable }

class EventPublisherImpl(broker: BrokerConnection, registry: ServiceRegistry) extends EventPublisher {

  def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) {
    val info = registry.getServiceTypeInformation(klass)
    broker.bindQueue(subQueue, info.getEventExchange, key)
  }

  def publishEvent[A](eventType: SubscriptionEventType, eventMessage: A, routingKey: String) {
    val info = registry.getServiceTypeInformation(ClassLookup.get(eventMessage))
    val desc = info.getSubscriptionDescriptor.asInstanceOf[TypeDescriptor[A]]
    val event = RestHelpers.getEvent(eventType, eventMessage, desc)
    broker.publish(info.getEventExchange, routingKey, event.toByteArray)
  }
}

class ServiceRegistrationImpl(broker: BrokerConnection, registry: ServiceRegistry, exe: Executor) extends ServiceRegistration {

  def getEventPublisher: EventPublisher = new EventPublisherImpl(broker, registry)

  def bindService[A](service: Service, descriptor: TypeDescriptor[A], destination: Routable, competing: Boolean): SubscriptionBinding = {
    val serviceInfo = registry.getServiceTypeInformation(descriptor.getKlass)

    def subscribe(competing: Boolean) = {
      broker.declareExchange(serviceInfo.getEventExchange)
      broker.declareExchange(descriptor.id)
      val sub = if (competing) broker.listen(descriptor.id + "_server")
      else broker.listen()
      broker.bindQueue(sub.getQueue, descriptor.id, destination.getKey)
      new DefaultServiceBinding[A](broker, sub, exe)
    }

    val sub: DefaultServiceBinding[A] = subscribe(competing)
    sub.start(service)

    sub
  }

  def bindServiceQueue[T](subscriptionQueue: String, key: String, klass: Class[T]) {
    val info = registry.getServiceTypeInformation(klass)
    broker.bindQueue(subscriptionQueue, info.getDescriptor.id, key)
  }

  def declareEventExchange(klass: Class[_]) {
    val info = registry.getServiceTypeInformation(klass)
    broker.declareExchange(info.getEventExchange)
  }
}
