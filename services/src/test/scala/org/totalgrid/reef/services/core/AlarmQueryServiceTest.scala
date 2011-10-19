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

import org.totalgrid.reef.api.proto.Events._
import org.totalgrid.reef.api.proto.Alarms._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging.serviceprovider.SilentEventPublishers
import org.totalgrid.reef.api.japi.{ BadRequestException, Envelope }

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.models._
import org.totalgrid.reef.event._
import org.totalgrid.reef.event.EventType.eventTypeToString
import org.totalgrid.reef.services.core.util._

import java.util.{ Date, Calendar }
import org.totalgrid.reef.api.proto.Model.{ ReefUUID, Entity => EntityProto }
import org.totalgrid.reef.services.core.SyncServiceShims._
import org.totalgrid.reef.services.{ HeadersRequestContext, ServiceDependencies }

@RunWith(classOf[JUnitRunner])
class AlarmQueryServiceTest extends DatabaseUsingTestBase {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
  import Alarm.State._

  val ALARM = EventConfig.Designation.ALARM.getNumber
  val EVENT = EventConfig.Designation.EVENT.getNumber
  val LOG = EventConfig.Designation.LOG.getNumber

  val STATE_UNACK = List(UNACK_AUDIBLE, UNACK_AUDIBLE)
  val STATE_ACK = List(ACKNOWLEDGED)
  val STATE_REM = List(REMOVED)
  val STATE_ANY = List[Alarm.State]()

  // Let's use some values that are inaccurate, but easy to debug!
  val DAYS_AGO_2 = 10000L
  val DAYS_AGO_1 = 20000L
  val HOURS_AGO_2 = 30000L
  val HOURS_AGO_1 = 40000L
  val NOW = 50000L

  val CRITICAL = 1
  val INFORM = 6

  val USER_ANY = "*"
  val USER1 = "user1"
  val USER2 = "user2"
  val USER3 = "user3"

  // UIDs are strings in protos and long in the DB.
  var ENTITY1 = "Entity1"
  var ENTITY2 = "Entity2"
  var ENTITY_ANY = ""

  val SUB1 = "subsystem1"

  override def beforeAll() {
    super.beforeAll()
    val al = new AlarmAndEventInserter
    al.seedEventConfigTable
    al.seedMessages
  }

  class AlarmAndEventInserter {
    // Create the service factories
    //

    def seedEventConfigTable() {
      import org.squeryl.PrimitiveTypeMode._
      import org.squeryl.Table
      import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }
      import EventType._

      val ecs = List[EventConfigStore](
        //               EventType    SEVERITY  DESIGNATION ALARM_STATE RESOURCE
        EventConfigStore(System.UserLogin, 7, EVENT, 0, "User logged in", true),
        EventConfigStore(System.UserLogout, 7, EVENT, 0, "User logged out", true),
        EventConfigStore(System.SubsystemStarting, 8, LOG, 0, "Subsystem is starting", true),
        EventConfigStore(System.SubsystemStarted, 8, LOG, 0, "Subsystem has started", true),
        EventConfigStore(System.SubsystemStopping, 8, LOG, 0, "Subsystem is stapping", true),
        EventConfigStore(System.SubsystemStopped, 8, LOG, 0, "Subsystem has stopped", true),
        EventConfigStore(Scada.ControlExe, 3, ALARM, AlarmModel.UNACK_AUDIBLE, "User executed control {attr0} on device {attr1}", true))

      transaction {
        ApplicationSchema.eventConfigs.deleteWhere(e => true === true)
        ecs.foreach(ApplicationSchema.eventConfigs.insert(_))
      }
    }

    def seedMessages() {
      import org.squeryl.PrimitiveTypeMode._
      import org.squeryl.Table
      import org.totalgrid.reef.models.{ ApplicationSchema, EventStore }
      import EventType._

      transaction {

        // Post events to create alarms, events, and logs

        val entity1 = ApplicationSchema.entities.insert(new Entity(ENTITY1))
        val entity2 = ApplicationSchema.entities.insert(new Entity(ENTITY2))

        val factories = new ModelFactories(new ServiceDependencies)

        val eventService = factories.events
        val context = new HeadersRequestContext

        eventService.createFromProto(context, makeEvent(System.UserLogin, DAYS_AGO_2, USER1, None))
        eventService.createFromProto(context, makeEvent(Scada.ControlExe, DAYS_AGO_2 + 1000, USER1, Some(entity1.id.toString)))

        eventService.createFromProto(context, makeEvent(System.UserLogin, HOURS_AGO_2, USER2, None))
        eventService.createFromProto(context, makeEvent(Scada.ControlExe, HOURS_AGO_2 + 1000, USER2, Some(entity2.id.toString)))
        eventService.createFromProto(context, makeEvent(System.UserLogout, HOURS_AGO_2 + 2000, USER2, None))

        eventService.createFromProto(context, makeEvent(System.UserLogin, HOURS_AGO_1, USER3, None))
        eventService.createFromProto(context, makeEvent(Scada.ControlExe, HOURS_AGO_1 + 1000, USER3, Some(entity2.id.toString)))
        eventService.createFromProto(context, makeEvent(System.UserLogout, HOURS_AGO_1 + 2000, USER3, None))

        eventService.createFromProto(context, makeEvent(System.UserLogout, NOW, USER1, None))

      }
    }
  }

  import EventType._

  test("FailPutAlarmList") {
    val service = new AlarmQueryService

    val resp = service.put(makeAL(STATE_ANY, 0, 0, Some(Scada.ControlExe), USER_ANY, ENTITY_ANY))
    resp.status should equal(Envelope.Status.NOT_ALLOWED)
  }

  test("SimpleQueries") {
    val service = new AlarmQueryService

    // Select EventType only.
    //

    var resp = service.get(makeAL(STATE_ANY, 0, 0, Some(Scada.ControlExe), USER_ANY, ENTITY_ANY)).expectOne()
    resp.getAlarmsCount should equal(3)
    resp.getAlarmsList.toIterable.foreach(a => a.getEvent.getEventType should equal(Scada.ControlExe.toString))

    resp = service.get(makeAL(STATE_ANY, 0, 0, Some(System.UserLogin), USER_ANY, ENTITY_ANY)).expectOne()
    resp.getAlarmsCount should equal(0)

    // Select EventType and user
    //

    resp = service.get(makeAL(STATE_ANY, 0, 0, Some(System.UserLogout), USER1, ENTITY_ANY)).expectOne()
    resp.getAlarmsCount should equal(0)

    resp = service.get(makeAL(STATE_ANY, 0, 0, None, USER1, ENTITY_ANY)).expectOne()
    resp.getAlarmsCount should equal(1)

    // Select EventType, user, and entity
    //

    resp = service.get(makeAL(STATE_ANY, 0, 0, Some(Scada.ControlExe), USER1, ENTITY1)).expectOne()
    resp.getAlarmsCount should equal(1)
    resp.getAlarms(0).getEvent.getEventType should equal(Scada.ControlExe.toString)
    resp.getAlarms(0).getEvent.getUserId should equal(USER1)
    resp.getAlarms(0).getEvent.getEntity.getName should equal(ENTITY1)
  }

  test("Negative limit") {
    val req = AlarmList.newBuilder.setSelect(
      AlarmSelect.newBuilder
        .addAllState(STATE_ACK)
        .setEventSelect(EventSelect.newBuilder.setLimit(-1))).build

    val service = new AlarmQueryService

    intercept[BadRequestException] {
      service.get(req)
    }
  }

  /*
  def testQueriesWithSets(fixture: Fixture) {
    import fixture._
    import EventType._

    val empty = List[String]()
    val anyEventType = List[EventType]()

    var resp = one(service.get(makeAL(0, 0, List(Scada.ControlExe, System.UserLogin), empty, empty)))
    resp.getAlarmsCount should equal(6)

    resp = one(service.get(makeAL(0, 0, anyEventType, List(USER1, USER2), empty)))
    resp.getAlarmsCount should equal(6)

    resp = one(service.get(makeAL(0, 0, anyEventType, List(USER1, USER2, USER3), empty)))
    resp.getAlarmsCount should equal(9)

    resp = one(service.get(makeAL(0, 0, anyEventType, empty, List(ENTITY1, ENTITY2))))
    resp.getAlarmsCount should equal(3)

    resp = one(service.get(makeAL(0, 0, List(System.UserLogin, System.UserLogout), List(USER1, USER2, USER3), empty)))
    resp.getAlarmsCount should equal(6)

  }

  def testQueriesWithTime(fixture: Fixture) {
    import fixture._
    import EventType._

    var resp = one(service.get(makeAL(0, 0, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(9)

    resp = one(service.get(makeAL(HOURS_AGO_1, 0, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(4)
    //resp.getAlarmList.toIterable.foreach(e => e.getResourceId should equal(ENTITY2))

    resp = one(service.get(makeAL(HOURS_AGO_1, 0, System.UserLogout, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(2)
    resp.getAlarmList.toIterable.foreach(e => e.getEventType should equal(System.UserLogout.toString))

    resp = one(service.get(makeAL(DAYS_AGO_2, HOURS_AGO_2 + 9000, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(5)

    resp = one(service.get(makeAL(DAYS_AGO_2, HOURS_AGO_1 + 9000, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(8)

    // Just timeFrom
    resp = one(service.get(makeAL(DAYS_AGO_2, 0, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(9)
    resp = one(service.get(makeAL(HOURS_AGO_2, 0, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(7)

  }
*/
  /**
   *  Add some events to the database, and see if we're getting the updates.
   */
  /*
  def testUpdates(fixture: Fixture) {
    import fixture._
    import EventType._
    import org.squeryl.PrimitiveTypeMode._
    import org.squeryl.Table
    import org.totalgrid.reef.models.{ ApplicationSchema, EventStore }
    import EventType._

    val ENTITY42 = "42" // Make the entity for updated entries unique.

    var resp = one(service.get(makeAL(0, 0, None, USER_ANY, ENTITY_ANY)))
    resp.getAlarmsCount should equal(9)
    var lastUid = resp.getAlarmList.head.getUuid // The latest UID in the database

    val events = List[EventStore](
      // EventStore: EventType, alarm, time, deviceTime, severity, subsystem, userId, entityUid, args

      // Overlap the first event with the same time as the last event to make sure the don't get overlaps
      // and we don't miss one.
      //
      EventStore(System.UserLogin, false, NOW, 0, INFORM, SUB1, USER1, ENTITY42.toLong, Array[Byte]()),
      EventStore(Scada.ControlExe, false, NOW + 1, 0, CRITICAL, SUB1, USER1, ENTITY42.toLong, Array[Byte]()),

      EventStore(System.UserLogin, false, NOW + 2, 0, INFORM, SUB1, USER2, ENTITY42.toLong, Array[Byte]()),
      EventStore(Scada.ControlExe, false, NOW + 3, 0, CRITICAL, SUB1, USER2, ENTITY42.toLong, Array[Byte]()),
      EventStore(System.UserLogout, false, NOW + 4, 0, INFORM, SUB1, USER2, ENTITY42.toLong, Array[Byte]()),

      EventStore(System.UserLogin, false, NOW + 5, 0, INFORM, SUB1, USER3, ENTITY42.toLong, Array[Byte]()),
      EventStore(Scada.ControlExe, false, NOW + 6, 0, CRITICAL, SUB1, USER3, ENTITY42.toLong, Array[Byte]()),
      EventStore(System.UserLogout, false, NOW + 7, 0, INFORM, SUB1, USER3, ENTITY42.toLong, Array[Byte]()),

      EventStore(System.UserLogout, false, NOW + 8, 0, INFORM, SUB1, USER1, ENTITY42.toLong, Array[Byte]()))

    transaction {
      events.foreach(ApplicationSchema.events.insert(_))
    }

    var resp2 = one(service.get(makeAL_UidAfter(STATE_ANY, lastUid, USER_ANY)))
    resp2.getAlarmsCount should equal(9)
    resp2.getAlarmList.toIterable.foreach(e => {
      e.getTime should be >= (NOW)
      e.getEntity.getUuid should equal(ENTITY42)
    })

    resp2 = one(service.get(makeAL_UidAfter(STATE_ANY, lastUid, USER1)))
    resp2.getAlarmsCount should equal(3)
    resp2.getAlarmList.toIterable.foreach(e => {
      e.getTime should be >= (NOW)
      e.getEntity.getUuid should equal(ENTITY42)
      e.getUserId should equal(USER1)
    })

  }
*/
  ////////////////////////////////////////////////////////
  // Utilities

  /**
   * Make an Event
   */
  def makeEvent(event: EventType, time: Long, userId: String, entityId: Option[String]) = {
    val alist = new AttributeList
    alist += ("attr0" -> AttributeString("val0"))
    alist += ("attr1" -> AttributeString("val1"))

    val b = Event.newBuilder
      .setTime(time)
      .setDeviceTime(0)
      .setEventType(event)
      .setSubsystem("FEP")
      .setUserId(userId)
      .setArgs(alist.toProto)
    entityId.foreach(x => b.setEntity(EntityProto.newBuilder.setUuid(ReefUUID.newBuilder.setUuid(x)).build))
    b.build
  }

  /**
   * Make an AlarmList proto for selecting events via single parameters
   */
  def makeAL(states: List[Alarm.State], timeFrom: Long, timeTo: Long, eventType: Option[EventType], userId: String, entityName: String) = {

    val es = EventSelect.newBuilder
    if (timeFrom > 0)
      es.setTimeFrom(timeFrom)
    if (timeTo > 0)
      es.setTimeTo(timeTo)
    eventType.foreach(es.addEventType(_))
    if (userId != "") es.addUserId(userId)
    if (entityName != ENTITY_ANY) es.addEntity(EntityProto.newBuilder.setName(entityName).build)

    val as = AlarmSelect.newBuilder
    states.foreach(as.addState)
    as.setEventSelect(es)

    AlarmList.newBuilder
      .setSelect(as)
      .build
  }

  /**
   * Make an AlarmList proto for selecting events via parameter lists
   */
  def makeAL(states: List[Alarm.State], timeFrom: Long, timeTo: Long, eventType: List[EventType], userId: List[String], entityNames: List[String]) = {

    val es = EventSelect.newBuilder
    if (timeFrom > 0)
      es.setTimeFrom(timeFrom)
    if (timeTo > 0)
      es.setTimeTo(timeTo)
    eventType.foreach(x => es.addEventType(x.toString))
    userId.foreach(es.addUserId)
    entityNames.foreach(x => es.addEntity(EntityProto.newBuilder.setName(x).build))

    val as = AlarmSelect.newBuilder
    states.foreach(as.addState)
    as.setEventSelect(es)

    AlarmList.newBuilder
      .setSelect(as)
      .build
  }

  /**
   * Make an AlarmList proto for selecting events after the specified UID.
   */
  /*def makeAL_UidAfter(states: List[Alarm.State], uid: String, userId: String) = {

    val es = EventSelect.newBuilder
    es.setUuidAfter(uid)
    if (userId != "") es.addUserId(userId)

    val as = AlarmSelect.newBuilder
    states.foreach(as.addState)
    as.setEventSelect(es)

    AlarmList.newBuilder
      .setSelect(as)
      .build
  } */
}
