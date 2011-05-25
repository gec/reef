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
package org.totalgrid.reef.messaging

import org.totalgrid.reef.api.scalaclient.{ ClientSession, Event, ISessionPool }
import org.totalgrid.reef.api.{ ServiceList, RequestEnv, IDestination, AnyNode }
import org.totalgrid.reef.api.service.IServiceAsync
import org.totalgrid.reef.reactor.Reactable

trait PoolableConnection {

  def getClientSession(): ClientSession

}

/** Combines the various registry traits into a single interface */
trait Connection {

  def getSessionPool(): ISessionPool

  /** Creates an event queue of type A that can be monitored using an ObservableSubscription */
  def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit

  /** Overload that defines the subscription in the function call */
  def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit

  /**
   * bind a service handler to the bus for a given exchange
   * @param exchange   exchange to bind to
   * @param service handler for the ServiceRequest, must return ServiceReponse
   * @param destination Optionally overrides the default destination of AnyNode
   * @param competing  false => (everyone gets a copy of the messages) or true => (only one handler gets each message)
   * @param reactor    if not None messaging handling is dispatched to a user defined reactor using execute
   */
  def bindService(service: IServiceAsync[_], destination: IDestination = AnyNode, competing: Boolean = false, reactor: Option[Reactable] = None): Unit

}

/** Implements the ProtoRegistry trait to provide a concrete AMQP service implementation */
class AMQPProtoRegistry(factory: AMQPProtoFactory, timeoutms: Long, lookup: ServiceList, defaultEnv: Option[RequestEnv] = None) extends Connection {

  private lazy val pool = new SessionPool(this)

  def getClientSession(): ClientSession = {
    val client = new ProtoClient(factory, lookup, timeoutms)
    defaultEnv.foreach(client.setDefaultHeaders)
    client
  }

  final override def getSessionPool(): ISessionPool = pool

  final override def defineEventQueue[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit): Unit = {
    factory.getEventQueue(deserialize, accept)
  }

  final override def defineEventQueueWithNotifier[A](deserialize: Array[Byte] => A, accept: Event[A] => Unit)(notify: String => Unit): Unit = {
    factory.getEventQueue(deserialize, accept, notify)
  }

  final override def bindService(service: IServiceAsync[_], destination: IDestination = AnyNode, competing: Boolean = false, reactor: Option[Reactable] = None): Unit = {
    factory.bindService(service.descriptor.id, service.respond, destination, competing, reactor)
  }

}
