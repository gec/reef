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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.FEP.{ CommChannel, FrontEndProcessor, CommEndpointRouting }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig => ConfigProto, CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.Application.ApplicationConfig

import org.totalgrid.reef.api.ServiceTypes.{ Response, Event }
import org.totalgrid.reef.messaging.mock.{ MockConnection, MockEvent }
import org.totalgrid.reef.protocol.api.MockProtocol
import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.event._

import org.totalgrid.reef.api.ServiceHandlerHeaders.convertRequestEnvToServiceHeaders

import org.scalatest.fixture.FixtureSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import MockProtocol._
import org.totalgrid.reef.api.Envelope

@RunWith(classOf[JUnitRunner])
class FrontEndActorTests extends FixtureSuite with ShouldMatchers {

  case class Fixture(conn: MockConnection, protocol: MockProtocol, a: FrontEndActor) {
    val client = conn.getMockClient
  }

  type FixtureParam = Fixture

  def withFixture(test: OneArgTest) = {

    val conn = new MockConnection {}
    val mp = new MockProtocol
    val eventLog = SilentEventLogPublisher // publishers for events and logs
    val appConfig = ApplicationConfig.newBuilder.setUid("0").build

    //immediately retry for testing purposes
    val a = new FrontEndActor(conn, List(mp), eventLog, appConfig, 0) with ReactActor
    try {
      a.start
      test(Fixture(conn, mp, a))
    } finally {}
  }

  def sendEventQueueNotifications(conn: MockConnection) {

    case class Mapping(val klass: Class[_], val queue: String)

    val queues = List(
      Mapping(classOf[ConnProto], "connqueue"))

    queues.foreach { x =>
      conn.getEvent(x.klass) match {
        case MockEvent(accept, Some(notify)) => notify(x.queue)
        case _ => assert(false)
      }
    }
  }

  def respondToAnnounce(conn: MockConnection, status: Envelope.Status): FrontEndProcessor = {

    var ret: Option[FrontEndProcessor] = None

    conn.getMockClient.respond[FrontEndProcessor] { request =>
      ret = Some(request.payload)
      request.verb should equal(Envelope.Verb.PUT)
      val rsp = FrontEndProcessor.newBuilder(request.payload).setUid("someuid")
      Some(Response(status, List(rsp.build)))
    }

    ret.get
  }

  def respondToShutdown(conn: MockConnection): Unit = {

    conn.getMockClient.respond[FrontEndProcessor] { request =>
      request.verb should equal(Envelope.Verb.PUT)
      val rsp = FrontEndProcessor.newBuilder(request.payload).setUid("someuid").build
      Some(Response(Envelope.Status.OK, List(rsp)))
    }
  }

  def respondToSubscribe(conn: MockConnection, name: String)(validatePayload: ConnProto => List[ConnProto]) = {

    conn.getMockClient.respond[ConnProto] { request =>
      request.verb should equal(Envelope.Verb.GET)
      request.env.subQueue should equal(Some(name))
      Some(Response(Envelope.Status.OK, validatePayload(request.payload)))
    }
  }

  def respondToConnectionSubscribe(conn: MockConnection, list: List[ConnProto]) = {
    respondToSubscribe(conn, "connqueue") { (x: ConnProto) =>
      x.getFrontEnd.getUid should equal("someuid")
      list
    }
  }

  def verifyProtocolAdditions(protocol: MockProtocol, port: String, endpoint: String) {

    protocol.checkFor {
      case AddPort(p) => p.getName should equal(port)
    }

    protocol.checkFor {
      case AddEndpoint(e, p, list) =>
        e should equal(endpoint)
        p should equal(port)
        list should equal(Nil)
    }
  }

  def respondToSingleGet(fix: Fixture, portname: String) {
    fix.client.respond[ConfigProto] { request =>
      request.verb should equal(Envelope.Verb.GET)
      val port = CommChannel.newBuilder.setUid(portname).setName(portname).build
      val rsp = ConfigProto.newBuilder(request.payload).setProtocol("mock").setChannel(port).build
      Some(Response(Envelope.Status.OK, List(rsp)))
    }
    fix.client.respond[CommChannel] { request =>
      request.verb should equal(Envelope.Verb.GET)
      Some(Response(Envelope.Status.OK, List(request.payload)))
    }
  }

  def testSingleAnnounce(fix: Fixture) {
    respondToAnnounce(fix.conn, Envelope.Status.OK)
  }

  def testAnnounceRetry(fix: Fixture) {
    respondToAnnounce(fix.conn, Envelope.Status.INTERNAL_ERROR)
    respondToAnnounce(fix.conn, Envelope.Status.OK)
  }

  /// When the FEP gets the uid, it should subscribe to slave device connections
  def testSubscribeAfterUID(fix: Fixture) {
    respondToAnnounce(fix.conn, Envelope.Status.OK)
    sendEventQueueNotifications(fix.conn)
    respondToConnectionSubscribe(fix.conn, Nil) // respond with no device connections
  }

  /** When the FEP gets a device connection via subscription,		it should go get all the related data and add then start the port/device */
  def testProtocolAddViaSubscribe(fix: Fixture) {
    val fep = respondToAnnounce(fix.conn, Envelope.Status.OK)
    sendEventQueueNotifications(fix.conn)
    val pt = CommChannel.newBuilder.setUid("port")
    val cfg = ConfigProto.newBuilder.setUid("config").setChannel(pt).setName("connection")
    val rt = CommEndpointRouting.newBuilder
    val conn = ConnProto.newBuilder.setUid("connection").setFrontEnd(fep).setEndpoint(cfg).setRouting(rt).build
    respondToConnectionSubscribe(fix.conn, List(conn)) //return 1 subscription     
    respondToSingleGet(fix, "port") //respond to the request to read the config/port

    //at this point the fep should have all of things it needs to add a device,
    //so we can check the mock protocol to see if things get added correctly
    verifyProtocolAdditions(fix.protocol, "port", "connection")
  }

  /** When the FEP gets a device connection via an event,		it should go get all the related data and add then start the port/device		just like it does via subscription */
  def testProtocolAddViaEvent(fix: Fixture) {

    val fep = respondToAnnounce(fix.conn, Envelope.Status.OK)
    sendEventQueueNotifications(fix.conn)
    respondToConnectionSubscribe(fix.conn, Nil) //return 0 subscriptions

    val pt = CommChannel.newBuilder.setUid("port")
    val cfg = ConfigProto.newBuilder.setUid("config").setChannel(pt).setName("connection")
    val rt = CommEndpointRouting.newBuilder
    val conn = ConnProto.newBuilder.setUid("connection").setFrontEnd(fep).setEndpoint(cfg).setRouting(rt).build

    fix.conn.getEvent(classOf[ConnProto]).accept(Event(Envelope.Event.ADDED, conn))

    respondToSingleGet(fix, "port") //respond to the request to read the config/port

    //at this point the fep should have all of things it needs to add a device,
    //so we can check the mock protocol to see if things get added correctly
    verifyProtocolAdditions(fix.protocol, "port", "connection")
  }

}
