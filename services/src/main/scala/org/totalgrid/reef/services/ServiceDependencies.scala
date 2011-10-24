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

import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore, MeasurementStore }
import org.totalgrid.reef.event.{ SilentEventSink, SystemEventSink }
import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.api.sapi.client.rest.{ RpcProviderInfo, Connection, SubscriptionHandler }
import org.totalgrid.reef.api.sapi.types.ServiceInfo
import org.totalgrid.reef.api.japi.Envelope.Event
import org.totalgrid.reef.api.sapi.service.AsyncService
import net.agileautomata.executor4s._
import org.totalgrid.reef.api.japi.client.Routable
import org.totalgrid.reef.client.sapi.ReefServicesList
import testing.InstantExecutor

// TODO: move MockConnection down to test classes that use it
class MockConnection extends Connection {
  def declareEventExchange(klass: Class[_]) = null

  def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) {}

  def bindService[A](service: AsyncService[A], dispatcher: Executor, destination: Routable, competing: Boolean) = null

  def login(authToken: String) = null

  def login(userName: String, password: String) = null

  def publishEvent[A](typ: Event, value: A, key: String) {}

  def addRpcProvider(info: RpcProviderInfo) = null

  def addServiceInfo[A](info: ServiceInfo[A, _]) = null
}

case class ServiceDependencies(
    connection: Connection = new MockConnection,
    pubs: SubscriptionHandler = new SilentServiceSubscriptionHandler,
    cm: MeasurementStore = new InMemoryMeasurementStore,
    eventSink: SystemEventSink = new SilentEventSink,
    coordinatorExecutor: Executor = new InstantExecutor,
    authToken: String = "") {

  // TODO: is this the best place to define event exchanges?
  def defineEventExchanges() {
    ReefServicesList.getServicesList.foreach { serviceInfo =>
      connection.declareEventExchange(serviceInfo.descriptor.getKlass)
    }
  }
}

class HeadersRequestContext(
  extraHeaders: BasicRequestHeaders = BasicRequestHeaders.empty,
  dependencies: ServiceDependencies = new ServiceDependencies)
    extends DependenciesRequestContext(dependencies) {
  modifyHeaders(_.merge(extraHeaders))
}

class DependenciesRequestContext(dependencies: ServiceDependencies) extends RequestContext {

  private var headers = BasicRequestHeaders.empty

  def getHeaders = headers

  def modifyHeaders(modify: BasicRequestHeaders => BasicRequestHeaders): BasicRequestHeaders = {
    val newHeaders = modify(headers)
    headers = newHeaders
    newHeaders
  }

  val operationBuffer = new BasicOperationBuffer

  val subHandler = dependencies.pubs

  val eventSink = dependencies.eventSink

  def client = dependencies.connection.login(dependencies.authToken)
}

class DependenciesSource(dependencies: ServiceDependencies) extends RequestContextSource {
  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}
class SimpleRequestContextSource extends DependenciesSource(new ServiceDependencies)