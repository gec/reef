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
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.messaging.serviceprovider.{ SilentServiceSubscriptionHandler, ServiceSubscriptionHandler }
import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.japi.Envelope.Event
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.event.{ SilentEventSink, SystemEventSink }

trait RequestContext {

  def operationBuffer: OperationBuffer

  def subHandler: ServiceSubscriptionHandler

  def eventSink: SystemEventSink

  def headers: RequestEnv
}

class Buffer extends OperationBuffer with LinkedBufferedEvaluation

class SimpleRequestContext extends HeadersRequestContext(new RequestEnv)

class HeadersRequestContext(val headers: RequestEnv) extends RequestContext {
  val operationBuffer = new Buffer

  val subHandler = new SilentServiceSubscriptionHandler

  val eventSink = new SilentEventSink
}

class DependenciesRequestContext(dependencies: ServiceDependencies) extends RequestContext {

  val headers = new RequestEnv

  val operationBuffer = new Buffer

  val subHandler = new ServiceSubscriptionHandler {
    def publish(event: Event, resp: GeneratedMessage, key: String) = {
      dependencies.pubs.getEventSink(resp.getClass).publish(event, resp, key)
    }

    def bind(subQueue: String, key: String, request: AnyRef) = {
      dependencies.pubs.getEventSink(request.getClass).bind(subQueue, key, request)
    }
  }

  val eventSink = dependencies.eventSink
}

trait RequestContextSource {
  def transaction[A](f: RequestContext => A): A
}

class SimpleRequestContextSource extends DependenciesSource(new ServiceDependencies)

class DependenciesSource(dependencies: ServiceDependencies) extends RequestContextSource {
  def transaction[A](f: RequestContext => A) = {
    val context = new DependenciesRequestContext(dependencies)
    ServiceTransactable.doTransaction(context.operationBuffer, { b: OperationBuffer => f(context) })
  }
}
