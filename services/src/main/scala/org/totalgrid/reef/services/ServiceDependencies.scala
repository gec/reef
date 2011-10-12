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
import org.totalgrid.reef.services.core.{ SilentSummaryPoints, SummaryPoints }
import org.totalgrid.reef.event.{ SilentEventSink, SystemEventSink }
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.executor.mock.InstantExecutor
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceSubscriptionHandler, SilentEventPublishers, ServiceEventPublishers }
import org.totalgrid.reef.japi.Envelope.Event
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.services.framework._

case class ServiceDependencies(pubs: ServiceEventPublishers = new SilentEventPublishers,
  summaries: SummaryPoints = new SilentSummaryPoints,
  cm: MeasurementStore = new InMemoryMeasurementStore,
  eventSink: SystemEventSink = new SilentEventSink,
  coordinatorExecutor: Executor = new InstantExecutor)

class HeadersRequestContext(
  extraHeaders: BasicRequestHeaders = BasicRequestHeaders.empty,
  dependencies: ServiceDependencies = new ServiceDependencies)
    extends DependenciesRequestContext(dependencies) {
  modifyHeaders(_.merge(extraHeaders))
}

class AllTypeServiceSubscriptionHandler(dependencies: ServiceDependencies) extends ServiceSubscriptionHandler {
  def publish(event: Event, resp: GeneratedMessage, key: String) = {
    dependencies.pubs.getEventSink(resp.getClass).publish(event, resp, key)
  }

  def bind(subQueue: String, key: String, request: AnyRef) = {
    dependencies.pubs.getEventSink(request.getClass).bind(subQueue, key, request)
  }
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

  val subHandler = new AllTypeServiceSubscriptionHandler(dependencies)

  val eventSink = dependencies.eventSink
}

class DependenciesSource(dependencies: ServiceDependencies) extends RequestContextSource {
  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}
class SimpleRequestContextSource extends DependenciesSource(new ServiceDependencies)