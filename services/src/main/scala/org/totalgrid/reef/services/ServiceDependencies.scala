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
package org.totalgrid.reef.services

import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.event.SystemEventSink
import org.totalgrid.reef.clientapi.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.clientapi.sapi.client.rest.{ Connection, SubscriptionHandler }
import org.totalgrid.reef.models.AuthPermission

class ServiceDependencies(
  connection: Connection,
  pubs: SubscriptionHandler,
  val measurementStore: MeasurementStore,
  eventSink: SystemEventSink,
  authToken: String) extends RequestContextDependencies(connection, pubs, authToken, eventSink)

class RequestContextDependencies(
  val connection: Connection,
  val pubs: SubscriptionHandler,
  val authToken: String,
  val eventSink: SystemEventSink)

trait HeadersContext {
  protected var headers = BasicRequestHeaders.empty

  def getHeaders = headers

  def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders): BasicRequestHeaders = {
    val newHeaders = modify(headers)
    headers = newHeaders
    newHeaders
  }
}

trait PermissionsContext {
  protected var permissions = Option.empty[List[AuthPermission]]

  def getPermissions = permissions
  def setPermissions(p: List[AuthPermission]) = permissions = Some(p)
}

class DependenciesRequestContext(dependencies: RequestContextDependencies) extends RequestContext with HeadersContext with PermissionsContext {

  val operationBuffer = new BasicOperationBuffer

  val subHandler = dependencies.pubs

  val eventSink = dependencies.eventSink

  def client = dependencies.connection.login(dependencies.authToken)
}

class DependenciesSource(dependencies: RequestContextDependencies) extends RequestContextSource {
  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}

// TODO: get rid of all uses of NullRequestContext
class NullRequestContext extends RequestContext with HeadersContext with PermissionsContext {

  def client = throw new Exception
  def eventSink = throw new Exception
  def operationBuffer = throw new Exception
  def subHandler = throw new Exception
}