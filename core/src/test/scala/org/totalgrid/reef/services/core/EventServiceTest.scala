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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Events.{ Event => EventProto }
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto }
import org.totalgrid.reef.services.core.util._

import org.totalgrid.reef.messaging.mock.AMQPFixture

import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.Application._
import org.totalgrid.reef.protoapi.{ ProtoServiceException, RequestEnv, ProtoServiceTypes }
import org.totalgrid.reef.messaging.ServiceRequestHandler

import org.totalgrid.reef.models._

import org.totalgrid.reef.event._
import org.totalgrid.reef.event.EventType.eventTypeToString
import org.totalgrid.reef.event.SilentEventLogPublisher

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers

@RunWith(classOf[JUnitRunner])
class EventServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {
  val ALARM = EventConfig.Designation.ALARM.getNumber
  val EVENT = EventConfig.Designation.EVENT.getNumber
  val LOG = EventConfig.Designation.LOG.getNumber

  override def beforeAll() {
    import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
    DbConnector.connect(DbInfo.loadInfo("test"))
    transaction { ApplicationSchema.reset }
    seedEventConfigTable
  }

  def seedEventConfigTable() {
    import org.squeryl.PrimitiveTypeMode._
    import org.squeryl.Table
    import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }
    import EventType._

    val ecs = List[EventConfigStore](
      //               EventType    SEVERITY  DESIGNATION ALARM_STATE RESOURCE
      EventConfigStore(System.UserLogin, 7, EVENT, 0, "User logged in"),
      EventConfigStore(System.UserLogout, 7, EVENT, 0, "User logged out"),
      EventConfigStore(System.SubsystemStarting, 8, LOG, 0, "Subsystem is starting"),
      EventConfigStore(System.SubsystemStarted, 8, LOG, 0, "Subsystem has started"),
      EventConfigStore(System.SubsystemStopping, 8, LOG, 0, "Subsystem is stapping"),
      EventConfigStore(System.SubsystemStopped, 8, LOG, 0, "Subsystem has stopped"),
      EventConfigStore(Scada.ControlExe, 3, ALARM, AlarmModel.UNACK_AUDIBLE, "User executed control {attr0} on device {attr1}"))

    transaction {
      ecs.foreach(ApplicationSchema.eventConfigs.insert(_))
    }
  }

  test("Create Events and Alarms") {
    import EventType._
    import EventConfig.Designation

    transaction {
      val factories = new ModelFactories(new SilentEventPublishers, new SilentSummaryPoints)
      val eventService = factories.events.model
      // Post an event
      var event = eventService.createFromProto(makeEvent(System.UserLogin))
      event.alarm should be(false)
      event.severity should be(7)
      event.rendered should be("User logged in")

      // Post an event that is an alarm
      event = eventService.createFromProto(makeEvent(Scada.ControlExe))
      event.alarm should be(true)
      event.severity should be(3)
      event.rendered should be("User executed control val0 on device val1")
      val alarm = event.associatedAlarm.value
      alarm.eventUid should be(event.id)
      alarm.state should be(AlarmModel.UNACK_AUDIBLE)

    }
  }

  test("Update Alarm States") {
    import EventType._

    transaction {
      val factories = new ModelFactories(new SilentEventPublishers, new SilentSummaryPoints)
      val eventService = factories.events.model
      val alarmService = factories.alarms.model

      // Post an event that is an alarm
      var event = eventService.createFromProto(makeEvent(Scada.ControlExe))
      event.alarm should be(true)
      event.severity should be(3)
      var alarm = event.associatedAlarm.value
      alarm.eventUid should be(event.id)
      alarm.state should be(AlarmModel.UNACK_AUDIBLE)

      // Can't go to REMOVED
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.REMOVED), alarm) }

      // Update to UNACK_SILENT
      {
        val (alarm2, modified) = alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_SILENT), alarm)
        modified should be(true)
        alarm2.state should be(AlarmModel.UNACK_SILENT)
        alarm = alarm2
      }

      // Can't go back to UNACK_AUDIBLE
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_AUDIBLE), alarm) }

      // Update to ACKNOWLEDGED
      {
        val (alarm2, modified) = alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.ACKNOWLEDGED), alarm)
        modified should be(true)
        alarm2.state should be(AlarmModel.ACKNOWLEDGED)
        alarm = alarm2
      }
      // Update to ACKNOWLEDGED again
      {
        val (alarm2, modified) = alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.ACKNOWLEDGED), alarm)
        modified should be(false)
        alarm2.state should be(AlarmModel.ACKNOWLEDGED)
        alarm = alarm2
      }

      // Can't go back to UNACK_*
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_AUDIBLE), alarm) }
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_SILENT), alarm) }

      // Update to REMOVED
      {
        val (alarm2, modified) = alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.REMOVED), alarm)
        modified should be(true)
        alarm2.state should be(AlarmModel.REMOVED)
        alarm = alarm2
      }

      // Can't go back to ACKNOWLEDGED or UNACK_*
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_AUDIBLE), alarm) }
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.UNACK_SILENT), alarm) }
      intercept[ProtoServiceException] { alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.ACKNOWLEDGED), alarm) }

      // Check UNACK_AUDIBLE straight to ACKNOWLEDGED with a new event
      event = eventService.createFromProto(makeEvent(Scada.ControlExe))
      alarm = event.associatedAlarm.value
      alarm.state should be(AlarmModel.UNACK_AUDIBLE); // need semicolon!

      // Update to ACKNOWLEDGED
      {
        val (alarm2, modified) = alarmService.updateFromProto(makeAlarm(alarm.id, Alarm.State.ACKNOWLEDGED), alarm)
        modified should be(true)
        alarm2.state should be(AlarmModel.ACKNOWLEDGED)
        alarm = alarm2
      }
    }
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

    EventProto.newBuilder
      .setTime(0)
      .setDeviceTime(0)
      .setEventType(event)
      .setSubsystem("FEP")
      .setUserId("flint")
      .setEntity(EntityProto.newBuilder.setUid("42").build)
      .setArgs(alist.toProto)
      .build
  }

  /**
   * Make an Event
   */
  def makeAlarm(uid: Long, state: Alarm.State) =
    Alarm.newBuilder
      .setUid(uid.toString)
      .setState(state)
      .build
}
