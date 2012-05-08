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

import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.core.util.SynchronizedResult
import org.totalgrid.reef.services.{ SilentRequestContext, ServiceBootstrap, DependenciesRequestContext, ServiceDependencies }
import org.totalgrid.reef.models.{ ApplicationSchema, Agent, DatabaseUsingTestBaseNoTransaction }
import org.totalgrid.reef.client.RequestHeaders

class SyncService[A <: AnyRef](service: ServiceEntryPoint[A], contextSource: RequestContextSource) {

  def get(req: A): Response[A] = get(req, BasicRequestHeaders.empty)
  def get(req: A, env: RequestHeaders): Response[A] = {
    val response = new SynchronizedResult[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.getAsync(cm, req)(response.set _)
    response.await
  }

  def put(req: A): Response[A] = put(req, BasicRequestHeaders.empty)
  def put(req: A, env: RequestHeaders): Response[A] = {
    val response = new SynchronizedResult[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.putAsync(cm, req)(response.set _)
    response.await
  }

  def post(req: A): Response[A] = post(req, BasicRequestHeaders.empty)
  def post(req: A, env: RequestHeaders): Response[A] = {
    val response = new SynchronizedResult[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.postAsync(cm, req)(response.set _)
    response.await
  }

  def delete(req: A): Response[A] = delete(req, BasicRequestHeaders.empty)
  def delete(req: A, env: RequestHeaders): Response[A] = {
    val response = new SynchronizedResult[Response[A]]()
    val cm = new RequestContextSourceWithHeaders(contextSource, env)
    service.deleteAsync(cm, req)(response.set _)
    response.await
  }
}

trait AgentAddingContextSource {

  var agents = Map.empty[String, Agent]

  def addUser(context: RequestContext) {
    val userName = context.get[String]("user_name").getOrElse("user01")
    val agentModel = agents.get(userName) match {
      case Some(agent) => agent
      case None =>
        val agentModel = new AgentServiceModel().createAgentWithPassword(new SilentRequestContext, userName, "password")
        val agent = ApplicationSchema.agents.insert(agentModel)
        agents += userName -> agent
        agent
    }
    context.set("agent", agentModel)
  }
}

class MockRequestContextSource(dependencies: ServiceDependencies, var userName: String = "user01") extends RequestContextSource with AgentAddingContextSource {

  // just define all of the event exchanges at the beginning of the test
  ServiceBootstrap.defineEventExchanges(dependencies.connection)

  def transaction[A](f: RequestContext => A) = {
    val context = getContext
    ServiceTransactable.doTransaction(dependencies.dbConnection, context.operationBuffer, { b: OperationBuffer =>
      addUser(context)
      f(context)
    })
  }

  def getContext = {
    val context = new DependenciesRequestContext(dependencies)
    context.set("user_name", userName)
    context
  }
}

trait SyncServicesTestHelpers { self: DatabaseUsingTestBaseNoTransaction =>

  def getRequestContextSource() = {

    new MockRequestContextSource(new ServiceDependenciesDefaults(dbConnection))
  }
  lazy val defaultContextSource = getRequestContextSource()
  def sync[A <: AnyRef](service: ServiceEntryPoint[A]): SyncService[A] = {
    sync(service, defaultContextSource)
  }
  def sync[A <: AnyRef](service: ServiceEntryPoint[A], contextSource: RequestContextSource): SyncService[A] = {
    new SyncService(service, contextSource)
  }
}