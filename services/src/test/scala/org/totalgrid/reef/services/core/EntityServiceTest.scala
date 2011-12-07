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

import org.totalgrid.reef.models.DatabaseUsingTestBase
import java.util.UUID
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }

import SyncServiceShims._
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.Envelope.Status

class EntityServiceTest extends DatabaseUsingTestBase {

  val service = new EntityService()

  test("Put Entity with predetermined UUID") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build

    val created = service.put(upload).expectOne

    created.getUuid.getValue.toString should equal(uuid)
  }

  test("Put two entities with same uuids") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build
    val upload2 = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject2").addTypes("TestType").build

    service.put(upload).expectOne

    intercept[ReefServiceException] {
      service.put(upload2).expectOne
    }
  }

  test("Put Entity with 2 types") {

    val upload1 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType1").addTypes("TestType3").build
    val upload2 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType4").addTypes("TestType2").build

    service.put(upload1).expectOne(Status.CREATED)
    service.put(upload2).expectOne(Status.UPDATED)

    service.put(upload1).expectOne(Status.NOT_MODIFIED)
    service.put(upload2).expectOne(Status.NOT_MODIFIED)
  }
}