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
import org.totalgrid.reef.client.sapi.client._
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.{ Promise => JPromise }
import org.totalgrid.reef.client.operations.{ RequestListenerManager, Response => JResponse }

trait Client
    extends Executor
    with ClientBindOperations
    with DefaultHeaders
    with Connection
    with RpcProvider
    with SubscriptionCreatorManager
    with ServiceRegistry
    with ClientLogout {
  /**
   * create a copy of the client for use in a different thread (or whose
   * subscriptions we want handled in pararell with the original client).
   *
   * No listeners are copied, only the auth token.
   */
  def spawn(): Client

  def requestJava[A](verb: Envelope.Verb, payload: A, headers: Option[BasicRequestHeaders]): JPromise[JResponse[A]]

  def listenerManager: RequestListenerManager

  def notifyListeners[A](verb: Envelope.Verb, payload: A, promise: JPromise[JResponse[A]])
}