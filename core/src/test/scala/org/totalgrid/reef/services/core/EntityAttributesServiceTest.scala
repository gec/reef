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

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.proto.Model.{ Entity, EntityAttributes }
import org.totalgrid.reef.proto.Utils.Attribute

import org.totalgrid.reef.api.Envelope.Status

import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.api.BadRequestException
import org.totalgrid.reef.models.{ EntityAttribute, ApplicationSchema, RunTestsInsideTransaction }
import com.google.protobuf.ByteString

@RunWith(classOf[JUnitRunner])
class EntityAttributesServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach with RunTestsInsideTransaction {

  override def beforeAll() = DbConnector.connect(DbInfo.loadInfo("test"))

  override def beforeEach() = transaction { ApplicationSchema.reset }

  def seedEntity(name: String, typ: String) = {
    EQ.addEntity(name, typ).id.toString
  }

  protected val service = new EntityAttributesService

  test("Put") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build

    val result = one(Status.CREATED, service.put(entAttr))
    result.getAttributesCount should equal(1)
  }

  test("Bad put - no entity") {
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.addAttributes(attribute).build))
  }

  test("Bad put - no attributes") {
    val entity = Entity.newBuilder.setUid("fake").build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).build))
  }

  test("Bad put - entity doesn't exist") {
    val entity = Entity.newBuilder.setUid("0").build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).setValueSint64(56).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build))
  }

  test("Bad put - type but no value") {
    val entUid = transaction {
      seedEntity("ent01", "entType")
    }

    val entity = Entity.newBuilder.setUid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(Attribute.Type.SINT64).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build

    intercept[BadRequestException](service.put(EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build))
  }

  def simpleGetScenario = {
    transaction {
      val id = EQ.addEntity("ent01", "entType1").id
      EQ.addEntity("ent02", "entType2")

      ApplicationSchema.entityAttributes.insert(new EntityAttribute(id, "attr01", Some("hello"), None, None, None, None))
      id.toString
    }
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

  test("Get by uid") {
    val entUid = simpleGetScenario

    val entity = Entity.newBuilder.setUid(entUid).build
    val entAttr = EntityAttributes.newBuilder.setEntity(entity).build

    checkSimpleGetScenario(entAttr)
  }

  test("Get all") {
    simpleGetScenario

    val entity = Entity.newBuilder.setUid("*").build
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

    val entity = Entity.newBuilder.setUid("*").build
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

    val entity = Entity.newBuilder.setUid(entUid).build
    val attribute = Attribute.newBuilder.setName("testAttr").setVtype(typ)
    setup(attribute, v)

    val entAttr = EntityAttributes.newBuilder.setEntity(entity).addAttributes(attribute).build
    one(Status.CREATED, service.put(entAttr))

    val result = one(Status.OK, service.get(EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUid(entUid)).build))

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

}
