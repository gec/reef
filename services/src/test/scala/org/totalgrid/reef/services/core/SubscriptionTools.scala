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
import org.totalgrid.reef.models.Entity

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

  class MockContextSource(dbConnection: DbConnection) extends RequestContextSource with AgentAddingContextSource {
    private var subHandler = new QueueingEventSink
    private var auth = new QueueingAuthz

    val userName = "user01"

    def reset() {
      subHandler = new QueueingEventSink
      auth = new QueueingAuthz
    }
    def sink = subHandler
    def authQueue = auth.queue

    def transaction[A](f: (RequestContext) => A): A = {
      val context = new QueueingRequestContext(subHandler, auth)
      ServiceTransactable.doTransaction(dbConnection, context.operationBuffer, { b: OperationBuffer =>
        addUser(context)
        f(context)
      })
    }
  }

  case class AuthRequest(resource: String, action: String, entities: List[String])
  class QueueingAuthz extends AuthzService {

    val queue = new scala.collection.mutable.Queue[AuthRequest]

    def authorize(context: RequestContext, componentId: String, action: String, entities: => List[Entity]) {
      queue.enqueue(AuthRequest(componentId, action, entities.map(_.name)))
    }

    def prepare(context: RequestContext) {}
  }

}