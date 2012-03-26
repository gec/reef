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
import org.totalgrid.reef.authz.{ Permission, WildcardMatcher, Denied }

@RunWith(classOf[JUnitRunner])
class AuthFilterServiceTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {

  class Fixture extends SubscriptionTools.SubscriptionTesting {
    def _dbConnection = dbConnection

    val serv = new SyncService(new AuthFilterService, contextSource)
  }

  def buildReq(res: String, action: String, ents: List[Entity]): AuthFilter = {
    val b = AuthFilterRequest.newBuilder().setResource(res).setAction(action)
    ents.foreach(b.addEntity(_))

    AuthFilter.newBuilder().setRequest(b).build()
  }

  test("Basic") {
    val f = new Fixture

    val req = buildReq("entity", "read", List(Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*").build).build))

    val resp = Denied(new Permission(true, List("testR"), List("TestA"), new WildcardMatcher))
    f.filterResponses.enqueue(List(resp))

    println(f.serv.post(req))
  }

}
