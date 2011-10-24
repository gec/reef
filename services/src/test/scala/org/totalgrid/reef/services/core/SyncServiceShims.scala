/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.api.sapi.client.Response
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.api.sapi.client.impl.SynchronizedPromise
import org.totalgrid.reef.services.{ ServiceBootstrap, DependenciesRequestContext, ServiceDependencies }

class SyncService[A <: AnyRef](service: ServiceEntryPoint[A], contextSource: RequestContextSource) {

  def get(req: A): Response[A] = get(req, BasicRequestHeaders.empty)
  def get(req: A, env: BasicRequestHeaders): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.getAsync(cm, req)(response.set _)
    response.await
  }

  def put(req: A): Response[A] = put(req, BasicRequestHeaders.empty)
  def put(req: A, env: BasicRequestHeaders): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.putAsync(cm, req)(response.set _)
    response.await
  }

  def post(req: A): Response[A] = post(req, BasicRequestHeaders.empty)
  def post(req: A, env: BasicRequestHeaders): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.postAsync(cm, req)(response.set _)
    response.await
  }

  def delete(req: A): Response[A] = delete(req, BasicRequestHeaders.empty)
  def delete(req: A, env: BasicRequestHeaders): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.deleteAsync(cm, req)(response.set _)
    response.await
  }
}

class MockRequestContextSource(dependencies: ServiceDependencies, commonHeaders: BasicRequestHeaders) extends RequestContextSource {

  // just define all of the event exchanges at the beginning of the test
  ServiceBootstrap.defineEventExchanges(dependencies.connection)

  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    context.modifyHeaders(_.merge(commonHeaders))
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}

object SyncServiceShims {

  implicit def getRequestEnv: BasicRequestHeaders = BasicRequestHeaders.empty.setUserName("user")

  implicit def getRequestContextSource(implicit headers: BasicRequestHeaders) = {
    new MockRequestContextSource(new ServiceDependenciesDefaults, headers)
  }

  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit contextSource: RequestContextSource) = new SyncService[A](service, contextSource)
}

object CustomServiceShims {
  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit contextSource: RequestContextSource) = new SyncService[A](service, contextSource)
}