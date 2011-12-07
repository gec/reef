/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ Entity }
import org.totalgrid.reef.client.service.proto.Utils.{ Attribute }

import org.totalgrid.reef.client.sapi.rpc.impl.builders.EntityAttributesBuilders
import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite

@RunWith(classOf[JUnitRunner])
class EntityAttributesRequestTest
    extends ClientSessionSuite("EntityAttributes.xml", "EntityAttributes",
      <div>
        <p>
          Attributes can be attached to entities to provide extra information (i.e. location, display name).
        </p>
      </div>)
    with ShouldMatchers {

  import EntityAttributesBuilders._

  test("Puts") {
    val ent = session.get(Entity.newBuilder.setName("StaticSubstation").build).await.expectOne

    recorder.addExplanation("Put by entity id", "Creates or replaces an attribute for the specified entity (selected by id).")
    val resp1 = session.put(
      EntityAttributesBuilders.putAttributesToEntityId(
        ent.getUuid,
        List(Attribute.newBuilder.setName("subName").setVtype(Attribute.Type.STRING).setValueString("Apex").build))).await.expectOne

    resp1.getAttributesCount should equal(1)

    recorder.addExplanation("Put by entity name", "Creates or replaces two attributes for the specified entity (selected by name).")
    val resp2 = session.put(
      EntityAttributesBuilders.putAttributesToEntityName(
        "StaticSubstation",
        List(Attribute.newBuilder.setName("gisLat").setVtype(Attribute.Type.DOUBLE).setValueDouble(41.663).build,
          Attribute.newBuilder.setName("gisLong").setVtype(Attribute.Type.DOUBLE).setValueDouble(84.99).build))).await.expectOne

    resp2.getAttributesCount should equal(2)

    {
      recorder.addExplanation("Get by entity id", "Finds the attributes associated with a particular entity.")
      val resp = session.get(getForEntityId(ent.getUuid)).await.expectOne
      resp.getAttributesCount should equal(2)
    }
    {
      recorder.addExplanation("Get by entity name", "Finds the attributes associated with a particular entity.")
      val resp = session.get(getForEntityName("StaticSubstation")).await.expectOne
      resp.getAttributesCount should equal(2)
    }

    {
      recorder.addExplanation("Delete by entity id", "Deletes the attributes associated with a particular entity.")
      val resp = session.delete(getForEntityId(ent.getUuid)).await.expectOne
      resp.getAttributesCount should equal(2)
    }

    {
      session.put(
        EntityAttributesBuilders.putAttributesToEntityName(
          "StaticSubstation",
          List(Attribute.newBuilder.setName("gisLat").setVtype(Attribute.Type.DOUBLE).setValueDouble(41.663).build,
            Attribute.newBuilder.setName("gisLong").setVtype(Attribute.Type.DOUBLE).setValueDouble(84.99).build))).await.expectOne

      recorder.addExplanation("Delete by entity name", "Deletes the attributes associated with a particular entity.")
      val resp = session.delete(getForEntityName(ent.getName)).await.expectOne
      resp.getAttributesCount should equal(2)
    }
  }

  test("API") {
    val ent = client.getEntityByName("StaticSubstation").await

    val id = ent.getUuid

    {
      val attr = client.getEntityAttributes(id).await
      attr.getAttributesCount should equal(0)
    }

    {
      val attr = client.setEntityAttribute(id, "test01", "testString").await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
      attr.getAttributesList.get(0).getValueString should equal("testString")
    }

    {
      val attr = client.setEntityAttribute(id, "test02", true).await
      attr.getAttributesCount should equal(2)
    }

    {
      val attr = client.removeEntityAttribute(id, "test01").await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0).getName should equal("test02")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.BOOL)
      attr.getAttributesList.get(0).getValueBool should equal(true)
    }

    val attr = client.clearEntityAttributes(id).await
    attr.map { _.getAttributesCount } should equal(Some(1))

  }

  test("Set types") {
    val ent = client.getEntityByName("StaticSubstation").await
    val id = ent.getUuid

    {
      val attr = client.setEntityAttribute(id, "test01", true).await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.BOOL)
      attr.getAttributesList.get(0).getValueBool should equal(true)
      client.removeEntityAttribute(id, "test01").await
    }
    {
      val attr = client.setEntityAttribute(id, "test01", 23432).await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.SINT64)
      attr.getAttributesList.get(0).getValueSint64 should equal(23432)
      client.removeEntityAttribute(id, "test01").await
    }
    {
      val attr = client.setEntityAttribute(id, "test01", 5.437).await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.DOUBLE)
      attr.getAttributesList.get(0).getValueDouble should equal(5.437)
      client.removeEntityAttribute(id, "test01").await
    }
    {
      val attr = client.setEntityAttribute(id, "test01", "test").await
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
      attr.getAttributesList.get(0).getValueString should equal("test")
      client.removeEntityAttribute(id, "test01").await
    }
  }

}
