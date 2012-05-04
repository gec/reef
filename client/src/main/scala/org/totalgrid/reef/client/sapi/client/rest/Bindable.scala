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
package org.totalgrid.reef.client.sapi.client.rest

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.{ Subscription, SubscriptionBinding, Routable }
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.sapi.service.AsyncService

trait Bindable {

  /**
   * subscribe returns a Future to the result that is always going to be set when it is returned, it is
   * returned as a future so a client who wants to listen to the SubscriptionResult will get the event
   * on the same dispatcher as the result would come on
   */
  def subscribe[A](descriptor: TypeDescriptor[A], dispatcher: Executor): Subscription[A]

  /**
   * setup and bind a service listener to the published "request exchange" associated with the service type A.
   * NOTE: Requires "services" level access to broker to perform binding operations, most clients
   * do not have the necessary privileges to bind to arbitrary queues.
   */
  def bindService[A](service: AsyncService[A], dispatcher: Executor, destination: Routable, competing: Boolean): SubscriptionBinding

  /**
   * setups a service listener to the published "request exchange" associated with the service type A; binding must be
   * done later by an authorized agent with "services" level access to the broker using the bindServiceQueue() function.
   */
  def lateBindService[A](service: AsyncService[A], dispatcher: Executor): SubscriptionBinding

  /**
   * Do the exchange -> queue binding for a lateBoundService
   * NOTE: Requires "services" level access to broker to perform binding operations, most clients
   * do not have the necessary privileges to bind to arbitrary queues.
   */
  def bindServiceQueue[A](subQueue: String, key: String, klass: Class[A])
}