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
package org.totalgrid.reef.client.operations.scl

import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.{ Batching, Promise, RequestHeaders, Client }
import ScalaServiceOperations._
import org.totalgrid.reef.client.operations.{ RequestListener, RequestListenerManager, ServiceOperations }

abstract class ServiceOperationsProvider(client: Client)
    extends UsesServiceOperations with UsesServiceRegistry with HasHeaders with HasBatching with RequestListenerManager {

  protected def ops: ServiceOperations = client.getServiceOperations
  def batching: Batching = client.getBatching

  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _] = client.getServiceRegistry.getServiceTypeInformation(klass)

  def getHeaders = client.getHeaders

  def setHeaders(hdrs: RequestHeaders) {
    client.setHeaders(hdrs)
  }

  def batchGets[A](errorMessage: => String)(gets: List[A]): Promise[List[A]] = {
    ops.batchOperation(errorMessage) { session =>
      PromiseCollators.collate(client.getInternal.getExecutor, gets.map(session.get(_).map(_.one)))
    }
  }

  def addRequestListener(listener: RequestListener) {
    client.getRequestListenerManager.addRequestListener(listener)
  }

  def removeRequestListener(listener: RequestListener) {
    client.getRequestListenerManager.removeRequestListener(listener)
  }
}
