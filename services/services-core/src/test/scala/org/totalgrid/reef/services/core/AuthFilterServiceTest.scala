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
import org.totalgrid.reef.client.service.proto.Auth.{ AuthFilterRequest, AuthFilter }
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity }
import java.util.UUID
import org.totalgrid.reef.services.core.SubscriptionTools.FilterRequest
import org.totalgrid.reef.authz.{ Allowed, Permission, WildcardMatcher, Denied }
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.client.Expectations._

@RunWith(classOf[JUnitRunner])
class AuthFilterServiceTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {

  class Fixture extends SubscriptionTools.SubscriptionTesting {
    def _dbConnection = dbConnection

    contextSource.enableFilter()

    val modelFac = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))
    val entServ = new SyncService(new EntityService(modelFac.entities), contextSource)

    val entA = entServ.put(Entity.newBuilder().setName("EntA").addTypes("TypeA").build).expectOne()
    val entB = entServ.put(Entity.newBuilder().setName("EntB").addTypes("TypeB").build).expectOne()

    val serv = new SyncService(new AuthFilterService, contextSource)
  }

  def buildReq(res: String, action: String, ents: List[Entity]): AuthFilter = {
    val b = AuthFilterRequest.newBuilder().setResource(res).setAction(action)
    ents.foreach(b.addEntity(_))

    AuthFilter.newBuilder().setRequest(b).build()
  }

  import org.totalgrid.reef.models.{ Entity => EntityModel, ApplicationSchema }
  def entSet: (List[EntityModel], List[UUID]) = {
    import org.squeryl.PrimitiveTypeMode._

    val ents = ApplicationSchema.entities.where(t => true === true).toList

    (ents, ents.map(_.id))
  }

  test("Inputs/outputs") {
    val f = new Fixture

    val req = buildReq("entity", "read", List(Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*").build).build))

    val (ents, uuids) = entSet

    val responses = List(Allowed(ents(0), new Permission(true, List("entity_fake"), List("read_fake"), new WildcardMatcher)),
      Denied(ents(1), new Permission(true, List("entity_fake"), List("read_fake"), new WildcardMatcher)),
      Allowed(ents(2), new Permission(true, List("entity_fake"), List("read_fake"), new WildcardMatcher)))

    f.filterResponses.enqueue(responses)

    val results = f.serv.post(req).expectOne()

    results.hasRequest should equal(false)
    val resultList = results.getResultsList.toList
    resultList.size should equal(3)
    resultList(0).getAllowed should equal(true)
    resultList(0).getEntity.getName should equal(responses(0).result.name)
    resultList(1).getAllowed should equal(false)
    resultList(1).getEntity.getName should equal(responses(1).result.name)
    resultList(2).getAllowed should equal(true)
    resultList(2).getEntity.getName should equal(responses(2).result.name)

    val requests = f.filterRequests.toList.asInstanceOf[List[FilterRequest[EntityModel]]]
    requests.size should equal(1)

    val head = requests.head
    head.action should equal("read")
    head.componentId should equal("entity")
    head.payload.toSet should equal(ents.toSet)
    head.uuids.size should equal(3)
    head.uuids.toSet should equal(uuids.map(List(_)).toSet)

  }

}
