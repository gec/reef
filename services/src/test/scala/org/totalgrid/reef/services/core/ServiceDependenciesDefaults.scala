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

import org.totalgrid.reef.services.framework.SilentServiceSubscriptionHandler
import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore, MeasurementStore }
import org.totalgrid.reef.event.{ SilentEventSink, SystemEventSink }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.api.sapi.service.AsyncService
import org.totalgrid.reef.api.japi.client.Routable
import org.totalgrid.reef.api.japi.Envelope.Event
import org.totalgrid.reef.api.sapi.client.rest.{ RpcProviderInfo, SubscriptionHandler, Connection }
import org.totalgrid.reef.api.sapi.types.ServiceInfo
import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.services.{ DependenciesRequestContext, RequestContextDependencies, ServiceDependencies }

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
class ServiceDependenciesDefaults(connection: Connection = new MockConnection,
  pubs: SubscriptionHandler = new SilentServiceSubscriptionHandler,
  cm: MeasurementStore = new InMemoryMeasurementStore,
  eventSink: SystemEventSink = new SilentEventSink,
  authToken: String = "") extends ServiceDependencies(connection, pubs, cm, eventSink, authToken)

class HeadersRequestContext(
  extraHeaders: BasicRequestHeaders = BasicRequestHeaders.empty,
  dependencies: RequestContextDependencies = new ServiceDependenciesDefaults())
    extends DependenciesRequestContext(dependencies) {
  modifyHeaders(_.merge(extraHeaders))
}
