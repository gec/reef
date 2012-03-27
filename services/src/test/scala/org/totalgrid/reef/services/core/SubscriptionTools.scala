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

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.client.sapi.client.rest.SubscriptionHandler
import org.totalgrid.reef.services.HeadersContext
import org.totalgrid.reef.event.SilentEventSink
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.persistence.squeryl.DbConnection
import org.totalgrid.reef.services.authz.{ AuthzService, NullAuthzService }
import java.util.UUID
import org.totalgrid.reef.models.{ ApplicationSchema, Entity }
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.authz.{ Permission, AuthzFilteringService, FilteredResult }

// TODO: either extract auth stuff or rename to "context source tools" or something
object SubscriptionTools {

  trait SubscriptionTesting {

    def _dbConnection: DbConnection

    val contextSource = new MockContextSource(_dbConnection)

    def authQueue = contextSource.authQueue
    def popAuth: List[AuthRequest] = {
      val result = authQueue.toList
      authQueue.clear()
      result
    }

    def filterRequests = contextSource.filterRequests
    def filterResponses = contextSource.filterResponses

    def popFilterRequests: List[FilterRequest[_]] = {
      val result = filterRequests.toList
      filterRequests.clear()
      result
    }

    def events = contextSource.sink.events

    def eventCheck = events.map(s => (s.typ, s.value.getClass))

    def printEvents() {
      println(events.map(s => (s.typ, s.value.getClass.getSimpleName)).mkString("\n"))
    }
  }

  case class SubEvent(typ: SubscriptionEventType, value: AnyRef, key: String)
  class QueueingEventSink extends SubscriptionHandler {

    private var received = List.empty[SubEvent]
    def events = received.reverse

    def publishEvent[A](typ: SubscriptionEventType, value: A, key: String) {
      received ::= SubEvent(typ, value.asInstanceOf[AnyRef], key)
    }

    def bindQueueByClass[A](subQueue: String, key: String, klass: Class[A]) {}
  }

  class QueueingRequestContext(val subHandler: SubscriptionHandler, val auth: AuthzService) extends RequestContext with HeadersContext {
    def client = throw new Exception("Asked for client in silent request context")
    val eventSink = new SilentEventSink
    val operationBuffer = new BasicOperationBuffer
  }

  // TODO: merge userName setting with other mock request context
  class MockContextSource(dbConnection: DbConnection, var userName: String = "user01") extends RequestContextSource with AgentAddingContextSource {
    private var subHandler = new QueueingEventSink
    private var auth = new QueueingAuthz

    def reset() {
      subHandler = new QueueingEventSink
      auth = new QueueingAuthz
    }
    def sink = subHandler

    def authQueue = auth.queue
    def filterRequests = auth.filterRequestQueue
    def filterResponses = auth.filterResponseQueue

    def transaction[A](f: (RequestContext) => A): A = {
      val context = new QueueingRequestContext(subHandler, auth)
      context.set("user_name", userName)
      ServiceTransactable.doTransaction(dbConnection, context.operationBuffer, { b: OperationBuffer =>
        addUser(context)
        f(context)
      })
    }
  }

  case class FilterRequest[A](componentId: String, action: String, payload: List[A], uuids: List[List[UUID]])
  case class AuthRequest(resource: String, action: String, entities: List[String])
  class QueueingAuthz extends AuthzService with AuthzFilteringService {

    val queue = new scala.collection.mutable.Queue[AuthRequest]

    val filterRequestQueue = new scala.collection.mutable.Queue[FilterRequest[_]]
    val filterResponseQueue = new scala.collection.mutable.Queue[List[FilteredResult[_]]]

    // called by (actual) services
    def filter[A](context: RequestContext, componentId: String, action: String, payload: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]] = {
      filterRequestQueue.enqueue(FilterRequest(componentId, action, payload, uuids))
      filterResponseQueue.dequeue.asInstanceOf[List[FilteredResult[A]]]
    }
    // called to "check" permissions
    def filter[A](permissions: => List[Permission], service: String, action: String, payloads: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]] = {
      filterRequestQueue.enqueue(FilterRequest(service, action, payloads, uuids))
      filterResponseQueue.dequeue.asInstanceOf[List[FilteredResult[A]]]
    }

    def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {

      val names = uuids.map { ApplicationSchema.entities.lookup(_).get.name }

      queue.enqueue(AuthRequest(componentId, action, names))
    }

    def prepare(context: RequestContext) {
      context.set(AuthzService.filterService, this)
    }

  }

}