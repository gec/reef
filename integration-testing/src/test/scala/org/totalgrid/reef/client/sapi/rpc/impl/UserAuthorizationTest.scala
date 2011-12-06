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
package org.totalgrid.reef.client.sapi.request

import org.scalatest.matchers.ShouldMatchers
import scala.collection.JavaConversions._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.exception.UnauthorizedException
import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

@RunWith(classOf[JUnitRunner])
class UserAuthorizationTest
    extends ClientSessionSuite("UserAuthorization.xml", "UserAuthorization",
      <div>
        <p>
          Users can have different permissions and abilities in the system.
        </p>
      </div>)
    with ShouldMatchers {

  private def asGuestUser(name: String, password: String, permission: String = "read_only")(fun: (Client, AllScadaService) => Unit) = {
    val agent = client.createNewAgent(name, password, List(permission)).await
    try {
      val guestClient = session.login(name, password).await
      fun(guestClient, guestClient.getRpcInterface(classOf[AllScadaService]))
      guestClient.logout().await
    } finally {
      client.deleteAgent(agent).await
    }
  }

  test("Double logout doesn't throw") {
    asGuestUser("test-agent", "test-password") { (guestClient, guestServices) =>
      guestClient.logout().await

      guestClient.logout().await
    }
  }

  test("Fail after logout ") {
    asGuestUser("test-agent", "test-password") { (guestClient, guestServices) =>
      guestClient.logout().await

      intercept[UnauthorizedException] {
        guestServices.getAgents().await
      }
    }
  }

  test("Can Read but not execute") {

    asGuestUser("test-agent", "test-password") { (guestClient, guestServices) =>
      // make sure we can see the commands in the system
      val commands = guestServices.getCommands().await

      // but can't create locks or executions
      intercept[UnauthorizedException] {
        guestServices.createCommandExecutionLock(commands.head :: Nil).await
      }
    }

  }

  test("Can update own password") {
    val userName = "test-agent3"
    val password = "test-password3"
    asGuestUser(userName, password) { (guestClient, guestServices) =>

      val agent = guestServices.getAgentByName(userName).await

      // change password
      guestServices.setAgentPassword(agent, password + password).await

      // get a new auth token with new password
      val newClient = guestClient.login(userName, password + password).await

      // change password back
      newClient.getRpcInterface(classOf[AllScadaService]).setAgentPassword(agent, password).await

      newClient.logout().await

      // show that we can't create a new agent
      val permissionSets = guestServices.getPermissionSets.await
      intercept[UnauthorizedException] {
        guestServices.createNewAgent("newUser", "newUser", permissionSets.map { _.getName }).await
      }

      // or delete one
      intercept[UnauthorizedException] {
        guestServices.deleteAgent(agent).await
      }
    }
  }

  ignore("Can't update others passwords") {
    // need attributes for agent updates
    asGuestUser("test-agent4", "test-password4") { (guestClient, guestServices) =>
      asGuestUser("test-agent2", "test-password2") { (_, _) =>
        val agent = guestServices.getAgentByName("test-agent2").await
        intercept[UnauthorizedException] {
          guestServices.setAgentPassword(agent, "changed-password2").await
        }
      }
    }
  }

}
