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
import org.totalgrid.reef.japi.UnauthorizedException

import scala.collection.JavaConversions._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class UserAuthorizationTest
    extends ClientSessionSuite("UserAuthorization.xml", "UserAuthorization",
      <div>
        <p>
          Users can have different permissions and abilities in the system.
        </p>
      </div>)
    with ShouldMatchers {

  override val username = "guest"
  override val password = "guest"

  test("Can Read but not execute") {
    // make sure we can see the commands in the system
    val commands = client.getCommands.toList

    // but can't create locks or executions
    intercept[UnauthorizedException] {
      client.createCommandExecutionLock(commands.head :: Nil)
    }
  }

  test("Can update own password") {
    val agent = client.getAgent(username)

    // change password
    client.setAgentPassword(agent, password + password)

    // get a new auth token with new password
    client.createNewAuthorizationToken(username, password + password)

    // change password back
    client.setAgentPassword(agent, password)

    // show that we can't create a new agent
    val permissionSets = client.getPermissionSets.toList
    intercept[UnauthorizedException] {
      client.createNewAgent("newUser", "newUser", permissionSets.map { _.getName })
    }

    // or delete one
    intercept[UnauthorizedException] {
      client.deleteAgent(agent)
    }
  }

  ignore("Can't update others passwords") {
    // need attributes for agent updates

    val agent = client.getAgent("operator")
    intercept[UnauthorizedException] {
      client.setAgentPassword(agent, password)
    }
  }

}