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

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.sapi.client.Response
import org.totalgrid.reef.promise.SynchronizedPromise
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.{ DependenciesRequestContext, ServiceDependencies }

class SyncService[A <: AnyRef](service: ServiceEntryPoint[A], contextSource: RequestContextSource) {

  def get(req: A): Response[A] = get(req, new RequestEnv)
  def get(req: A, env: RequestEnv): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.getAsync(cm, req)(response.onResponse)
    response.await
  }

  def put(req: A): Response[A] = put(req, new RequestEnv)
  def put(req: A, env: RequestEnv): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.putAsync(cm, req)(response.onResponse)
    response.await
  }

  def post(req: A): Response[A] = post(req, new RequestEnv)
  def post(req: A, env: RequestEnv): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.postAsync(cm, req)(response.onResponse)
    response.await
  }

  def delete(req: A): Response[A] = delete(req, new RequestEnv)
  def delete(req: A, env: RequestEnv): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.deleteAsync(cm, req)(response.onResponse)
    response.await
  }
}

class MockRequestContextSource(dependencies: ServiceDependencies, commonHeaders: RequestEnv) extends RequestContextSource {
  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    context.headers.merge(commonHeaders)
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}

object SyncServiceShims {

  implicit def getRequestEnv: RequestEnv = {
    val env = new RequestEnv
    env.setUserName("user")
    env
  }

  implicit def getRequestContextSource(implicit headers: RequestEnv) = {
    new MockRequestContextSource(new ServiceDependencies, headers)
  }

  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit contextSource: RequestContextSource) = new SyncService[A](service, contextSource)
}

object CustomServiceShims {
  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit contextSource: RequestContextSource) = new SyncService[A](service, contextSource)
}