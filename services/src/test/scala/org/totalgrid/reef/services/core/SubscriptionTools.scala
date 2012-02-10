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
import org.totalgrid.reef.services.{ PermissionsContext, HeadersContext }
import org.totalgrid.reef.event.SilentEventSink
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.persistence.squeryl.DbConnection

object SubscriptionTools {

  trait SubscriptionTesting {

    def _dbConnection: DbConnection

    val contextSource = new MockContextSource(_dbConnection)

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

  class QueueingRequestContext(val subHandler: SubscriptionHandler) extends RequestContext with HeadersContext with PermissionsContext {
    def client = throw new Exception("Asked for client in silent request context")
    val eventSink = new SilentEventSink
    val operationBuffer = new BasicOperationBuffer
    //val subHandler = new QueueingEventSink
  }

  class MockContextSource(dbConnection: DbConnection) extends RequestContextSource {
    private var subHandler = new QueueingEventSink
    private def makeContext = {
      val context = new QueueingRequestContext(subHandler)
      context.modifyHeaders(_.setUserName("user"))
      context
    }

    def reset() {
      subHandler = new QueueingEventSink
    }
    def sink = subHandler

    def transaction[A](f: (RequestContext) => A): A = {
      val context = makeContext
      ServiceTransactable.doTransaction(dbConnection, context.operationBuffer, { b: OperationBuffer => f(context) })
    }
  }
}