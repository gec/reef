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
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }
import java.util.UUID
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.totalgrid.reef.loader.commons.LoaderServices

@RunWith(classOf[JUnitRunner])
class EntityRequestTest extends ServiceClientSuite {

  test("Simple gets") {

    val allEntities = client.getEntities().await
    val targetUuid = allEntities.head.getUuid

    client.getEntityByUuid(targetUuid).await should equal(allEntities.head)
  }

  test("Get by types") {

    val allAgents = client.getAgents().await
    val permissions = client.getPermissionSets().await

    val entities = client.getEntitiesWithTypes(List("Agent", "PermissionSet")).await

    // TODO: make sure agents and permission sets get an entity
    // entities.size should equal(allAgents.size + permissions.size)
  }

  test("Put with UUID") {

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])

    loaderServices.findEntityByName("MagicTestObject").await.foreach { e =>
      loaderServices.delete(e).await
    }

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build

    val created = loaderServices.addEquipment(upload).await

    created.getUuid.getValue.toString should equal(uuid)
  }
}