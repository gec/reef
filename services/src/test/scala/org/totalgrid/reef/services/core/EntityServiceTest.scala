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

import java.util.UUID

import SyncServiceShims._
import org.totalgrid.reef.client.exception.ReefServiceException
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ Relationship, Entity, ReefUUID, EntityEdge }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.proto.Envelope.{ SubscriptionEventType, Status }
import org.totalgrid.reef.services.SilentRequestContext
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.models.{ EventConfigStore, ApplicationSchema, DatabaseUsingTestBase }
import org.totalgrid.reef.client.service.proto.Alarms.Alarm
import org.totalgrid.reef.client.service.proto.Events.Event

@RunWith(classOf[JUnitRunner])
class EntityServiceTest extends DatabaseUsingTestBase {

  import SubscriptionTools._

  val contextSource = new MockContextSource

  val service = new SyncService(new EntityService(new EntityServiceModel), contextSource)
  val edgeModel = new EntityEdgeServiceModel
  val silentContext = new SilentRequestContext

  override def beforeEachInTransaction() {
    contextSource.reset()
  }

  def events = contextSource.sink.events

  import SubscriptionEventType._

  test("Put with bad names") {
    def makeEnt(name: String) = Entity.newBuilder().setName(name).build()

    intercept[ReefServiceException] {
      service.put(makeEnt("Bad Name")).expectOne
    }
    intercept[ReefServiceException] {
      service.put(makeEnt("*")).expectOne
    }
    intercept[ReefServiceException] {
      service.put(makeEnt("Bad*Name")).expectOne
    }
  }

  test("Put Entity with predetermined UUID") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build

    val created = service.put(upload).expectOne

    val events = contextSource.sink.events
    events.size should equal(1)
    events.head.typ should equal(ADDED)
    events.head.value should equal(created)

    created.getUuid.getValue.toString should equal(uuid)
  }

  test("Put two entities with same uuids") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build
    val upload2 = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject2").addTypes("TestType").build

    service.put(upload).expectOne(Status.CREATED)

    events.size should equal(1)
    events.head.typ should equal(ADDED)

    intercept[ReefServiceException] {
      service.put(upload2).expectOne
    }

    events.size should equal(1)
  }

  test("Put Entity with 2 types") {

    val upload1 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType1").addTypes("TestType3").build
    val upload2 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType4").addTypes("TestType2").build

    service.put(upload1).expectOne(Status.CREATED)
    val updated = service.put(upload2).expectOne(Status.UPDATED)

    val types = List("TestType1", "TestType3", "TestType4", "TestType2")
    updated.getTypesList.toList.diff(types) should equal(Nil)

    service.put(upload1).expectOne(Status.NOT_MODIFIED)
    service.put(upload2).expectOne(Status.NOT_MODIFIED)

    events.map(_.typ) should equal(List(ADDED, MODIFIED))
  }

  test("Get multi level") {

    val regId = EntityTestSeed.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder
      .setName("Reg")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .setDistance(1))

    val root = service.get(req.build).expectOne(Status.OK)
    root.getName should equal("Reg")

    root.getRelationsCount should equal(1)
    root.getRelations(0).getEntitiesCount should equal(1)
    root.getRelations(0).getEntities(0).getName should equal("Sub")
    root.getRelations(0).getEntities(0).getRelationsCount should equal(0)
  }

  test("Get single level") {

    val regId = EntityTestSeed.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder
      .addTypes("Region")

    val root = service.get(req.build).expectOne(Status.OK)

    root.getName should equal("Reg")
    root.getRelationsCount should equal(0)
  }

  test("Get sorted") {

    val regId = EntityTestSeed.addEntity("B", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("a", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("c", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder.setUuid(ReefUUID.newBuilder().setValue("*"))

    val list = service.get(req.build).expectMany(Status.OK)

    list.get(0).getName() should equal("a")
    list.get(1).getName() should equal("B")
    list.get(2).getName() should equal("c")
  }

  test("Get result limit: all") {

    val regId = EntityTestSeed.addEntity("a", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("b", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("c", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder.setUuid(ReefUUID.newBuilder().setValue("*"))

    val env = BasicRequestHeaders.empty
    val list = service.get(req.build, env.setResultLimit(1)).expectMany(1)

    list.size() should equal(1)
    list.get(0).getName() should equal("a")
  }

  test("Get result limit: types") {

    val regId = EntityTestSeed.addEntity("a", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("b", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("c", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder.addTypes("EquipmentGroup")

    val env = BasicRequestHeaders.empty
    val list = service.get(req.build, env.setResultLimit(1)).expectMany(Status.OK) //.expectMany(1)

    list.size() should equal(1)
  }

  test("Get result limit: tree") {

    val regId = EntityTestSeed.addEntity("a", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("b", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("c", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val req = Entity.newBuilder
      .addTypes("EquipmentGroup")
      .addRelations(
        Relationship.newBuilder
          .setDistance(1))

    val env = BasicRequestHeaders.empty
    val list = service.get(req.build, env.setResultLimit(1)).expectMany(1)

    list.size() should equal(1)
  }

  test("Delete Entity") {

    val upload = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType").build
    val created = service.put(upload).expectOne(Status.CREATED)

    val deleteEnt = Entity.newBuilder().setUuid(created.getUuid).build()
    val deleted = service.delete(deleteEnt).expectOne(Status.DELETED)

    events.map(_.typ) should equal(List(ADDED, REMOVED))
  }

  import org.squeryl.PrimitiveTypeMode._

  test("Complicated Delete") {

    val regId = EntityTestSeed.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityTestSeed.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, regId, subId, "owns")
    val devId = EntityTestSeed.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    edgeModel.addEdge(silentContext, subId, devId, "owns")

    val edges = ApplicationSchema.edges
    val deriveds = ApplicationSchema.derivedEdges

    val preEdges = edges.where(e => true === true).toList
    val preDeriveds = deriveds.where(e => true === true).toList

    preEdges.size should equal(3)
    preDeriveds.size should equal(1)

    val deleteEnt = Entity.newBuilder().setName("Sub").build()

    val deleted = service.delete(deleteEnt).expectOne(Status.DELETED)

    val postEdges = edges.where(e => true === true).toList
    postEdges should equal(Nil)

    val postDeriveds = deriveds.where(e => true === true).toList
    postDeriveds should equal(Nil)

    val eventList = List(
      (REMOVED, classOf[Entity]),
      (REMOVED, classOf[EntityEdge]),
      (REMOVED, classOf[EntityEdge]),
      (REMOVED, classOf[EntityEdge]))

    events.map(s => (s.typ, s.value.getClass)) should equal(eventList)
  }

  test("Multi-model Delete") {
    val dev = EntityTestSeed.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    val sub = EntityTestSeed.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    edgeModel.addEdge(silentContext, dev, sub, "owns")

    val eventConfigModel = new EventConfigServiceModel
    val alarmModel = new AlarmServiceModel
    val eventModel = new EventServiceModel(eventConfigModel, alarmModel)
    alarmModel.eventModel = Some(eventModel)

    val req = Event.newBuilder.build

    eventModel.makeEvent(silentContext, EventConfigStore.ALARM, req, 8, Some(sub), "rendered", "user", Alarm.State.UNACK_SILENT.getNumber)
    ApplicationSchema.events.where(t => true === true).toList.size should equal(1)
    ApplicationSchema.alarms.where(t => true === true).toList.size should equal(1)

    val deleteEnt = Entity.newBuilder().setName("Sub").build()
    val deleted = service.delete(deleteEnt).expectOne(Status.DELETED)

    ApplicationSchema.events.where(t => true === true).toList.size should equal(0)
    ApplicationSchema.alarms.where(t => true === true).toList.size should equal(0)

    // multiple events due to multi-level subscriptions
    val eventList = List(
      (REMOVED, classOf[Alarm]),
      (REMOVED, classOf[Alarm]),
      (REMOVED, classOf[Event]),
      (REMOVED, classOf[Event]),
      (REMOVED, classOf[Entity]),
      (REMOVED, classOf[EntityEdge]))

    events.map(s => (s.typ, s.value.getClass)) should equal(eventList)
  }

}