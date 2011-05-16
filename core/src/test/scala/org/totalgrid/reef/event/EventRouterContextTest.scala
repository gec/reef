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

//import scala.collection.mutable

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.services.core.util._

import EventType.eventTypeToString

import org.scalatest.fixture.FixtureSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class EventRouterContextTest extends FixtureSuite with ShouldMatchers {

  val CRITICAL = 1
  val INFORM = 7

  class Sink {
    var event: Event = null
    var eventStore: Event = null
    var log: Log = null

    //    def publishEvent( e: Event) = { event = e }
    //    def publishLog( l: Log) = { log = l}
    //    def storeEvent( e: Event) = { eventStore = e}

    def severity = event.getSeverity
  }

  case class Fixture(context: EventRouterContext, sink: Sink)

  type FixtureParam = Fixture

  def withFixture(test: OneArgTest) = {
    import EventType._
    import EventConfig.Designation

    val sink = new Sink
    val context = new EventRouterContext(sink.event_=, sink.log_=, sink.eventStore_=)

    context.add(makeEC(Scada.ControlExe, CRITICAL, Designation.EVENT, 0))
    context.add(makeEC(System.UserLogin, INFORM, Designation.EVENT, 0))
    context.add(makeEC(System.UserLogout, INFORM, Designation.EVENT, 0))

    test(Fixture(context, sink))
  }

  def testReceiveEvent(fixture: Fixture) {
    import fixture._
    import EventType._

    context.process(makeEvent(Scada.ControlExe))
    sink.severity should equal(CRITICAL)
    sink.eventStore.getEventType should equal(Scada.ControlExe.toString)

    context.process(makeEvent(System.UserLogin))
    sink.severity should equal(INFORM)
    sink.eventStore.getEventType should equal(System.UserLogin.toString)

    // Unknown events default to CRITICAL
    context.process(makeEvent(System.SubsystemStarting))
    sink.severity should equal(CRITICAL)
    sink.eventStore.getEventType should equal(System.SubsystemStarting.toString)

  }

  ////////////////////////////////////////////////////////
  // Utilities

  /**
   * Make an Event
   */
  def makeEvent(event: EventType) = {
    val alist = new AttributeList
    alist += ("attr0" -> AttributeString("val0"))
    alist += ("attr1" -> AttributeString("val1"))

    Event.newBuilder
      .setTime(0)
      .setDeviceTime(0)
      .setEventType(event)
      .setSubsystem("FEP")
      .setUserId("flint")
      .setEntity(Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("42")).build)
      .setArgs(alist.toProto)
      .build
  }

  /**
   * Make an EventConfig
   */
  def makeEC(event: EventType, severity: Int, designation: EventConfig.Designation, alarmState: Int) = {
    val ec = EventConfig.newBuilder
      .setEventType(event)
      .setSeverity(severity)
      .setDesignation(designation)
    if (alarmState > 0)
      ec.setAlarmState(Alarm.State.valueOf(alarmState))

    ec.build
  }

}