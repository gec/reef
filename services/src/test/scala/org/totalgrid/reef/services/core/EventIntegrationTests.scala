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
import org.totalgrid.reef.messaging.{ AMQPProtoFactory }
import org.totalgrid.reef.messaging.serviceprovider.ServiceEventPublisherRegistry
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.models.{ DatabaseUsingTestBase, Entity }

import scala.collection.JavaConversions._
import org.totalgrid.reef.services.ServiceDependencies
import org.totalgrid.reef.proto.Alarms.{ Alarm => AlarmProto, EventConfig => EventConfigProto, AlarmList => AlarmListProto }

import org.totalgrid.reef.proto.Utils.{ AttributeList, Attribute }

import org.totalgrid.reef.sapi.BasicRequestHeaders

class EventIntegrationTestsBase extends DatabaseUsingTestBase {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  class AlarmTestFixture(amqp: AMQPProtoFactory) {
    val publishers = new ServiceEventPublisherRegistry(amqp, ReefServicesList)
    val summaries = new SilentSummaryPoints
    val deps = ServiceDependencies(publishers, summaries)
    val env = BasicRequestHeaders.empty.setUserName("user")
    val contextSource = new MockRequestContextSource(deps, env)

    val factories = new ModelFactories(deps, contextSource)

    val alarms = new SyncService(new AlarmService(factories.alarms), contextSource)
    val events = new SyncService(new EventService(factories.events), contextSource)
    val eventConfigs = new SyncService(new EventConfigService(factories.eventConfig), contextSource)

    val eventQuery = new SyncService(new EventQueryService, contextSource)
    val alarmQuery = new SyncService(new AlarmQueryService, contextSource)

    def publishEvent(evt: EventProto): EventProto = {
      events.put(evt).expectOne()
    }

    seed()

    val alarmInitializer = new AlarmSummaryInitializer(factories.alarms, summaries)
    alarmInitializer.addAMQPConsumers(amqp, new InstantExecutor {})

    def seed() {
      seed("SubA")
      seed("SubB")

      EntityQueryManager.addEntity("Orphan", "Orphan")
    }
    def seed(name: String) {
      val subId = EntityQueryManager.addEntity(name, "Substation" :: "EquipmentGroup" :: Nil)
      seedDevice(subId, name + "-DeviceA", "Line")
      seedDevice(subId, name + "-DeviceB", "Line")
    }
    def seedDevice(subId: Entity, name: String, typ: String) {
      val devId = EntityQueryManager.addEntity(name, typ :: "Equipment" :: Nil)
      val toSubId = EntityQueryManager.addEdge(subId, devId, "owns")
      seedPoint(subId, devId, name + "-PointA", "owns")
      seedPoint(subId, devId, name + "-PointB", "owns")
    }
    def seedPoint(subId: Entity, devId: Entity, name: String, rel: String) {
      val pointId = EntityQueryManager.addEntity(name, "Point")
      val toDevId = EntityQueryManager.addEdge(devId, pointId, rel)
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

  def makeEvent(eventType: String,
    subsystem: String = "FEP",
    dt: Option[Long] = None,
    entityName: Option[String] = None,
    args: Option[List[Attribute]] = None) = {

    val b = EventProto.newBuilder.setEventType(eventType).setSubsystem(subsystem)
    dt.foreach(b.setDeviceTime(_))
    entityName.foreach(u => b.setEntity(EntityProto.newBuilder.setName(u)))

    args.foreach { argList =>
      val aList = AttributeList.newBuilder
      argList.toList.foreach { aList.addAttribute(_) }
      b.setArgs(aList)
    }
    b.build
  }

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

  val waitTime = 5000

  test("Event Subscribe at multiple levels") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Event", 1, EventConfigProto.Designation.EVENT))

      fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceA-PointA")))
      fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceA-PointB")))
      fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceB-PointB")))
      fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubB-DeviceB-PointB")))
      fix.publishEvent(makeEvent("Test.Event", entityName = Some("Orphan")))

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

      // should get event1 on all 4 levels of subscriber
      val event1 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceA-PointA")))
      allEvents.pop(waitTime) should equal(event1)
      subAll.pop(waitTime) should equal(event1)
      subA.pop(waitTime) should equal(event1)
      deviceA.pop(waitTime) should equal(event1)
      pointA.pop(waitTime) should equal(event1)
      allEmpty()

      // should get event2 for device and substation subscriptions
      val event2 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceA-PointB")))
      allEvents.pop(waitTime) should equal(event2)
      subAll.pop(waitTime) should equal(event2)
      subA.pop(waitTime) should equal(event2)
      deviceA.pop(waitTime) should equal(event2)
      allEmpty()

      // should get event3 for substation only
      val event3 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceB-PointB")))
      allEvents.pop(waitTime) should equal(event3)
      subAll.pop(waitTime) should equal(event3)
      subA.pop(waitTime) should equal(event3)
      allEmpty()

      // should get event4 on the substation subscriptions
      val event4 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubB-DeviceB-PointB")))
      allEvents.pop(waitTime) should equal(event4)
      subAll.pop(waitTime) should equal(event4)
      allEmpty()

      // since its not in our "device tree" we wont see any alarms not associated with our devices
      val event5 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("Orphan")))
      allEvents.pop(waitTime) should equal(event5)
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

      val event1 = fix.publishEvent(makeEvent("Test.Alarm", entityName = Some("SubA-DeviceA-PointA")))
      allEvents.pop(waitTime) should equal(event1)
      allAlarms.pop(waitTime).getEvent should equal(event1)
      deviceAlarms.pop(waitTime).getEvent should equal(event1)
      allEmpty()

      val event2 = fix.publishEvent(makeEvent("Test.Event", entityName = Some("SubA-DeviceA-PointA")))
      allEvents.pop(waitTime) should equal(event2)
      allEmpty()

      val event3 = fix.publishEvent(makeEvent("Test.Alarm", entityName = Some("SubA")))
      allEvents.pop(waitTime) should equal(event3)
      allAlarms.pop(waitTime).getEvent should equal(event3)
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

      val rootEvent = fix.publishEvent(makeEvent("Test.Alarm", entityName = Some("SubA-DeviceA-PointA")))
      val freshAlarm = allAlarms.pop(waitTime)
      freshAlarm.getEvent should equal(rootEvent)
      freshAlarm.getState should equal(AlarmProto.State.UNACK_AUDIBLE)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val silenced = fix.alarms.put(freshAlarm.toBuilder.setState(AlarmProto.State.UNACK_SILENT).build).expectOne()
      silenced.getState should equal(AlarmProto.State.UNACK_SILENT)
      allAlarms.pop(waitTime) should equal(silenced)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val acked = fix.alarms.put(silenced.toBuilder.setState(AlarmProto.State.ACKNOWLEDGED).build).expectOne()
      acked.getState should equal(AlarmProto.State.ACKNOWLEDGED)
      allAlarms.pop(waitTime) should equal(acked)
      allEmpty()

      fix.alarms.get(allAlarmRequest).expectOne()

      val removed = fix.alarms.put(acked.toBuilder.setState(AlarmProto.State.REMOVED).build).expectOne()
      removed.getState should equal(AlarmProto.State.REMOVED)
      allAlarms.pop(waitTime) should equal(removed)
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

      val event1 = fix.publishEvent(makeEvent("Test.Alarm1", "FEP", entityName = Some("SubA-DeviceA-PointA")))
      val event2 = fix.publishEvent(makeEvent("Test.Alarm2", "FEP", entityName = Some("SubA-DeviceA-PointA")))
      val event3 = fix.publishEvent(makeEvent("Test.Alarm2", "Processing", entityName = Some("SubA-DeviceA-PointA")))
      val event4 = fix.publishEvent(makeEvent("Test.Alarm3", "FEP", entityName = Some("SubB-DeviceA-PointA")))

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
