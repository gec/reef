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
package org.totalgrid.reef.app

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.executor.mock.MockExecutor
import scala.collection.mutable._

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers

import org.totalgrid.reef.sapi.client.{ SuccessResponse, FailureResponse, Event }
import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt
import org.totalgrid.reef.messaging.mock.synchronous.{ MockSession, MockConnection }

@RunWith(classOf[JUnitRunner])
class ServiceHandlerTest extends FunSuite with ShouldMatchers {

  class EventReceiver[A] {
    val responses = new Queue[List[A]]
    val events = new Queue[Event[A]]

    def onResponse(result: List[A]) = responses.enqueue(result)
    def onEvent(event: Envelope.Event, result: A) = events.enqueue(Event(event, result))
  }

  def fixture(test: (MockExecutor, MockConnection, EventReceiver[Int]) => Unit) = {
    val exe = new MockExecutor
    val conn = new MockConnection
    val receiver = new EventReceiver[Int]

    val handler = new ServiceHandler(exe)
    handler.addService(conn, 5000, _ => 99, 3, receiver.onResponse, receiver.onEvent)

    test(exe, conn, receiver)
  }

  test("Service subscriptions are retried") {
    fixture { (exe, conn, receiver) =>

      conn.eventQueueSize should equal(1)
      val record = conn.expectEventQueueRecord[Int]
      conn.eventQueueSize should equal(0)
      record.onNewQueue("queue01")
      exe.numActionsPending should equal(0)

      3.times {
        conn.session.respond[Int] { request =>
          request.verb should equal(Envelope.Verb.GET)
          request.env.subQueue should equal(Some("queue01"))
          request.payload should equal(3)
          FailureResponse(Envelope.Status.INTERNAL_ERROR)
        }
        exe.delayNext(1, 0) should equal(5000)
      }

    }
  }

  def testSuccessfulResponse(list: List[Int]) {
    fixture { (exe, conn, receiver) =>
      conn.eventQueueSize should equal(1)
      val record = conn.expectEventQueueRecord[Int]
      conn.eventQueueSize should equal(0)
      record.onNewQueue("queue01")

      conn.session.respond[Int](request => SuccessResponse(Envelope.Status.OK, list))
      conn.session.numRequestsPending should equal(0)

      receiver.responses.size should equal(0) //posting the successful response is deferred
      exe.executeNext(1, 0)
      receiver.responses.size should equal(1)
      receiver.events.size should equal(0)
      receiver.responses.dequeue() should equal(list)
    }
  }

  test("Non-empty successful response causes correct notifications") { testSuccessfulResponse(List(1, 2, 3)) }

  test("Empty successful response still causes notification") { testSuccessfulResponse(Nil) }

  test("Correct event routing") {
    fixture { (exe, conn, receiver) =>

      conn.eventQueueSize should equal(1)
      val record = conn.expectEventQueueRecord[Int]
      conn.eventQueueSize should equal(0)
      record.onNewQueue("queue01")

      val event = Event(Envelope.Event.ADDED, 99)
      record.onEvent(event)
      exe.executeNext(1, 0)
      receiver.events.size should equal(1)
      receiver.events.dequeue() should equal(event)
    }
  }

}