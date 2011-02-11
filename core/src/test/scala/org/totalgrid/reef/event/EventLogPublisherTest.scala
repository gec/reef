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
package org.totalgrid.reef.event

import org.totalgrid.reef.messaging.mock.{ MockProtoRegistry, MockProtoPublisherRegistry }
import scala.concurrent.{ MailBox, TIMEOUT }
import org.totalgrid.reef.proto.{ Events }
import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.services.core.util._

import org.scalatest.fixture.FixtureSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import EventType.eventTypeToString
import org.totalgrid.reef.messaging.RoutingKeys

@RunWith(classOf[JUnitRunner])
class EventLogPublisherTest extends FixtureSuite with ShouldMatchers {

  case class Fixture(registry: MockProtoRegistry,
    mailEvent: MailBox,
    mailLog: MailBox,
    publishEvent: Events.Event => Unit,
    publishLog: Events.Log => Unit) {}
  type FixtureParam = Fixture

  def withFixture(test: OneArgTest) = {

    val registry = new MockProtoRegistry()

    val publishEvent = registry.publish(RoutingKeys.event, "raw")
    val publishLog = registry.publish(RoutingKeys.log, "raw")
    val mailEvent = registry.getMailbox(classOf[Events.Event])
    val mailLog = registry.getMailbox(classOf[Events.Log])

    test(Fixture(registry, mailEvent, mailLog, publishEvent, publishLog))
  }

  def testPublishEvent(fixture: Fixture) {
    import fixture._

    val alist = new AttributeList
    alist += ("attr0" -> AttributeString("val0"))
    alist += ("attr1" -> AttributeString("val1"))

    val e = Events.Event.newBuilder
      .setTime(0)
      .setDeviceTime(0)
      .setEventType(EventType.Scada.ControlExe)
      .setSubsystem("FEP")
      .setUserId("userId")
      .setEntity(Entity.newBuilder.setUid("42").build)
      .setArgs(alist.toProto)
      .build

    publishEvent(e)
    mailEvent.receiveWithin(0) {
      case x: Events.Event =>
      case _ => assert(false)
    }

  }

  def testPublishLog(fixture: Fixture) {
    import fixture._

    val l = Events.Log.newBuilder
      .setTime(0)
      .setSubsystem("FEP")
      .setLevel(Events.Level.INFO)
      .setMessage("test log message")
      .build

    publishLog(l)
    mailLog.receiveWithin(0) {
      case x: Events.Log =>
      case _ => assert(false)
    }

  }
}