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

import org.totalgrid.reef.client.sapi.rpc.RESTOperations
import org.totalgrid.reef.client.service.{ RESTOperations => JRESTOperations }
import org.totalgrid.reef.client.sapi.client.rest.{ Client, RpcProvider }
import org.totalgrid.reef.client.sapi.client.rpc.framework.ApiBase
import org.totalgrid.reef.client.service.impl.RESTOperationsJavaShim
import org.totalgrid.reef.client.service.async.impl.RESTOperationsAsyncJavaShim
import org.totalgrid.reef.client.service.async.RESTOperationsAsync

class RESTOperationsWrapper(client: Client) extends ApiBase(client) with RESTOperationsImpl

final class RESTOperationsJavaShimWrapper(client: Client) extends RESTOperationsJavaShim {

  private val srv = new RESTOperationsWrapper(client)

  override def service = srv
}

final class RESTOperationsAsyncJavaShimWrapper(client: Client) extends RESTOperationsAsyncJavaShim {

  private val srv = new RESTOperationsWrapper(client)

  override def service = srv
}

object RESTServiceProviders {
  def getScalaServiceInfo = RpcProvider(new RESTOperationsWrapper(_), List(classOf[RESTOperations]))
  def getJavaServiceInfo = RpcProvider(new RESTOperationsJavaShimWrapper(_), List(classOf[JRESTOperations]))
  def getJavaAsyncServiceInfo = RpcProvider(new RESTOperationsAsyncJavaShimWrapper(_), List(classOf[RESTOperationsAsync]))
}