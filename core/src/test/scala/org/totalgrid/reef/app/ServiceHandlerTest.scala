/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.app

import org.totalgrid.reef.messaging.Connection
import org.totalgrid.reef.sapi.client.{ Response, Event }
import org.totalgrid.reef.messaging.mock.MockConnection
import org.totalgrid.reef.proto.{ Processing }
import org.totalgrid.reef.proto.Model.Point

import Processing._

import org.totalgrid.reef.reactor.ReactActor
import scala.concurrent.MailBox

import org.totalgrid.reef.japi.Envelope

class ServiceHandlerMock(conn: Connection, retryMS: Long) {

  val act = new ReactActor with ServiceHandler

  def start() = act.start

  val query = MeasOverride.newBuilder.setPoint(Point.newBuilder.setName("*")).build
  val tranClient = act.addService(conn, retryMS, MeasOverride.parseFrom, query, this.onResponse, this.onEvent)

  val box = new MailBox

  def onResponse(result: List[MeasOverride]) = box.send(result)
  def onEvent(event: Envelope.Event, result: MeasOverride) = box.send((event, result))
}
import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ServiceHandlerTest extends Suite with ShouldMatchers {

  def testServiceRetry {
    val conn = new MockConnection {}
    val tester = new ServiceHandlerMock(conn, 50)

    val queueEvent = conn.getEvent(classOf[MeasOverride])
    val notify: String => Unit = queueEvent.observer match {
      case None => assert(false); null
      case Some(note) => note
    }
    val eventAccept = queueEvent.accept

    tester.start()

    notify("queue01")
    val cons = conn.getMockClient
    cons.respond[MeasOverride] { request =>
      request.verb should equal(Envelope.Verb.GET)
      request.env.subQueue should equal(Some("queue01"))
      request.payload.getPoint.getName should equal("*")
      None
    }
    cons.respond[MeasOverride] { request =>
      request.verb should equal(Envelope.Verb.GET)
      request.env.subQueue should equal(Some("queue01"))
      request.payload.getPoint.getName should equal("*")
      Some(Response[MeasOverride](Envelope.Status.OK))
    }

    tester.box.receiveWithin(5000) {
      case Nil =>
    }
  }

  def testRouting {
    val conn = new MockConnection {}
    val tester = new ServiceHandlerMock(conn, 5000)

    val queueEvent = conn.getEvent(classOf[MeasOverride])
    val notify: String => Unit = queueEvent.observer.get
    val eventAccept = queueEvent.accept

    tester.start()

    notify("queue01")
    val cons = conn.getMockClient
    cons.respond[MeasOverride] { request =>
      request.verb should equal(Envelope.Verb.GET)
      request.env.subQueue should equal(Some("queue01"))
      request.payload.getPoint.getName should equal("*")
      Some(Response[MeasOverride](Envelope.Status.OK))
    }

    tester.box.receiveWithin(5000) {
      case Nil => //tests for empty list       
    }

    val trans = MeasOverride.newBuilder.setPoint(Point.newBuilder.setName("meas01"))

    eventAccept(Event(Envelope.Event.ADDED, trans.build))

    tester.box.receiveWithin(5000) {
      case (event, result) =>
        event should equal(Envelope.Event.ADDED)
    }
  }

}