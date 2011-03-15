/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ Entity, EntityAttributes }
import org.totalgrid.reef.proto.Utils.{ Attribute }
import org.totalgrid.reef.api.request.builders.EntityAttributesBuilders

@RunWith(classOf[JUnitRunner])
class EntityAttributesRequestTest
    extends ServiceClientSuite("EntityAttributes.xml", "EntityAttributes",
      <div>
        <p>
          Attributes can be attached to entities to provide extra information (i.e. location, display name).
        </p>
      </div>)
    with ShouldMatchers {

  import EntityAttributesBuilders._

  test("Puts") {
    val ent = client.getOneOrThrow(Entity.newBuilder.setName("StaticSubstation").build)

    client.addExplanation("Put by entity uid", "Creates or replaces an attribute for the specified entity (selected by uid).")
    val resp1 = client.putOneOrThrow(
      EntityAttributesBuilders.putAttributesToEntityUid(
        ent.getUid,
        List(Attribute.newBuilder.setName("subName").setVtype(Attribute.Type.STRING).setValueString("Apex").build)))

    resp1.getAttributesCount should equal(1)

    client.addExplanation("Put by entity name", "Creates or replaces two attributes for the specified entity (selected by name).")
    val resp2 = client.putOneOrThrow(
      EntityAttributesBuilders.putAttributesToEntityName(
        "StaticSubstation",
        List(Attribute.newBuilder.setName("gisLat").setVtype(Attribute.Type.DOUBLE).setValueDouble(41.663).build,
          Attribute.newBuilder.setName("gisLong").setVtype(Attribute.Type.DOUBLE).setValueDouble(84.99).build)))

    resp2.getAttributesCount should equal(2)

    {
      client.addExplanation("Get by entity uid", "Finds the attributes associated with a particular entity.")
      val resp = client.getOneOrThrow(getForEntityUid(ent.getUid))
      resp.getAttributesCount should equal(2)
    }
    {
      client.addExplanation("Get by entity name", "Finds the attributes associated with a particular entity.")
      val resp = client.getOneOrThrow(getForEntityName("StaticSubstation"))
      resp.getAttributesCount should equal(2)
    }

    {
      client.addExplanation("Delete by entity uid", "Deletes the attributes associated with a particular entity.")
      val resp = client.deleteOneOrThrow(getForEntityUid(ent.getUid))
      resp.getAttributesCount should equal(0)
    }

    {
      client.putOneOrThrow(
        EntityAttributesBuilders.putAttributesToEntityName(
          "StaticSubstation",
          List(Attribute.newBuilder.setName("gisLat").setVtype(Attribute.Type.DOUBLE).setValueDouble(41.663).build,
            Attribute.newBuilder.setName("gisLong").setVtype(Attribute.Type.DOUBLE).setValueDouble(84.99).build)))

      client.addExplanation("Delete by entity name", "Deletes the attributes associated with a particular entity.")
      val resp = client.deleteOneOrThrow(getForEntityName(ent.getName))
      resp.getAttributesCount should equal(0)
    }
  }

}
