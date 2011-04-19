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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.Utils.Attribute

import org.totalgrid.reef.api.Envelope.Status

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.api.BadRequestException
import com.google.protobuf.ByteString

import scala.collection.JavaConversions._
import org.totalgrid.reef.models.{ DatabaseUsingTestBase, ApplicationSchema, EntityAttribute }
import org.totalgrid.reef.proto.Model.{ ReefUUID, Entity, EntityAttributes }

@RunWith(classOf[JUnitRunner])
class EntityAttributesServiceTest extends DatabaseUsingTestBase {

  def seedEntity(name: String, typ: String) = {
    ReefUUID.newBuilder.setUuid(EQ.addEntity(name, typ).id.toString).build
  }

  protected val service = new EntityAttributesService

  test("Put") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUuid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build

    val result = one(Status.OK, service.put(entAttr))
    result.getAttributesCount should equal(1)
  }

  test("Bad put - no entity") {
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.addAttributes(attribute).build))
  }

  /*test("Bad put - no attributes") {
    val entity = Entity.newBuilder.setUuid("fake").build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).build))
  }*/

  test("Bad put - entity doesn't exist") {
    val entity = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("0")).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build))
  }

  test("Bad put - type but no value") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUuid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build))
  }

  def simpleGetScenario = {
    transaction {
      val id = EQ.addEntity("ent01", "entType1").id
      EQ.addEntity("ent02", "entType2")

      ApplicationSchema.entityAttributes.insert(new EntityAttribute(id, "attr01", Some("hello"), None, None, None, None))
      ReefUUID.newBuilder.setUuid(id.toString).build
    }
  }

  def attrReq(entityUid: ReefUUID, attributes: List[Attribute]) = {
    val entity = Entity.newBuilder.setUuid(entityUid).build
    EntityAttributes.newBuilder.setEntity(entity).addAllAttributes(attributes.toList).build
  }

  test("Second put modifies") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUuid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build

    val result = one(Status.OK, service.put(entAttr))
    result.getAttributesCount should equal(1)
    val attr = result.getAttributesList.get(0)
    attr.getValueSint64 should equal(56)

    val result2 = one(Status.OK, service.put(EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute.toBuilder.setValueSint64(23)).build))

    result2.getAttributesCount should equal(1)
    result2.getAttributesList.get(0).getValueSint64 should equal(23)
  }

  test("Put fully replaces") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val initial = Attribute.newBuilder.setName("testAttr01").setVtype(Attribute.Type.SINT64).setValueSint64(56).build ::
      Attribute.newBuilder.setName("testAttr02").setVtype(Attribute.Type.SINT64).setValueSint64(23).build ::
      Nil

    val entAttr = attrReq(entUid, initial)

    val result = one(Status.OK, service.put(entAttr))
    result.getAttributesCount should equal(2)

    val req2 = attrReq(entUid, List(Attribute.newBuilder.setName("testAttr03").setVtype(Attribute.Type.SINT64).setValueSint64(400).build))
    val result2 = one(Status.OK, service.put(req2))

    result2.getAttributesCount should equal(1)
    result2.getAttributesList.get(0).getName should equal("testAttr03")
  }

  def checkSimpleGetScenario(request: EntityAttributes) = {
    val result = one(Status.OK, service.get(request))
    result.getEntity.getName should equal("ent01")
    result.getAttributesCount should equal(1)
    result.getAttributesList.get(0).getName should equal("attr01")
    result.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
    result.getAttributesList.get(0).hasValueString should equal(true)
    result.getAttributesList.get(0).getValueString should equal("hello")
  }

  test("Get all") {
    simpleGetScenario

    val entity = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).build

    val results = many(2, service.get(entAttr))
    println(results)

    results.foreach { result =>
      if (result.getEntity.getName == "ent01") {
        result.getAttributesCount should equal(1)
        result.getAttributesList.get(0).getName should equal("attr01")
        result.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
        result.getAttributesList.get(0).hasValueString should equal(true)
        result.getAttributesList.get(0).getValueString should equal("hello")
      } else {
        result.getAttributesCount should equal(0)
      }
    }
  }

  test("Get by uid") {
    val entUid = simpleGetScenario

    val entity = Entity.newBuilder.setUuid(entUid).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).build

    checkSimpleGetScenario(entAttr)
  }

  test("Get by name") {
    simpleGetScenario

    val entity = Entity.newBuilder.setName("ent01").build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).build

    checkSimpleGetScenario(entAttr)
  }

  test("Get multiple") {
    transaction {
      val entId1 = EQ.addEntity("ent01", "entType1").id
      val entId2 = EQ.addEntity("ent02", "entType2").id

      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId1, "attr01", Some("hello"), None, None, None, None))
      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId1, "attr02", Some("again"), None, None, None, None))

      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId2, "attr03", Some("hello"), None, None, None, None))
      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId2, "attr04", Some("again"), None, None, None, None))
    }

    val entity = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).build

    val results = many(2, service.get(entAttr))
    results.foreach { ent =>
      ent.getAttributesCount should equal(2)
    }
  }

  def roundtrip[A](v: A, typ: Attribute.Type, setup: (Attribute.Builder, A) => Unit, get: Attribute => A) = {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUuid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(typ)
    setup(attribute, v)

    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build
    one(Status.OK, service.put(entAttr))

    val result = one(Status.OK, service.get(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(entUid)).build))

    result.getAttributesCount should equal(1)
    val attr = result.getAttributesList.get(0)
    attr.getVtype should equal(typ)
    get(attr) should equal(v)
  }

  test("Roundtrip long") {
    roundtrip[Long](50L, Attribute.Type.SINT64, _ setValueSint64 _, _.getValueSint64)
  }
  test("Roundtrip bool") {
    roundtrip[Boolean](true, Attribute.Type.BOOL, _ setValueBool _, _.getValueBool)
  }
  test("Roundtrip string") {
    roundtrip[String]("blah", Attribute.Type.STRING, _ setValueString _, _.getValueString)
  }
  test("Roundtrip double") {
    roundtrip[Double](4234.44, Attribute.Type.DOUBLE, _ setValueDouble _, _.getValueDouble)
  }
  test("Roundtrip bytes") {
    roundtrip[Array[Byte]](
      List(Byte.MaxValue, Byte.MinValue).toArray,
      Attribute.Type.BYTES,
      (b, v) => b.setValueBytes(ByteString.copyFrom(v)),
      b => b.getValueBytes.toByteArray)
  }

  test("Delete") {
    val entId = deleteScenario

    val resp = one(Status.OK, service.delete(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(entId)).build))
    resp.getAttributesCount should equal(0)

    noneForEntity(entId)
  }

  test("Put no attributes is delete") {
    val entId = deleteScenario

    val resp = one(Status.OK, service.put(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(entId)).build))
    resp.getAttributesCount should equal(0)

    noneForEntity(entId)
  }

  def deleteScenario = {
    transaction {
      val entId1 = EQ.addEntity("ent01", "entType1").id
      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId1, "attr01", Some("hello"), None, None, None, None))
      ApplicationSchema.entityAttributes.insert(new EntityAttribute(entId1, "attr02", Some("again"), None, None, None, None))
      ReefUUID.newBuilder.setUuid(entId1.toString).build
    }
  }

  def noneForEntity(entId: ReefUUID) = {
    transaction {
      ApplicationSchema.entityAttributes.where(t => t.entityId === entId.getUuid.toLong).toList should equal(Nil)
    }
  }

}
