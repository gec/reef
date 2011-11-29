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
package org.totalgrid.reef.clientapi.javaimpl

import org.totalgrid.reef.clientapi.sapi.client.rest.{ Client => SClient }
import org.totalgrid.reef.clientapi.{ SubscriptionCreationListener, RequestHeaders, Client }

import org.totalgrid.reef.clientapi.sapi.client.{ BasicRequestHeaders, RequestSpy }
import org.totalgrid.reef.clientapi.exceptions.ServiceIOException
import org.totalgrid.reef.clientapi.rpc.RpcProviderInfo

class ClientWrapper(client: SClient) extends Client {

  def getHeaders = client.getHeaders

  // TODO - figure out a better way to expose this
  def setHeaders(headers: RequestHeaders) = headers match {
    case h: BasicRequestHeaders => client.setHeaders(h)
    case _ => throw new ServiceIOException("Cannot use custom header class. Must use headers returned from getHeaders")
  }

  def addRequestSpy(spy: RequestSpy) = client.addRequestSpy(spy)

  def removeRequestSpy(spy: RequestSpy) = client.removeRequestSpy(spy)

  def addSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.addSubscriptionCreationListener(listener)
  def removeSubscriptionCreationListener(listener: SubscriptionCreationListener) = client.removeSubscriptionCreationListener(listener)

  def getService[A](klass: Class[A]) = client.getRpcInterface(klass)
  def addServiceProvider(info: RpcProviderInfo) = client.addRpcProvider(info)
}