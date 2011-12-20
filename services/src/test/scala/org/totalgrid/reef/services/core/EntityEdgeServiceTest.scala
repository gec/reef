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
import org.totalgrid.reef.client.exception.{BadRequestException, ReefServiceException}

@RunWith(classOf[JUnitRunner])
class EntityEdgeServiceTest extends DatabaseUsingTestBase {

  import ApplicationSchema._

  //val service = new EntityService
  val service = new EntityEdgeModelService(new EntityEdgeServiceModel)


  def buildEdge(parent: String, child: String, rel: String) = {
    EntityEdgeProto.newBuilder()
      .setParent(EntityProto.newBuilder().setName(parent))
      .setChild(EntityProto.newBuilder().setName(child))
      .setRelationship(rel)
      .build
  }

  test("Put single") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)
    
    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal (reg.id.toString)
    result.getChild.getUuid.getValue should equal (sub.id.toString)
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

  test("Put duplicate") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)

    val edge = buildEdge("Reg", "Sub", "owns")

    val result = service.put(edge).expectOne(Status.CREATED)

    val uuid = result.getUuid.getValue
    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal (reg.id.toString)
    result.getChild.getUuid.getValue should equal (sub.id.toString)

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
    result.getParent.getUuid.getValue should equal (reg.id.toString)
    result.getChild.getUuid.getValue should equal (sub.id.toString)

    val two = buildEdge("Reg", "Sub", "relates")

    val second = service.put(two).expectOne(Status.CREATED)
    second.getRelationship should equal("relates")
    second.getParent.getUuid.getValue should equal (reg.id.toString)
    second.getChild.getUuid.getValue should equal (sub.id.toString)
  }


  test("Put multi-level") {
    val reg = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val sub = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    val dev = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)

    val initial = buildEdge("Reg", "Sub", "owns")

    val result = service.put(initial).expectOne(Status.CREATED)

    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal (reg.id.toString)
    result.getChild.getUuid.getValue should equal (sub.id.toString)

    val second = buildEdge("Sub", "Breaker", "owns")
    
    val secondResult = service.put(second).expectOne(Status.CREATED)

    result.getRelationship should equal("owns")
    result.getParent.getUuid.getValue should equal (sub.id.toString)
    result.getChild.getUuid.getValue should equal (dev.id.toString)

    edges.where(t => true === true).toList.size should equal(3)
  }
}