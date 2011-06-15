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

import org.totalgrid.reef.executor.mock.InstantExecutor
import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, Relationship => RelationshipProto }
import org.totalgrid.reef.proto.Events.{ Event => EventProto, EventList => EventListProto }
import org.squeryl.PrimitiveTypeMode.transaction
import org.totalgrid.reef.messaging.{ AMQPProtoFactory }
import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.models.{ DatabaseUsingTestBase, Entity }

import scala.collection.JavaConversions._
import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.proto.Alarms.{ Alarm => AlarmProto, EventConfig => EventConfigProto, AlarmList => AlarmListProto }

class EventIntegrationTestsBase extends DatabaseUsingTestBase {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  class AlarmTestFixture(amqp: AMQPProtoFactory) {
    val publishers = new ServiceEventPublisherRegistry(amqp, ReefServicesList)
    val summaries = new SilentSummaryPoints
    val factories = new ModelFactories(ServiceDependencies(publishers, summaries))

    val alarms = new AlarmService(factories.alarms)
    val events = new EventService(factories.events)
    val eventConfigs = new EventConfigService(factories.eventConfig)

    val eventQuery = new EventQueryService(factories.events, publishers)
    val alarmQuery = new AlarmQueryService(publishers)

    seed()

    val alarmInitializer = new AlarmSummaryInitializer(factories.alarms, summaries)
    alarmInitializer.addAMQPConsumers(amqp, new InstantExecutor {})

    def seed() {
      seed("SubA")
      seed("SubB")

      EQ.addEntity("Orphan", "Orphan")
    }
    def seed(name: String) {
      val subId = EQ.addEntity(name, "Substation", "EquipmentGroup")
      seedDevice(subId, name + "-DeviceA", "Line")
      seedDevice(subId, name + "-DeviceB", "Line")
    }
    def seedDevice(subId: Entity, name: String, typ: String) {
      val devId = EQ.addEntity(name, typ, "Equipment")
      val toSubId = EQ.addEdge(subId, devId, "owns")
      seedPoint(subId, devId, name + "-PointA", "owns")
      seedPoint(subId, devId, name + "-PointB", "owns")
    }
    def seedPoint(subId: Entity, devId: Entity, name: String, rel: String) {
      val pointId = EQ.addEntity(name, "Point")
      val toDevId = EQ.addEdge(devId, pointId, rel)
    }

    def subscribeEvents(expected: Int, req: EventProto) = {
      val (updates, env) = getEventQueue[EventProto](amqp, EventProto.parseFrom)
      val result = events.get(req, env).expectMany(expected)
      updates.size should equal(0)
      (result, updates)
    }

    def subscribeAlarms(expected: Int, req: AlarmProto) = {
      val (updates, env) = getEventQueue[AlarmProto](amqp, AlarmProto.parseFrom)
      val result = alarms.get(req, env).expectMany(expected)
      (result, updates)
    }

    def subscribeEvents(expected: Int, req: EventListProto) = {
      val (updates, env) = getEventQueue[EventProto](amqp, EventProto.parseFrom)
      val result = eventQuery.get(req, env).expectOne()
      updates.size should equal(0)
      result.getEventsCount should equal(expected)
      (result.getEventsList.toList, updates)
    }

    def subscribeAlarms(expected: Int, req: AlarmListProto) = {
      val (updates, env) = getEventQueue[AlarmProto](amqp, AlarmProto.parseFrom)
      val result = alarmQuery.get(req, env).expectOne()
      result.getAlarmsCount should equal(expected)
      (result.getAlarmsList.toList, updates)
    }

    def checkSummaries(expectedValues: Map[String, Int]) = {
      // check that the "live" counted summaries match our expectations
      summaries.getMap should equal(expectedValues)

      // check that a restart at this point would come up with the same numbers
      val integrityPoll = new SilentSummaryPoints
      AlarmSummaryCalculations.initializeSummaries(integrityPoll)
      integrityPoll.getMap should equal(expectedValues)

      expectedValues
    }

    def checkAllSummariesZero(size: Int) {
      val m = summaries.getMap
      m.size should equal(size)
      m.values.forall { _ == 0 }
      val integrityPoll = new SilentSummaryPoints
      AlarmSummaryCalculations.initializeSummaries(integrityPoll)
      val m2 = integrityPoll.getMap
      m2.size should equal(size)
      m2.values.forall { _ == 0 }
    }
  }

  def makeEvent(event: String, entityName: String, subsystem: String = "FEP") =
    EventProto.newBuilder
      .setTime(0)
      .setDeviceTime(0)
      .setEventType(event)
      .setSubsystem(subsystem)
      .setUserId("flint")
      .setEntity(EntityProto.newBuilder.setName(entityName).build)
      .build

  def makeEC(event: String, severity: Int, designation: EventConfigProto.Designation) =
    EventConfigProto.newBuilder
      .setEventType(event)
      .setSeverity(severity)
      .setDesignation(designation)
      .setResource("")
      .setAlarmState(AlarmProto.State.UNACK_AUDIBLE)
      .build

  def makeEventByEntityName(name: String) = {
    val ent = EntityProto.newBuilder.setName(name)
    ent.addRelations(
      RelationshipProto.newBuilder
        .setRelationship("owns")
        .setDescendantOf(true))
    EventProto.newBuilder.setEntity(ent).build
  }

  def makeEventByType(name: String) = {
    val ent = EntityProto.newBuilder.addTypes(name)
    ent.addRelations(
      RelationshipProto.newBuilder
        .setRelationship("owns")
        .setDescendantOf(true))
    EventProto.newBuilder.setEntity(ent).build
  }
  def makeAllEvent() = {
    EventProto.newBuilder.setEventType("*").build
  }
}

@RunWith(classOf[JUnitRunner])
class EventIntegrationTests extends EventIntegrationTestsBase {

  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  test("Event Subscribe at multiple levels") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Event", 1, EventConfigProto.Designation.EVENT))

      fix.events.put(makeEvent("Test.Event", "SubA-DeviceA-PointA"))
      fix.events.put(makeEvent("Test.Event", "SubA-DeviceA-PointB"))
      fix.events.put(makeEvent("Test.Event", "SubA-DeviceB-PointB"))
      fix.events.put(makeEvent("Test.Event", "SubB-DeviceB-PointB"))
      fix.events.put(makeEvent("Test.Event", "Orphan"))

      val (starting2, pointA) = fix.subscribeEvents(1, makeEventByEntityName("SubA-DeviceA-PointA"))
      val (starting3, deviceA) = fix.subscribeEvents(2, makeEventByEntityName("SubA-DeviceA"))
      val (starting4, subA) = fix.subscribeEvents(3, makeEventByEntityName("SubA"))
      val (starting5, subAll) = fix.subscribeEvents(4, makeEventByType("Substation"))
      // order is important, the all channel will be last to get publication messages so we only have to wait for 
      // that message to arrive here to know all pending messages will have already been delivered to the other subscribers 
      val (starting1, allEvents) = fix.subscribeEvents(5, makeAllEvent)

      def allEmpty() { // check that we didnt get any unexpected messages we didnt pop
        pointA.size should equal(0); deviceA.size should equal(0)
        subA.size should equal(0); subAll.size should equal(0)
        allEvents.size should equal(0)
      }

      // TODO - Make 5000 a default somewhere and then hide it with an overload - JAC

      // should get event1 on all 4 levels of subscriber
      val event1 = fix.events.put(makeEvent("Test.Event", "SubA-DeviceA-PointA")).expectOne()
      allEvents.pop(5000) should equal(event1)
      subAll.pop(5000) should equal(event1)
      subA.pop(5000) should equal(event1)
      deviceA.pop(5000) should equal(event1)
      pointA.pop(5000) should equal(event1)
      allEmpty()

      // should get event2 for device and substation subscriptions
      val event2 = fix.events.put(makeEvent("Test.Event", "SubA-DeviceA-PointB")).expectOne()
      allEvents.pop(5000) should equal(event2)
      subAll.pop(5000) should equal(event2)
      subA.pop(5000) should equal(event2)
      deviceA.pop(5000) should equal(event2)
      allEmpty()

      // should get event3 for substation only
      val event3 = fix.events.put(makeEvent("Test.Event", "SubA-DeviceB-PointB")).expectOne()
      allEvents.pop(5000) should equal(event3)
      subAll.pop(5000) should equal(event3)
      subA.pop(5000) should equal(event3)
      allEmpty()

      // should get event4 on the substation subscriptions
      val event4 = fix.events.put(makeEvent("Test.Event", "SubB-DeviceB-PointB")).expectOne()
      allEvents.pop(5000) should equal(event4)
      subAll.pop(5000) should equal(event4)
      allEmpty()

      // since its not in our "device tree" we wont see any alarms not associated with our devices
      val event5 = fix.events.put(makeEvent("Test.Event", "Orphan")).expectOne()
      allEvents.pop(5000) should equal(event5)
      allEmpty()
    }
  }

  test("Alarm Subscribe at mutlipe levels") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Alarm", 1, EventConfigProto.Designation.ALARM))
      fix.eventConfigs.put(makeEC("Test.Event", 1, EventConfigProto.Designation.EVENT))

      val (_, allAlarms) = fix.subscribeAlarms(0, AlarmProto.newBuilder.setEvent(makeAllEvent).build)
      val (_, deviceAlarms) = fix.subscribeAlarms(0, AlarmProto.newBuilder.setEvent(makeEventByEntityName("SubA-DeviceA")).build)
      val (_, allEvents) = fix.subscribeEvents(0, makeAllEvent)

      def allEmpty() {
        allAlarms.size should equal(0); deviceAlarms.size should equal(0)
        allEvents.size should equal(0)
      }

      val event1 = fix.events.put(makeEvent("Test.Alarm", "SubA-DeviceA-PointA")).expectOne()
      allEvents.pop(5000) should equal(event1)
      allAlarms.pop(5000).getEvent should equal(event1)
      deviceAlarms.pop(5000).getEvent should equal(event1)
      allEmpty()

      val event2 = fix.events.put(makeEvent("Test.Event", "SubA-DeviceA-PointA")).expectOne()
      allEvents.pop(5000) should equal(event2)
      allEmpty()

      val event3 = fix.events.put(makeEvent("Test.Alarm", "SubA")).expectOne()
      allEvents.pop(5000) should equal(event3)
      allAlarms.pop(5000).getEvent should equal(event3)
      allEmpty()
    }
  }

  test("Alarm Lifecycle updates cause events") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Alarm", 1, EventConfigProto.Designation.ALARM))
      val allAlarmRequest = AlarmProto.newBuilder.setEvent(makeAllEvent).build
      val (_, allAlarms) = fix.subscribeAlarms(0, allAlarmRequest)

      def allEmpty() = allAlarms.size should equal(0)

      val rootEvent = fix.events.put(makeEvent("Test.Alarm", "SubA-DeviceA-PointA")).expectOne()
      val freshAlarm = allAlarms.pop(5000)
      freshAlarm.getEvent should equal(rootEvent)
      freshAlarm.getState should equal(AlarmProto.State.UNACK_AUDIBLE)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val silenced = fix.alarms.put(freshAlarm.toBuilder.setState(AlarmProto.State.UNACK_SILENT).build).expectOne()
      silenced.getState should equal(AlarmProto.State.UNACK_SILENT)
      allAlarms.pop(5000) should equal(silenced)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val acked = fix.alarms.put(silenced.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()
      acked.getState should equal(AlarmProto.State.ACKNOWLEDGED)
      allAlarms.pop(5000) should equal(acked)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val removed = fix.alarms.put(acked.toBuilder.setState(AlarmProto.State.REMOVED).build).expectOne()
      removed.getState should equal(AlarmProto.State.REMOVED)
      allAlarms.pop(5000) should equal(removed)
      allEmpty()

      // we dont return REMOVED alarms by default
      fix.alarms.get(allAlarmRequest).expectNone()

      // but we can get them if asked for explictly
      fix.alarms.get(AlarmProto.newBuilder.setState(AlarmProto.State.REMOVED).setEvent(makeAllEvent).build).expectOne()
    }
  }

  test("Alarm Summaries are accurate") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Alarm1", 1, EventConfigProto.Designation.ALARM))
      fix.eventConfigs.put(makeEC("Test.Alarm2", 2, EventConfigProto.Designation.ALARM))
      fix.eventConfigs.put(makeEC("Test.Alarm3", 3, EventConfigProto.Designation.ALARM))

      //fix.checkAllSummariesZero(7)

      val event1 = fix.events.put(makeEvent("Test.Alarm1", "SubA-DeviceA-PointA", "FEP")).expectOne()
      val event2 = fix.events.put(makeEvent("Test.Alarm2", "SubA-DeviceA-PointA", "FEP")).expectOne()
      val event3 = fix.events.put(makeEvent("Test.Alarm2", "SubA-DeviceA-PointA", "Processing")).expectOne()
      val event4 = fix.events.put(makeEvent("Test.Alarm3", "SubB-DeviceA-PointA", "FEP")).expectOne()

      val expectedValues = fix.checkSummaries(Map(
        "summary.unacked_alarms_severity_1" -> 1,
        "summary.unacked_alarms_severity_2" -> 2,
        "summary.unacked_alarms_severity_3" -> 1,
        "summary.unacked_alarms_subsystem_FEP" -> 3,
        "summary.unacked_alarms_subsystem_Processing" -> 1,
        "summary.unacked_alarms_equipment_group_SubA" -> 3,
        "summary.unacked_alarms_equipment_group_SubB" -> 1))

      // grab alarms so we can acknowledge them
      val alarm1 = fix.alarms.get(AlarmProto.newBuilder.setEvent(event1).build).expectOne()
      val alarm2 = fix.alarms.get(AlarmProto.newBuilder.setEvent(event2).build).expectOne()
      val alarm3 = fix.alarms.get(AlarmProto.newBuilder.setEvent(event3).build).expectOne()
      val alarm4 = fix.alarms.get(AlarmProto.newBuilder.setEvent(event4).build).expectOne()
      alarm1.getState should equal(AlarmProto.State.UNACK_AUDIBLE)
      alarm3.getState should equal(AlarmProto.State.UNACK_AUDIBLE)

      // mark an alarm as silent (this is not the same as acknlowdeged)
      fix.alarms.put(alarm1.toBuilder.setState(AlarmProto.State.UNACK_SILENT).build).expectOne()
      // since the unacked state didnt change neigther should the counts
      fix.checkSummaries(expectedValues)

      // acknowledge 2 of the alarms
      fix.alarms.put(alarm1.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()
      fix.alarms.put(alarm3.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()

      fix.checkSummaries(Map(
        "summary.unacked_alarms_severity_1" -> 0,
        "summary.unacked_alarms_severity_2" -> 1,
        "summary.unacked_alarms_severity_3" -> 1,
        "summary.unacked_alarms_subsystem_FEP" -> 2,
        "summary.unacked_alarms_subsystem_Processing" -> 0,
        "summary.unacked_alarms_equipment_group_SubA" -> 1,
        "summary.unacked_alarms_equipment_group_SubB" -> 1))

      // acknowledge other alarms
      fix.alarms.put(alarm1.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()
      fix.alarms.put(alarm3.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()

      fix.checkAllSummariesZero(7)
    }
  }

}
