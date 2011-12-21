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
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.models.{ ApplicationSchema, DatabaseUsingTestBase }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ Relationship, Entity => EntityProto, EntityEdge => EntityEdgeProto, ReefUUID }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.exception.{ BadRequestException, ReefServiceException }

import org.squeryl.PrimitiveTypeMode._

@RunWith(classOf[JUnitRunner])
class EntityEdgeServiceTest extends DatabaseUsingTestBase {

  import ApplicationSchema._

  val service = new EntityEdgeModelService(new EntityEdgeServiceModel)

  def buildEdge(parent: String, child: String, rel: String) = {
    EntityEdgeProto.newBuilder()
      .setParent(EntityProto.newBuilder().setName(parent))
      .setChild(EntityProto.newBuilder().setName(child))
      .setRelationship(rel)
      .build
  }
  def buildEdge(parent: Option[String], child: Option[String], rel: Option[String]) = {
    val b = EntityEdgeProto.newBuilder()

    parent.foreach(par => b.setParent(EntityProto.newBuilder().setName(par)))
    child.foreach(ch => b.setChild(EntityProto.newBuilder().setName(ch)))
    rel.foreach(relate => b.setRelationship(relate))

    b.build
  }

  test("Put single") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)

    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal(reg.id.toString)
    result.getChild.getUuid.getValue should equal(sub.id.toString)
  }

  test("Put with missing properties") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    {
      val edge = EntityEdgeProto.newBuilder()
        .setParent(EntityProto.newBuilder().setName("Reg"))
        .setChild(EntityProto.newBuilder().setName("Sub"))
        .build

      intercept[BadRequestException] {
        service.put(edge)
      }
    }
    {
      val edge = EntityEdgeProto.newBuilder()
        .setParent(EntityProto.newBuilder().setName("Reg"))
        .setRelationship("owns")
        .build

      intercept[BadRequestException] {
        service.put(edge)
      }
    }
    {
      val edge = EntityEdgeProto.newBuilder()
        .setChild(EntityProto.newBuilder().setName("Sub"))
        .setRelationship("owns")
        .build

      intercept[BadRequestException] {
        service.put(edge)
      }
    }
  }

  test("Put same entity") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Reg", "owns")

    intercept[BadRequestException] {
      service.put(edge)
    }
  }

  test("Put to nonexistent entity") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Wrong", "owns")

    intercept[BadRequestException] {
      service.put(edge)
    }
  }

  test("Put to nonexistent entity partial match possible") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)

    val uuid = result.getUuid.getValue
    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal(reg.id.toString)
    result.getChild.getUuid.getValue should equal(sub.id.toString)

    val partial = buildEdge("Reg", "Wrong", "owns")

    intercept[BadRequestException] {
      service.put(partial)
    }
  }

  test("Put duplicate") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)

    val uuid = result.getUuid.getValue
    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal(reg.id.toString)
    result.getChild.getUuid.getValue should equal(sub.id.toString)

    val second = service.put(edge).expectOne(Status.NOT_MODIFIED)
    second.getUuid.getValue should equal(uuid)
  }

  test("Put second with different relationship") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)

    val uuid = result.getUuid.getValue
    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal(reg.id.toString)
    result.getChild.getUuid.getValue should equal(sub.id.toString)

    val two = buildEdge("Reg", "Sub", "relates")

    val second = service.put(two).expectOne(Status.CREATED)
    second.getRelationship should equal("relates")
    second.getParent.getUuid.getValue should equal(reg.id.toString)
    second.getChild.getUuid.getValue should equal(sub.id.toString)
  }

  test("Put multi-level") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val dev = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)

    val result = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal(reg.id.toString)
    result.getChild.getUuid.getValue should equal(sub.id.toString)

    val secondResult = service.put(buildEdge("Sub", "Bkr", "owns")).expectOne(Status.CREATED)

    secondResult.getRelationship should equal("owns")
    secondResult.getParent.getUuid.getValue should equal(sub.id.toString)
    secondResult.getChild.getUuid.getValue should equal(dev.id.toString)

    val edgeList = edges.where(t => true === true).toList
    edgeList.size should equal(3)

    val (topList, rest) = edgeList.partition(_.id == result.getUuid.getValue.toInt)
    val top = topList.head

    val (bottomList, last) = rest.partition(_.id == secondResult.getUuid.getValue.toInt)
    val bottom = bottomList.head

    val multi = last.head
    multi.parentId should equal(reg.id)
    multi.childId should equal(dev.id)

    val derivedList = derivedEdges.where(t => true === true).toList

    derivedList.size should equal(1)
    val derived = derivedList.head
    derived.edgeId should equal(bottom.id)
    derived.parentEdgeId should equal(multi.id)
  }

  test("Get") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    val result = service.get(buildEdge("Reg", "Sub", "owns")).expectOne()
    result.getUuid.getValue should equal(upload.getUuid.getValue)
  }

  test("Get partial: parent") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    val result = service.get(buildEdge(Some("Reg"), None, None)).expectOne()
    result.getUuid.getValue should equal(upload.getUuid.getValue)
  }

  test("Get partial: child") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    val result = service.get(buildEdge(None, Some("Sub"), None)).expectOne()
    result.getUuid.getValue should equal(upload.getUuid.getValue)
  }

  test("Get partial: relationship") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    val result = service.get(buildEdge(None, None, Some("owns"))).expectOne()
    result.getUuid.getValue should equal(upload.getUuid.getValue)
  }

  test("Get multiple") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val dev = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    val top = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)
    val bottom = service.put(buildEdge("Sub", "Bkr", "owns")).expectOne(Status.CREATED)

    val results = service.get(buildEdge(Some("Reg"), None, None)).expectMany(2)

    val topGot = results.find(r => r.getUuid.getValue == top.getUuid.getValue)
    topGot should not equal (None)

    val depthGot = results.find(r => r.getChild.getUuid.getValue == dev.id.toString)
    depthGot should not equal (None)
  }

  test("Get wrong parent") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    service.get(buildEdge("Wrong", "Sub", "owns")).expectNone()
  }
  test("Get wrong child") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    service.get(buildEdge("Reg", "Wrong", "owns")).expectNone()
  }
  test("Get wrong relationship") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    service.get(buildEdge("Reg", "Sub", "Wrong")).expectNone()
  }

  test("Delete") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val upload = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)

    edges.where(t => true === true).toList.size should equal(1)

    val result = service.delete(buildEdge("Reg", "Sub", "owns")).expectOne()
    result.getUuid.getValue should equal(upload.getUuid.getValue)

    edges.where(t => true === true).toList.size should equal(0)
  }

  test("Delete Multi") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val dev = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    val top = service.put(buildEdge("Reg", "Sub", "owns")).expectOne(Status.CREATED)
    val bottom = service.put(buildEdge("Sub", "Bkr", "owns")).expectOne(Status.CREATED)

    edges.where(t => true === true).toList.size should equal(3)
    derivedEdges.where(t => true === true).toList.size should equal(1)

    val result = service.delete(buildEdge("Sub", "Bkr", "owns")).expectOne()
    result.getUuid.getValue should equal(bottom.getUuid.getValue)

    edges.where(t => true === true).toList.size should equal(1)
    derivedEdges.where(t => true === true).toList.size should equal(0)
  }
}