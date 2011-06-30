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

package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.FEP.{ CommChannel, FrontEndProcessor, CommEndpointRouting }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig => ConfigProto, CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.Application.ApplicationConfig

import org.totalgrid.reef.protocol.api.mock.{ NullProtocol, RecordingProtocol }
import org.totalgrid.reef.event._

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import RecordingProtocol._
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.proto.Model.ReefUUID
import org.totalgrid.reef.executor.mock.MockExecutor
import org.junit.runner.RunWith
import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt
import org.totalgrid.reef.messaging.mock.synchronous.MockConnection.EventQueueRecord
import org.totalgrid.reef.messaging.mock.synchronous.MockConnection
import org.totalgrid.reef.sapi.client._

// TODO - Test that FrontEndConnections binds a service, right exception is being caught internally. Probably best to mock this class.

@RunWith(classOf[JUnitRunner])
class FrontEndManagerTests extends FunSuite with ShouldMatchers {

  private def makeUuid(str: String) = ReefUUID.newBuilder.setUuid(str).build

  implicit def makeUuidFromString(str: String): ReefUUID = makeUuid(str)

  def fixture(test: (MockConnection, MockExecutor, RecordingProtocol, FrontEndManager) => Unit) = {

    val conn = new MockConnection
    val exe = new MockExecutor
    val mp = new NullProtocol("mock") with RecordingProtocol {
      override def requiresChannel = true
    }
    val eventLog = SilentEventLogPublisher // publishers for events and logs
    val appConfig = ApplicationConfig.newBuilder.setUuid("0").build
    val fem = new FrontEndManager(conn, exe, List(mp), eventLog, appConfig, 5000)

    fem.start()
    test(conn, exe, mp, fem)
  }

  def verifyProtocolAdditions(protocol: RecordingProtocol, channel: String, endpoint: String) {
    protocol.next() should equal(Some(AddChannel(channel)))
    protocol.next() should equal(Some(AddEndpoint(endpoint, channel, Nil)))
    protocol.next() should equal(None)
  }

  def respondToAnnounce(conn: MockConnection, status: Envelope.Status = Envelope.Status.OK): FrontEndProcessor = {
    conn.session.numRequestsPending should equal(1)
    val request = conn.session.respond[FrontEndProcessor] { request =>
      request.verb should equal(Envelope.Verb.PUT)
      val rsp = FrontEndProcessor.newBuilder(request.payload).setUuid("someuid").build
      Response(status, rsp)
    }
    conn.session.numRequestsPending should equal(0)
    request.payload
  }

  def initEventQueue(conn: MockConnection): EventQueueRecord[ConnProto] = {
    conn.eventQueueSize should equal(1)
    val record = conn.expectEventQueueRecord[ConnProto]
    record.onNewQueue("queue")
    conn.eventQueueSize should equal(0)
    record
  }

  def getConnProto(fep: FrontEndProcessor): ConnProto = {
    val pt = CommChannel.newBuilder.setUuid("port")
    val cfg = ConfigProto.newBuilder.setUuid("config").setChannel(pt).setName("endpoint1")
    val rt = CommEndpointRouting.newBuilder
    ConnProto.newBuilder.setUid("connection").setFrontEnd(fep).setEndpoint(cfg).setRouting(rt).setEnabled(true).build
  }

  def responseToConfigProto(channelName: String)(request: Request[ConfigProto]): Response[ConfigProto] = {
    request.verb should equal(Envelope.Verb.GET)
    val rsp = ConfigProto.newBuilder(request.payload).setProtocol("mock").setChannel(
      CommChannel.newBuilder.setUuid(channelName).setName(channelName)).build
    Response(Envelope.Status.OK, List(rsp))
  }

  def responseToCommChannel(request: Request[CommChannel]): Response[CommChannel] = {
    request.verb should equal(Envelope.Verb.GET)
    Response(Envelope.Status.OK, List(request.payload))
  }

  test("Announces on startup") {
    fixture((conn, exe, mp, fem) => respondToAnnounce(conn))
  }

  test("Retries announces with executor delay") {
    fixture { (conn, exe, mp, fem) =>
      3.times {
        respondToAnnounce(conn, Envelope.Status.INTERNAL_ERROR)
        exe.delayNext(1, 0) should equal(5000)
      }
    }
  }

  test("Subcribes after announcing") {
    fixture { (conn, exe, mp, fem) =>
      conn.eventQueueSize should equal(0)
      val fep = respondToAnnounce(conn)
      initEventQueue(conn)
      conn.session.respond[ConnProto](request => Success(Envelope.Status.OK, Nil))
      exe.executeNext(1, 0)
    }
  }

  test("Adds to protocol via subscribe") {
    fixture { (conn, exe, mp, fem) =>
      val fep = respondToAnnounce(conn)
      initEventQueue(conn)

      conn.session.respond[ConnProto](request => Success(Envelope.Status.OK, List(getConnProto(fep))))
      conn.session.queueResponse(responseToConfigProto("channel1"))
      conn.session.queueResponse(responseToCommChannel)
      exe.executeNext(1, 0)

      conn.session.numResponsesPending should equal(0)
      verifyProtocolAdditions(mp, "channel1", "endpoint1")
    }
  }

  /* When the FEP gets a device connection via an event, it should go get all the related data and add then start the port/device just like it does via subscription */
  test("Adds to protocol via event") {
    fixture { (conn, exe, mp, fem) =>
      val fep = respondToAnnounce(conn)
      val record = initEventQueue(conn)

      conn.session.respond[ConnProto](request => Success(Envelope.Status.OK, Nil))
      exe.executeNext(1, 0)
      record.onEvent(Event(Envelope.Event.ADDED, getConnProto(fep)))
      conn.session.queueResponse(responseToConfigProto("channel1"))
      conn.session.queueResponse(responseToCommChannel)
      exe.executeNext(1, 0)

      conn.session.numResponsesPending should equal(0)
      verifyProtocolAdditions(mp, "channel1", "endpoint1")
    }
  }

}
