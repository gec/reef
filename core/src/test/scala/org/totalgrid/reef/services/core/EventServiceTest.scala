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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.models.DatabaseUsingTestBase

import org.totalgrid.reef.proto.Events.{ Event => EventProto }
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.OptionalProtos._

import org.totalgrid.reef.services.framework.SystemEventCreator
import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.japi.ReefServiceException

import SyncServiceShims._

@RunWith(classOf[JUnitRunner])
class EventServiceTest extends DatabaseUsingTestBase with SystemEventCreator {

  class Fixture {
    val factories = new ModelFactories()
    val eventService = new EventService(factories.events)
    val eventConfigService = new EventConfigService(factories.eventConfig)
    val alarmService = new AlarmService(factories.alarms)
    val headers = new RequestEnv
    headers.setUserName("user")

    def publishEvent(evt: EventProto): EventProto = {
      eventService.put(evt, headers).expectOne
    }
    def createConfig(evt: EventConfig): EventConfig = {
      eventConfigService.put(evt, headers).expectOne
    }

    def updateAlarm(alarm: Alarm): Alarm = {
      val ret = alarmService.put(alarm, headers).expectOne
      ret.state should equal(alarm.state)
      ret
    }
  }

  test("Create Simple event") {
    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Event"), Some(7), Some(EventConfig.Designation.EVENT), Some("Test Event")))
    // Post an event
    val event = fix.publishEvent(createSystemEvent("Test.Event", "FEP").build)
    event.alarm.get should be(false)
    event.severity.get should be(7)
    event.rendered.get should be("Test Event")
    event.time.get should not be (0)
    event.deviceTime should equal(None)
    event.userId.get should equal("user")
    event.subsystem.get should equal("FEP")
    event.entity.isDefined should equal(false)
    event.args.isDefined should equal(false)
  }

  test("Create Event with args") {
    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Event"), Some(7), Some(EventConfig.Designation.EVENT), Some("{attr0} {attr1}")))
    // Post an event
    val event = fix.publishEvent(createSystemEvent("Test.Event", "FEP", args = "attr0" -> "val0" :: "attr1" -> "val1" :: Nil).build)
    event.rendered.get should be("val0 val1")
    val attrs = event.args.get.attribute.get
    attrs.size should equal(2)
  }

  test("Create Event with wrong args") {
    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Event"), Some(7), Some(EventConfig.Designation.EVENT), Some("{attr0} {attr1}")))

    val event = fix.publishEvent(createSystemEvent("Test.Event", "FEP", args = "badattr0" -> "val0" :: "badattr1" -> "val1" :: Nil).build)
    event.rendered.get should be("{attr0} {attr1}")
  }

  test("Create Alarms") {
    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Alarm"), Some(3), Some(EventConfig.Designation.ALARM), Some("Test Alarm"), Some(Alarm.State.UNACK_AUDIBLE)))
    // Post an event that is an alarm
    val event = fix.publishEvent(createSystemEvent("Test.Alarm", "FEP").build)
    event.alarm.get should be(true)
    event.severity.get should be(3)
    event.rendered.get should be("Test Alarm")
    event.uid should not be (None)

    val alarm = fix.alarmService.get(makeAlarm(event)).expectOne
    alarm.state.get should be(Alarm.State.UNACK_AUDIBLE)

  }

  test("Update Alarm States") {

    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Alarm"), Some(3), Some(EventConfig.Designation.ALARM), Some("Test Alarm"), Some(Alarm.State.UNACK_AUDIBLE)))
    // Post an event that is an alarm
    val event = fix.publishEvent(createSystemEvent("Test.Alarm", "FEP").build)
    event.alarm.get should be(true)

    val alarm = fix.alarmService.get(makeAlarm(event)).expectOne
    alarm.state.get should be(Alarm.State.UNACK_AUDIBLE)

    // Can't go to REMOVED
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.REMOVED)) }

    // Update to UNACK_SILENT
    fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_SILENT))

    // Can't go back to UNACK_AUDIBLE
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_AUDIBLE)) }

    // Update to ACKNOWLEDGED
    fix.updateAlarm(makeAlarm(alarm, Alarm.State.ACKNOWLEDGED))

    // Can't go back to UNACK_*
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_AUDIBLE)) }
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_SILENT)) }

    // Update to REMOVED
    fix.updateAlarm(makeAlarm(alarm, Alarm.State.REMOVED))

    // Can't go back to ACKNOWLEDGED or UNACK_*
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_AUDIBLE)) }
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.UNACK_SILENT)) }
    intercept[ReefServiceException] { fix.updateAlarm(makeAlarm(alarm, Alarm.State.ACKNOWLEDGED)) }

  }
  test("Alarm straight to acknowledged") {
    // Check UNACK_AUDIBLE straight to ACKNOWLEDGED with a new event
    val fix = new Fixture

    fix.createConfig(makeEc(Some("Test.Alarm"), Some(3), Some(EventConfig.Designation.ALARM), Some(""), Some(Alarm.State.UNACK_AUDIBLE)))

    val event = fix.publishEvent(createSystemEvent("Test.Alarm", "FEP").build)
    val alarm = fix.alarmService.get(makeAlarm(event)).expectOne

    // Update to ACKNOWLEDGED
    fix.updateAlarm(makeAlarm(alarm, Alarm.State.ACKNOWLEDGED))
  }

  ////////////////////////////////////////////////////////
  // Utilities

  /**
   * Make an Event
   */
  def makeAlarm(alarm: Alarm, state: Alarm.State) =
    Alarm.newBuilder
      .setUid(alarm.getUid)
      .setState(state)
      .build

  /**
   * Make an Event
   */
  def makeAlarm(event: EventProto) =
    Alarm.newBuilder.setEvent(event).build

  def makeEc(event: Option[String],
    severity: Option[Int] = None,
    designation: Option[EventConfig.Designation] = None,
    resource: Option[String] = None,
    alarmState: Option[Alarm.State] = None) = {

    val b = EventConfig.newBuilder
    event.foreach(b.setEventType(_))
    severity.foreach(b.setSeverity(_))
    designation.foreach(b.setDesignation(_))
    resource.foreach(b.setResource(_))
    alarmState.foreach(b.setAlarmState(_))
    b.build
  }
}
