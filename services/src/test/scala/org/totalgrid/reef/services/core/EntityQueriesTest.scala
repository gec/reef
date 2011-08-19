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

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services._
import org.totalgrid.reef.services.coordinators._

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import org.totalgrid.reef.broker.ChannelSender
import scala.collection.JavaConversions._
import org.totalgrid.reef.japi.BadRequestException
import org.totalgrid.reef.models.{ DatabaseUsingTestBase, RunTestsInsideTransaction, ApplicationSchema, Entity, EntityEdge => Edge, EntityDerivedEdge => Derived }
import org.totalgrid.reef.proto.Model.{ ReefUUID, Entity => EntityProto, Relationship }
import java.util.UUID

@RunWith(classOf[JUnitRunner])
class EntityQueriesTest extends DatabaseUsingTestBase with RunTestsInsideTransaction {
  import EntityQueryManager._

  override def beforeEachInTransaction() {
    seed
  }

  def seed {
    val regId = EntityQueryManager.addEntity("RegA", "Region", "EquipmentGroup")
    seedSub(regId, "RegA-SubA")
    seedSub(regId, "RegA-SubB")
  }
  def seedSub(regId: Entity, name: String) {
    val subId = EntityQueryManager.addEntity(name, "Substation", "EquipmentGroup")
    EntityQueryManager.addEdge(regId, subId, "owns")
    seedDevice(regId, subId, name + "-DeviceA", "Line")
    seedDevice(regId, subId, name + "-DeviceB", "Line")
    seedDevice(regId, subId, name + "-DeviceC", "Breaker")
  }
  def seedDevice(regId: Entity, subId: Entity, name: String, typ: String) {
    val devId = EntityQueryManager.addEntity(name, typ, "Equipment")
    val toSubId = EntityQueryManager.addEdge(subId, devId, "owns")
    seedPoint(regId, subId, devId, name + "-PointA", "owns")
    seedPoint(regId, subId, devId, name + "-PointB", "owns")
    seedPoint(regId, subId, devId, name + "-PointC", "refs")
  }
  def seedPoint(regId: Entity, subId: Entity, devId: Entity, name: String, rel: String) {
    val pointId = EntityQueryManager.addEntity(name, "Point")
    val toDevId = EntityQueryManager.addEdge(devId, pointId, rel)
  }

  class EdgeString(me: String) {
    def owns(that: String) = EdgeResult(me, "owns", that)
    def refs(that: String) = EdgeResult(me, "refs", that)
  }
  implicit def strToEdge(str: String): EdgeString = new EdgeString(str)

  case class EdgeResult(parent: String, rel: String, child: String)

  def parseTree(proto: EntityProto): List[EdgeResult] = {
    proto.getRelationsList.flatMap { rel =>
      val isDescendant = rel.getDescendantOf
      val relName = rel.getRelationship

      rel.getEntitiesList.flatMap { ent =>
        (if (isDescendant) EdgeResult(proto.getName, relName, ent.getName)
        else EdgeResult(ent.getName, relName, proto.getName)) ::
          parseTree(ent)
      }
    }.toList
  }

  def checkResults[A](result: List[A], desc: List[A]) = {
    result.length should equal(desc.length)
    result filterNot desc.contains should equal(Nil)
    desc filterNot result.contains should equal(Nil)
  }

  def checkNames(req: EntityProto.Builder, desc: List[String]) = {
    val results = EntityQueryManager.fullQuery(req.build)
    val names = results.map(_.getName).toList

    checkResults(names, desc)
  }

  def parseResults(node: ResultNode): List[EdgeResult] = {
    node.subNodes.flatMap {
      case (rel, nodes) =>
        nodes.flatMap { sub =>
          val (parent, child) = if (rel.descendantOf) (node.name, sub.name) else (sub.name, node.name)
          EdgeResult(parent, rel.rel, child) :: parseResults(sub)
        }.toList
    }.toList
  }

  test("Proto to QueryNode (no ent)") {
    val req = EntityProto.newBuilder
      //.setUuid("1")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .setDistance(1))

    val nodes = protoToQuery(req.build)
    nodes.size should equal(1)

    val desc = QueryNode(Some("owns"), Some(true), Some(1), None, Nil, Nil)

    nodes.head should equal(desc)
  }

  test("Proto to QueryNode (depth)") {
    val req = EntityProto.newBuilder
      //.setUuid("1")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .setDistance(1)
          .addEntities(
            EntityProto.newBuilder
              .addTypes("Point")
              .addRelations(Relationship.newBuilder
                .setRelationship("owns")
                .setDescendantOf(true)
                .setDistance(1)
                .addEntities(
                  EntityProto.newBuilder
                    .addTypes("Command")))))

    val nodes = protoToQuery(req.build)
    nodes.size should equal(1)

    val desc = QueryNode(Some("owns"), Some(true), Some(1), None, List("Point"),
      List(QueryNode(Some("owns"), Some(true), Some(1), None, List("Command"), Nil)))

    nodes.head should equal(desc)
  }

  test("Proto to QueryNode (two entities)") {
    val req = EntityProto.newBuilder
      //.setUuid("1")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .setDistance(1)
          .addEntities(
            EntityProto.newBuilder
              .addTypes("Point"))
            .addEntities(
              EntityProto.newBuilder
                .setName("Junk01")
                .addTypes("Junk")))

    val nodes = protoToQuery(req.build)
    nodes.size should equal(2)

    nodes.contains {
      QueryNode(Some("owns"), Some(true), Some(1), None, List("Point"), Nil)
    }

    nodes.contains {
      QueryNode(Some("owns"), Some(true), Some(1), Some("Junk01"), List("Junk"), Nil)
    }
  }

  test("Proto to QueryNode (two rels)") {
    val req = EntityProto.newBuilder
      //.setUuid("1")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .addEntities(
            EntityProto.newBuilder
              .addTypes("Point")))
        .addRelations(
          Relationship.newBuilder
            .setRelationship("refs")
            .setDistance(4)
            .addEntities(
              EntityProto.newBuilder
                .setName("Thing1")
                .addTypes("Thing")))

    val nodes = protoToQuery(req.build)
    nodes.size should equal(2)

    nodes.contains {
      QueryNode(Some("owns"), Some(true), None, None, List("Point"), Nil)
    }

    nodes.contains {
      QueryNode(Some("refs"), None, Some(4), Some("Thing1"), List("Thing"), Nil)
    }
  }

  test("Proto query, abstract root set") {
    val req = EntityProto.newBuilder
      .addTypes("Breaker")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("refs")
          .setDescendantOf(true)
          .setDistance(1))

    val results = protoTreeQuery(req.build)
    results.size should equal(2)

    val tree =
      ("RegA-SubA-DeviceC" refs "RegA-SubA-DeviceC-PointC") ::
        ("RegA-SubB-DeviceC" refs "RegA-SubB-DeviceC-PointC") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Proto query, root set blank") {
    val req = EntityProto.newBuilder
      .addRelations(
        Relationship.newBuilder
          .setRelationship("refs")
          .setDescendantOf(true)
          .setDistance(1))

    intercept[Exception] {
      val results = protoTreeQuery(req.build)
    }
  }

  test("Proto query, root set nil") {
    val req = EntityProto.newBuilder
      .setName("Nonexistent")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("refs")
          .setDescendantOf(true)
          .setDistance(1))

    protoTreeQuery(req.build) should equal(Nil)
  }

  test("Proto query, root only") {
    val req = EntityProto.newBuilder
      .addTypes("Breaker")

    val results = protoTreeQuery(req.build)
    results.size should equal(2)
  }

  test("Results to proto") {
    def buildEnt(id: Long, name: String) = {
      val e = new Entity(name)
      e.id = uuid(id)
      e
    }
    def uuid(id: Long) = new UUID(id, id)

    def buildNode(ent: Entity, types: List[String], subs: Map[Relate, List[ResultNode]]): ResultNode = {
      val n = ResultNode(ent, subs)
      n.types = types
      n
    }

    def checkEnt(proto: EntityProto, uid: UUID, name: String, typ: String, relCount: Int) = {
      proto.getUuid.getUuid should equal(uid.toString)
      proto.getName should equal(name)
      proto.getTypesCount should equal(1)
      proto.getTypes(0) should equal(typ)
      proto.getRelationsCount should equal(relCount)
    }

    def checkRel(proto: Relationship, rel: String, desc: Boolean, dist: Int, entCount: Int) = {
      proto.getRelationship should equal(rel)
      proto.getDescendantOf should equal(desc)
      proto.getDistance should equal(dist)
      proto.getEntitiesCount should equal(entCount)
    }

    val node4 = buildNode(buildEnt(4, "ent04"), List("Relay"), Map())
    val node3 = buildNode(buildEnt(3, "ent03"), List("Breaker"), Map())
    val node2 = buildNode(buildEnt(2, "ent02"), List("Line"), Map())

    val subMap = Map((Relate("owns", true, 1) -> List(node2, node3)),
      (Relate("source", false, 1) -> List(node4)))

    val node1 = buildNode(buildEnt(1, "ent01"), List("Substation"), subMap)

    val proto = node1.toProto
    checkEnt(proto, uuid(1), "ent01", "Substation", 2)

    val rel1 = proto.getRelations(0)
    checkRel(rel1, "owns", true, 1, 2)

    val line = rel1.getEntities(0)
    checkEnt(line, uuid(2), "ent02", "Line", 0)

    val bkr = rel1.getEntities(1)
    checkEnt(bkr, uuid(3), "ent03", "Breaker", 0)

    val rel2 = proto.getRelations(1)
    checkRel(rel2, "source", false, 1, 1)

    val relay = rel2.getEntities(0)
    checkEnt(relay, uuid(4), "ent04", "Relay", 0)
  }

  test("Tree request downwards one level") {
    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA")

    val req = new QueryNode(Some("owns"), Some(true), None, None, List("Line"), Nil)
    val results = resultsForQuery(List(req), entRoot)

    val tree =
      ("RegA-SubA" owns "RegA-SubA-DeviceA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Tree request downwards") {
    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA")

    val req = new QueryNode(Some("owns"), Some(true), None, None, List("Line"), List(
      new QueryNode(Some("owns"), Some(true), None, None, List("Point"), Nil)))
    val results = resultsForQuery(List(req), entRoot)

    val tree =
      ("RegA-SubA" owns "RegA-SubA-DeviceA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB") ::
        ("RegA-SubA-DeviceA" owns "RegA-SubA-DeviceA-PointA") ::
        ("RegA-SubA-DeviceA" owns "RegA-SubA-DeviceA-PointB") ::
        ("RegA-SubA-DeviceB" owns "RegA-SubA-DeviceB-PointA") ::
        ("RegA-SubA-DeviceB" owns "RegA-SubA-DeviceB-PointB") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)

  }

  test("Tree request derived") {
    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA")

    val req = new QueryNode(Some("owns"), Some(true), None, None, List("Point"), Nil)
    val results = resultsForQuery(List(req), entRoot)

    val tree =
      ("RegA-SubA" owns "RegA-SubA-DeviceA-PointA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceA-PointB") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB-PointA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB-PointB") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceC-PointA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceC-PointB") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Tree request upwards") {

    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA-DeviceB-PointA")

    val req = new QueryNode(Some("owns"), Some(false), None, None, List("Line"), List(
      new QueryNode(Some("owns"), Some(false), None, None, List("Substation"), Nil)))
    val results = resultsForQuery(List(req), entRoot)

    val tree =
      ("RegA-SubA-DeviceB" owns "RegA-SubA-DeviceB-PointA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB") :: Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Query branching on relation") {
    val entRoot = EntityQueryManager.entities.where(t => t.name === "RegA-SubA")

    val req = new QueryNode(Some("owns"), Some(true), None, Some("RegA-SubA-DeviceA"), Nil,
      List(new QueryNode(Some("owns"), Some(true), None, None, List("Point"), Nil),
        new QueryNode(Some("refs"), Some(true), None, None, List("Point"), Nil)))

    val results = resultsForQuery(req, entRoot)

    val tree =
      ("RegA-SubA" owns "RegA-SubA-DeviceA") ::
        ("RegA-SubA-DeviceA" owns "RegA-SubA-DeviceA-PointA") ::
        ("RegA-SubA-DeviceA" owns "RegA-SubA-DeviceA-PointB") ::
        ("RegA-SubA-DeviceA" refs "RegA-SubA-DeviceA-PointC") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Query branching on type") {
    val entRoot = EntityQueryManager.entities.where(t => t.name === "RegA")

    val req = new QueryNode(Some("owns"), Some(true), None, Some("RegA-SubA"), Nil,
      List(new QueryNode(Some("owns"), Some(true), None, None, List("Line"), Nil),
        new QueryNode(Some("owns"), Some(true), None, None, List("Breaker"), Nil)))

    val results = resultsForQuery(req, entRoot)

    val tree =
      ("RegA" owns "RegA-SubA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceA") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceB") ::
        ("RegA-SubA" owns "RegA-SubA-DeviceC") ::
        Nil

    checkResults(results.flatMap(parseResults(_)), tree)
  }

  test("Get by name and type (exists)") {
    val req = EntityProto.newBuilder.setName("RegA-SubA").addTypes("Substation")
    val spec = List("RegA-SubA")

    checkNames(req, spec)
  }

  test("Get by name and type (nonexistant") {
    val req = EntityProto.newBuilder.setName("RegA-SubA").addTypes("Region")
    val spec = List()

    checkNames(req, spec)
  }

  test("Get by name") {
    val req = EntityProto.newBuilder.setName("RegA-SubA")
    val spec = List("RegA-SubA")

    checkNames(req, spec)
  }

  test("Get by type") {
    val req = EntityProto.newBuilder.addTypes("Substation")
    val spec = List("RegA-SubA", "RegA-SubB")

    checkNames(req, spec)
  }

  test("Get by multiple types") {
    val req = EntityProto.newBuilder.addTypes("Substation").addTypes("Region")
    val spec = List("RegA-SubA", "RegA-SubB", "RegA")

    checkNames(req, spec)
  }

  test("Get by secondary types") {
    val req = EntityProto.newBuilder.addTypes("EquipmentGroup")
    val spec = List("RegA-SubA", "RegA-SubB", "RegA")

    checkNames(req, spec)
  }

  test("Get all") {
    val req = EntityProto.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*"))

    val ents = ApplicationSchema.entities.where(t => true === true)
    val spec = ents.map(_.name).toList

    checkNames(req, spec)
  }

  // Bad join code can create two results instead of one
  test("Double types") {
    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA").head
    val req = EntityProto.newBuilder
      .setUuid(ReefUUID.newBuilder.setUuid(entRoot.id.toString))
      .setName("RegA-SubA")
      .addTypes("Substation")
      .addTypes("EquipmentGroup")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .addEntities(
            EntityProto.newBuilder
              .addTypes("Equipment"))).build

    val results = EntityQueryManager.fullQuery(req)
    results.length should equal(1)
  }

  test("Ids for type") {
    val entRoot = ApplicationSchema.entities.where(t => t.name === "RegA-SubA")

    val req = new QueryNode(Some("owns"), Some(true), None, None, List("Line"), List(
      new QueryNode(Some("owns"), Some(true), None, None, List("Point"), Nil)))
    val results = resultsForQuery(List(req), entRoot)

    val ids = results.flatMap(_.idsForType("Point"))
    val names =
      from(ApplicationSchema.entities)(ent =>
        where(ent.id in ids)
          select (ent.name)).toList

    val spec = List("RegA-SubA-DeviceA-PointA",
      "RegA-SubA-DeviceA-PointB",
      "RegA-SubA-DeviceB-PointA",
      "RegA-SubA-DeviceB-PointB")

    checkResults(spec, names)
  }

  test("Asking for unknown Type") {
    val req = EntityProto.newBuilder.addTypes("ShouldHaveBeenSubstation")

    intercept[BadRequestException] {
      EntityQueryManager.checkAllTypesInSystem(req.build)
    }
  }

  test("Asking for unknown in sub proto") {
    val req = EntityProto.newBuilder.setName("Nonexistent")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("refs")
          .setDescendantOf(true)
          .setDistance(1).addEntities(
            EntityProto.newBuilder.addTypes("ShouldHaveBeenEquipment")))

    intercept[BadRequestException] {
      EntityQueryManager.checkAllTypesInSystem(req.build)
    }
  }

}