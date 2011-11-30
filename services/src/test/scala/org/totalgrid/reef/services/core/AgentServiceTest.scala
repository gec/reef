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
import org.totalgrid.reef.proto.Auth.{ PermissionSet, Agent }

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.exceptions.BadRequestException
import org.totalgrid.reef.models.ApplicationSchema

import org.totalgrid.reef.services.core.SyncServiceShims._

@RunWith(classOf[JUnitRunner])
class AgentServiceTest extends AuthSystemTestBase {

  def makeAgent(name: String = "Nate", password: Option[String] = Some("password"), permissionSetNames: List[String] = List("all")) = {
    val b = Agent.newBuilder.setName(name)
    password.foreach(p => b.setPassword(p))
    permissionSetNames.foreach(n => b.addPermissionSets(PermissionSet.newBuilder.setName(n)))
    b.build
  }

  test("Agent Creation needs name and password") {
    val fix = new Fixture

    val goodRequest = makeAgent()

    intercept[BadRequestException] {
      fix.agentService.put(goodRequest.toBuilder.clearName.build)
    }
    intercept[BadRequestException] {
      fix.agentService.put(goodRequest.toBuilder.clearPassword.build)
    }
    intercept[BadRequestException] {
      fix.agentService.put(goodRequest.toBuilder.clearPermissionSets.build)
    }
  }

  test("Agent Creation needs valid permission sets") {
    val fix = new Fixture

    intercept[BadRequestException] {
      fix.agentService.put(makeAgent(permissionSetNames = List("*")))
    }

    intercept[BadRequestException] {
      fix.agentService.put(makeAgent(permissionSetNames = List("unknown")))
    }
  }

  test("Create, Update, Delete Agent") {
    val fix = new Fixture

    intercept[BadRequestException] {
      fix.login("Nate", "password")
    }

    val agent = fix.agentService.put(makeAgent()).expectOne()

    fix.login("Nate", "password")

    val deleted = fix.agentService.delete(agent).expectOne()

    intercept[BadRequestException] {
      fix.login("Nate", "password")
    }
  }

  test("Update Agent Password") {
    val fix = new Fixture

    val agent1 = fix.agentService.put(makeAgent()).expectOne()

    fix.login("Nate", "password")

    val agent2 = fix.agentService.put(makeAgent(password = Some("newPassword"))).expectOne()

    intercept[BadRequestException] {
      fix.login("Nate", "password")
    }

    fix.login("Nate", "newPassword")
  }

  test("Update Agent Permissions") {
    val fix = new Fixture

    val agent1 = fix.agentService.put(makeAgent(permissionSetNames = List("read_only"))).expectOne()
    agent1.getPermissionSets(0).getName should equal("read_only")

    val agent2 = fix.agentService.put(makeAgent(permissionSetNames = List("all"))).expectOne()
    agent2.getPermissionSets(0).getName should equal("all")

    val agent3 = fix.agentService.put(makeAgent(permissionSetNames = List("all", "read_only"))).expectOne()
    agent3.getPermissionSetsList.toList.map { _.getName }.sorted should equal(List("all", "read_only"))
  }

  test("Cannot update both password and permissions") {
    val fix = new Fixture
    fix.agentService.put(makeAgent()).expectOne()

    intercept[BadRequestException] {
      fix.agentService.put(makeAgent(password = Some("newPassword"), permissionSetNames = List("read_only"))).expectOne()
    }
  }

  test("Deleting Agent invalidates AuthTokens") {
    val fix = new Fixture

    fix.agentService.put(makeAgent()).expectOne()

    // each login generates an auth token
    fix.login("Nate", "password")
    fix.login("Nate", "password")

    import org.squeryl.PrimitiveTypeMode._

    ApplicationSchema.authTokens.size should equal(2)

    fix.agentService.delete(makeAgent()).expectOne()

    ApplicationSchema.authTokens.size should equal(0)

  }
}