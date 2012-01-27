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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.services.core.SubscriptionTools.SubscriptionTesting
import org.totalgrid.reef.client.service.proto.Model.{ Entity, EntityAttribute }
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._

@RunWith(classOf[JUnitRunner])
class EntityAttributeServiceTest extends DatabaseUsingTestBase {

  class Fixture extends SubscriptionTesting {
    def _dbConnection = dbConnection

    val dependencies = new ServiceDependenciesDefaults(dbConnection)
    val factories = new ModelFactories(dependencies)
    val headers = BasicRequestHeaders.empty.setUserName("user")

    val s = new SyncService(new EntityAttributeService(factories.attributes), contextSource)

    val ent = EntityTestSeed.addEntity("ent01", List("typ01"))
  }

  def stringAttr(attrName: String, v: String) = {
    EntityAttribute.newBuilder
      .setEntity(Entity.newBuilder.setName("ent01"))
      .setAttribute(Attribute.newBuilder.setName(attrName).setVtype(Attribute.Type.STRING).setValueString(v))
      .build
  }

  def boolAttr(attrName: String, v: Boolean) = {
    EntityAttribute.newBuilder
      .setEntity(Entity.newBuilder.setName("ent01"))
      .setAttribute(Attribute.newBuilder.setName(attrName).setVtype(Attribute.Type.BOOL).setValueBool(v))
      .build
  }

  test("Put") {
    val f = new Fixture

    val created = f.s.put(stringAttr("attr01", "value01")).expectOne(Status.CREATED)

    val eventList = List((ADDED, classOf[EntityAttribute]))
    f.eventCheck should equal(eventList)
  }

  test("Put modify value") {
    val f = new Fixture

    val created = f.s.put(stringAttr("attr01", "value01")).expectOne(Status.CREATED)
    created.getEntity.getName should equal("ent01")
    created.getAttribute.getName should equal("attr01")
    created.getAttribute.getVtype should equal(Attribute.Type.STRING)
    created.getAttribute.getValueString should equal("value01")

    val updated = f.s.put(stringAttr("attr01", "value02")).expectOne(Status.UPDATED)
    updated.getEntity.getName should equal("ent01")
    updated.getAttribute.getName should equal("attr01")
    updated.getAttribute.getVtype should equal(Attribute.Type.STRING)
    updated.getAttribute.getValueString should equal("value02")

    val eventList = List((ADDED, classOf[EntityAttribute]), (MODIFIED, classOf[EntityAttribute]))
    f.eventCheck should equal(eventList)
  }

  test("Put modify type") {
    val f = new Fixture

    val created = f.s.put(stringAttr("attr01", "value01")).expectOne(Status.CREATED)
    created.getEntity.getName should equal("ent01")
    created.getAttribute.getName should equal("attr01")
    created.getAttribute.getVtype should equal(Attribute.Type.STRING)
    created.getAttribute.getValueString should equal("value01")

    val updated = f.s.put(boolAttr("attr01", false)).expectOne(Status.UPDATED)
    updated.getEntity.getName should equal("ent01")
    updated.getAttribute.getName should equal("attr01")
    updated.getAttribute.getVtype should equal(Attribute.Type.BOOL)
    updated.getAttribute.getValueBool should equal(false)

    val eventList = List((ADDED, classOf[EntityAttribute]), (MODIFIED, classOf[EntityAttribute]))
    f.eventCheck should equal(eventList)
  }

  test("Delete / full lifecycle") {
    val f = new Fixture

    val created = f.s.put(stringAttr("attr01", "value01")).expectOne(Status.CREATED)
    val updated = f.s.put(stringAttr("attr01", "value02")).expectOne(Status.UPDATED)

    val attr = EntityAttribute.newBuilder
      .setEntity(Entity.newBuilder.setName("ent01"))
      .setAttribute(Attribute.newBuilder.setName("attr01").setVtype(Attribute.Type.BOOL))
      .build

    val deleted = f.s.delete(attr).expectOne(Status.DELETED)

    val eventList = List((ADDED, classOf[EntityAttribute]), (MODIFIED, classOf[EntityAttribute]), (REMOVED, classOf[EntityAttribute]))
    f.eventCheck should equal(eventList)
  }
}