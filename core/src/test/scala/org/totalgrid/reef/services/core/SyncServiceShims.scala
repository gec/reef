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
import org.totalgrid.reef.services.framework.{ HeadersRequestContext, SimpleRequestContext, RequestContext, ServiceEntryPoint }

class SyncService[A <: AnyRef](service: ServiceEntryPoint[A], context: RequestContext) {

  def get(req: A, env: RequestEnv): Response[A] = {
    context.headers.merge(env)
    get(req)
  }
  def get(req: A): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    service.getAsync(context, req)(response.onResponse)
    response.await
  }

  def put(req: A, env: RequestEnv): Response[A] = {
    context.headers.merge(env)
    put(req)
  }
  def put(req: A): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    service.putAsync(context, req)(response.onResponse)
    response.await
  }

  def delete(req: A, env: RequestEnv): Response[A] = {
    context.headers.merge(env)
    delete(req)
  }
  def delete(req: A): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    service.deleteAsync(context, req)(response.onResponse)
    response.await
  }

  def post(req: A, env: RequestEnv): Response[A] = {
    context.headers.merge(env)
    post(req)
  }
  def post(req: A): Response[A] = {
    val response = new SynchronizedPromise[Response[A]]()
    service.postAsync(context, req)(response.onResponse)
    response.await
  }
}

object SyncServiceShims {

  implicit def getRequestEnv: RequestEnv = {
    val env = new RequestEnv
    env.setUserName("user")
    env
  }

  implicit def getRequestContext(implicit headers: RequestEnv) = {
    new HeadersRequestContext(headers)
  }

  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit context: RequestContext) = new SyncService[A](service, context)
}

object CustomServiceShims {
  implicit def toSyncService[A <: AnyRef](service: ServiceEntryPoint[A])(implicit context: RequestContext) = new SyncService[A](service, context)
}