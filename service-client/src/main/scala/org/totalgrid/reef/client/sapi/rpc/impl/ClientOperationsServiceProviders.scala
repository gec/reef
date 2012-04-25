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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.sapi.rpc.ClientOperations
import org.totalgrid.reef.client.sapi.sync.{ ClientOperations => SyncOperations }
import org.totalgrid.reef.client.service.{ ClientOperations => JClientOperations }
import org.totalgrid.reef.client.sapi.client.rest.{ RpcProvider }
import org.totalgrid.reef.client.sapi.client.rpc.framework.ApiBase
import org.totalgrid.reef.client.service.impl.ClientOperationsJavaShim
import org.totalgrid.reef.client.sapi.sync.impl.ClientOperationsSyncShim
import org.totalgrid.reef.client.service.async.impl.ClientOperationsAsyncJavaShim
import org.totalgrid.reef.client.service.async.ClientOperationsAsync
import org.totalgrid.reef.client.Client

class ClientOperationsWrapper(client: Client) extends ApiBase(client) with ClientOperationsImpl

final class ClientOperationsSyncShimWrapper(client: Client) extends ClientOperationsSyncShim {
  private val srv = new ClientOperationsWrapper(client)
  override def service = srv
}

final class ClientOperationsJavaShimWrapper(client: Client) extends ClientOperationsJavaShim {

  private val srv = new ClientOperationsWrapper(client)

  override def service = srv
}

final class ClientOperationsAsyncJavaShimWrapper(client: Client) extends ClientOperationsAsyncJavaShim {

  private val srv = new ClientOperationsWrapper(client)

  override def service = srv
}

object ClientOperationsServiceProviders {
  def getScalaServiceInfo = RpcProvider(new ClientOperationsWrapper(_), List(classOf[ClientOperations]))
  def getScalaSyncServiceInfo = RpcProvider(new ClientOperationsSyncShimWrapper(_), List(classOf[SyncOperations]))
  def getJavaServiceInfo = RpcProvider(new ClientOperationsJavaShimWrapper(_), List(classOf[JClientOperations]))
  def getJavaAsyncServiceInfo = RpcProvider(new ClientOperationsAsyncJavaShimWrapper(_), List(classOf[ClientOperationsAsync]))
}