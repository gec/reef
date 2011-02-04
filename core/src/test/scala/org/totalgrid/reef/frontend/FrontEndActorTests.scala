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

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.util.OneArgFunc
import org.totalgrid.reef.proto.{ Envelope }
import org.totalgrid.reef.proto.FEP.{ Port, FrontEndProcessor, CommunicationEndpointRouting }
import org.totalgrid.reef.proto.FEP.{ CommunicationEndpointConfig => ConfigProto, CommunicationEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.Application.ApplicationConfig

import org.totalgrid.reef.protoapi.ProtoServiceTypes.{ Response, Event }
import org.totalgrid.reef.messaging.mock.{ MockProtoRegistry, MockEvent }
import org.totalgrid.reef.protocol.api.MockProtocol
import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.event._

import org.totalgrid.reef.protoapi.ServiceHandlerHeaders.convertRequestEnvToServiceHeaders

import org.scalatest.fixture.FixtureSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import MockProtocol._

@RunWith(classOf[JUnitRunner])
class FrontEndActorTests extends FixtureSuite with ShouldMatchers {

  case class Fixture(reg: MockProtoRegistry, protocol: MockProtocol, a: FrontEndActor) {
    val config = reg.getServiceConsumerMock(classOf[ConfigProto])
    val port = reg.getServiceConsumerMock(classOf[Port])
    val fem = reg.getServiceConsumerMock(classOf[FrontEndProcessor])
  }

  type FixtureParam = Fixture

  def withFixture(test: OneArgTest) = {

    val reg = new MockProtoRegistry
    val mp = new MockProtocol
    val eventLog = SilentEventLogPublisher // publishers for events and logs
    val appConfig = ApplicationConfig.newBuilder.setUid("0").build

    //immediately retry for testing purposes
    val a = new FrontEndActor(reg, List(mp), eventLog, appConfig, 0) with ReactActor
    try {
      a.start
      test(Fixture(reg, mp, a))
    } finally {}
  }

  def sendEventQueueNotifications(reg: MockProtoRegistry) {

    case class Mapping(val klass: Class[_], val queue: String)

    val queues = List(
      Mapping(classOf[ConnProto], "connqueue"))

    queues.foreach { x =>
      reg.getEvent(x.klass) match {
        case MockEvent(accept, Some(notify)) => notify(x.queue)
        case _ => assert(false)
      }
    }
  }

  def respondToAnnounce(reg: MockProtoRegistry, status: Envelope.Status): FrontEndProcessor = {

    var ret: Option[FrontEndProcessor] = None

    reg.getServiceConsumerMock(classOf[FrontEndProcessor]).respond { request =>
      ret = Some(request.payload)
      request.verb should equal(Envelope.Verb.PUT)
      val rsp = FrontEndProcessor.newBuilder(request.payload).setUid("someuid")
      Some(Response(status, "", List(rsp.build)))
    }

    ret.get
  }

  def respondToShutdown(reg: MockProtoRegistry): Unit = {

    reg.getServiceConsumerMock(classOf[FrontEndProcessor]).respond { request =>
      request.verb should equal(Envelope.Verb.PUT)
      val rsp = FrontEndProcessor.newBuilder(request.payload).setUid("someuid").build
      Some(Response(Envelope.Status.OK, "", List(rsp)))
    }
  }

  def respondToSubscribe[T <: GeneratedMessage](reg: MockProtoRegistry, name: String)(validatePayload: T => List[T]) = {
    reg.getServiceConsumerMock(OneArgFunc.getParamClass(validatePayload, classOf[List[T]])).respond { request =>
      request.verb should equal(Envelope.Verb.GET)
      request.env.subQueue should equal(Some(name))
      Some(Response(Envelope.Status.OK, "", validatePayload(request.payload)))
    }
  }

  def respondToConnectionSubscribe(reg: MockProtoRegistry, list: List[ConnProto]) = {
    respondToSubscribe(reg, "connqueue") { (x: ConnProto) =>
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
    fix.config.respond { request =>
      request.verb should equal(Envelope.Verb.GET)
      val port = Port.newBuilder.setUid(portname).setName(portname).build
      val rsp = ConfigProto.newBuilder(request.payload).setProtocol("mock").setPort(port).build
      Some(Response(Envelope.Status.OK, "", List(rsp)))
    }
    fix.port.respond { request =>
      request.verb should equal(Envelope.Verb.GET)
      Some(Response(Envelope.Status.OK, "", List(request.payload)))
    }
  }

  def testSingleAnnounce(fix: Fixture) {
    respondToAnnounce(fix.reg, Envelope.Status.OK)
  }

  def testAnnounceRetry(fix: Fixture) {
    respondToAnnounce(fix.reg, Envelope.Status.INTERNAL_ERROR)
    respondToAnnounce(fix.reg, Envelope.Status.OK)
  }

  /// When the FEP gets the uid, it should subscribe to slave device connections
  def testSubscribeAfterUID(fix: Fixture) {
    respondToAnnounce(fix.reg, Envelope.Status.OK)
    sendEventQueueNotifications(fix.reg)
    respondToConnectionSubscribe(fix.reg, Nil) // respond with no device connections
  }

  /** When the FEP gets a device connection via subscription,		it should go get all the related data and add then start the port/device */
  def testProtocolAddViaSubscribe(fix: Fixture) {
    val fep = respondToAnnounce(fix.reg, Envelope.Status.OK)
    sendEventQueueNotifications(fix.reg)
    val pt = Port.newBuilder.setUid("port")
    val cfg = ConfigProto.newBuilder.setUid("config").setPort(pt).setName("connection")
    val rt = CommunicationEndpointRouting.newBuilder
    val conn = ConnProto.newBuilder.setUid("connection").setFrontEnd(fep).setEndpoint(cfg).setRouting(rt).build
    respondToConnectionSubscribe(fix.reg, List(conn)) //return 1 subscription     
    respondToSingleGet(fix, "port") //respond to the request to read the config/port

    //at this point the fep should have all of things it needs to add a device,
    //so we can check the mock protocol to see if things get added correctly
    verifyProtocolAdditions(fix.protocol, "port", "connection")
  }

  /** When the FEP gets a device connection via an event,		it should go get all the related data and add then start the port/device		just like it does via subscription */
  def testProtocolAddViaEvent(fix: Fixture) {

    val fep = respondToAnnounce(fix.reg, Envelope.Status.OK)
    sendEventQueueNotifications(fix.reg)
    respondToConnectionSubscribe(fix.reg, Nil) //return 0 subscriptions

    val pt = Port.newBuilder.setUid("port")
    val cfg = ConfigProto.newBuilder.setUid("config").setPort(pt).setName("connection")
    val rt = CommunicationEndpointRouting.newBuilder
    val conn = ConnProto.newBuilder.setUid("connection").setFrontEnd(fep).setEndpoint(cfg).setRouting(rt).build

    fix.reg.getEvent(classOf[ConnProto]).accept(Event(Envelope.Event.ADDED, conn))

    respondToSingleGet(fix, "port") //respond to the request to read the config/port

    //at this point the fep should have all of things it needs to add a device,
    //so we can check the mock protocol to see if things get added correctly
    verifyProtocolAdditions(fix.protocol, "port", "connection")
  }

}
