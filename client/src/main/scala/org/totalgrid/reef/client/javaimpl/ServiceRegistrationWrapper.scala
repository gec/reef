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
package org.totalgrid.reef.client.javaimpl

import org.totalgrid.reef.client.sapi.client.rest.{ Connection => SConnection }
import org.totalgrid.reef.client.{ SubscriptionBinding, Routable }
import org.totalgrid.reef.client.registration.{ EventPublisher, Service, ServiceRegistration }
import org.totalgrid.reef.client.types.TypeDescriptor
import net.agileautomata.executor4s.Executor

class ServiceRegistrationWrapper(conn: SConnection, exe: Executor) extends ServiceRegistration {

  def getEventPublisher: EventPublisher = new EventPublisherWrapper(conn)

  def bindService[A](service: Service, desc: TypeDescriptor[A], destination: Routable, competing: Boolean): SubscriptionBinding = {
    val srv = new ServiceWrapper(service, desc)
    // TODO: this exe should be a strand I believe
    conn.bindService(srv, exe, destination, competing)
  }

  def bindServiceQueue[T](subscriptionQueue: String, key: String, klass: Class[T]) {
    conn.bindServiceQueue(subscriptionQueue, key, klass)
  }

  def declareEventExchange(klass: Class[_]) {
    conn.declareEventExchange(klass)
  }
}