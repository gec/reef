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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.service.proto.Utils.{ Attribute }

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class EntityAttributesRequestTest extends ServiceClientSuite {

  test("Puts") {
    val ent = client.getEntityByName("StaticSubstation")
    val entUuid = ent.getUuid

    val resp1 = client.setEntityAttribute(entUuid, "subName", "Apex")
    resp1.getAttributesCount should equal(1)

    val resp2 = client.setEntityAttribute(entUuid, "gisLat", 41.663)
    resp2.getAttributesCount should equal(2)

    val resp = client.getEntityAttributes(entUuid)
    resp.getAttributesCount should equal(2)

    client.clearEntityAttributes(entUuid)
  }

  test("API") {
    val ent = client.getEntityByName("StaticSubstation")

    val id = ent.getUuid

    {
      val attr = client.getEntityAttributes(id)
      attr.getAttributesCount should equal(0)
    }

    {
      val attr = client.setEntityAttribute(id, "test01", "testString")
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
      attr.getAttributesList.get(0).getValueString should equal("testString")
    }

    {
      val attr = client.setEntityAttribute(id, "test02", true)
      attr.getAttributesCount should equal(2)
    }

    {
      val attr = client.removeEntityAttribute(id, "test01")
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0).getName should equal("test02")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.BOOL)
      attr.getAttributesList.get(0).getValueBool should equal(true)
    }

    val attr = client.clearEntityAttributes(id)
    attr.map { _.getAttributesCount } should equal(Some(1))

  }

  test("Set types") {
    val ent = client.getEntityByName("StaticSubstation")
    val id = ent.getUuid

    {
      val attr = client.setEntityAttribute(id, "test01", true)
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.BOOL)
      attr.getAttributesList.get(0).getValueBool should equal(true)
      client.removeEntityAttribute(id, "test01")
    }
    {
      val attr = client.setEntityAttribute(id, "test01", 23432)
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.SINT64)
      attr.getAttributesList.get(0).getValueSint64 should equal(23432)
      client.removeEntityAttribute(id, "test01")
    }
    {
      val attr = client.setEntityAttribute(id, "test01", 5.437)
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.DOUBLE)
      attr.getAttributesList.get(0).getValueDouble should equal(5.437)
      client.removeEntityAttribute(id, "test01")
    }
    {
      val attr = client.setEntityAttribute(id, "test01", "test")
      attr.getAttributesCount should equal(1)
      attr.getAttributesList.get(0)
      attr.getAttributesList.get(0).getName should equal("test01")
      attr.getAttributesList.get(0).getVtype should equal(Attribute.Type.STRING)
      attr.getAttributesList.get(0).getValueString should equal("test")
      client.removeEntityAttribute(id, "test01")
    }
  }

}
