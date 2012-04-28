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

import org.totalgrid.reef.client.operations.ServiceOperations
import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.{ RequestHeaders, Client }

abstract class ServiceOperationsProvider(client: Client) extends UsesServiceOperations with UsesServiceRegistry with StubBatchOperations with HasHeaders {
  protected def ops: ServiceOperations = client.getServiceOperations
  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _] = client.getInternal.getServiceRegistry.getServiceInfo(klass)

  def getHeaders() = client.getHeaders

  def setHeaders(hdrs: RequestHeaders) {
    client.setHeaders(hdrs)
  }

  // PSEUDO-HACK
  def setResultLimit(limit: Int) { client.getHeaders.setResultLimit(limit) }
}
