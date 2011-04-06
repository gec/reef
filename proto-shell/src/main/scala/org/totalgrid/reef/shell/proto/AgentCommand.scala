/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }
import java.io.{ InputStreamReader, BufferedReader }
import org.totalgrid.reef.api.request.AgentService
import org.totalgrid.reef.shell.proto.presentation.AgentView

import scala.collection.JavaConversions._

abstract class AgentCommandBase extends ReefCommandSupport {
  @Argument(index = 0, name = "agent name", description = "agent name", required = true, multiValued = false)
  var agentName: String = null

  lazy val authService: AgentService = services
}

@Command(scope = "agents", name = "password", description = "Sets the password for an agent")
class AgentSetPasswordCommand extends AgentCommandBase {

  def doCommand() = {
    val stdin = new BufferedReader(new InputStreamReader(System.in))
    System.out.println("   New Password: ")
    val newPassword = stdin.readLine.trim

    System.out.println("Repeat Password: ")
    val repeatedPassword = stdin.readLine.trim

    if (repeatedPassword != newPassword) {
      System.out.println("Passwords didn't match")
    } else {
      val agent = authService.getAgent(agentName)

      authService.setAgentPassword(agent, newPassword)
      System.out.println("Updated password for agent: " + agentName)
    }
  }
}

@Command(scope = "agents", name = "list", description = "View agents on system")
class AgentListCommand extends AgentCommandBase {

  def doCommand() = {

    val agents = authService.getAgents()

    AgentView.printAgents(agents.toList)
  }
}

@Command(scope = "agents", name = "permissions", description = "View permission sets")
class AgentPermissionsCommand extends AgentCommandBase {

  def doCommand() = {

    val permissions = authService.getPermissionSets()

    AgentView.printPermissionSets(permissions.toList)
  }
}

@Command(scope = "agents", name = "create", description = "Create a new agent on system")
class AgentCreateCommand extends AgentCommandBase {

  @GogoOption(name = "-s", description = "Names of wanted permissionSets, must include at least one", required = true, multiValued = true)
  var permissionSets: java.util.List[String] = null

  def doCommand() = {

    val stdin = new BufferedReader(new InputStreamReader(System.in))
    System.out.println("   New Password: ")
    val newPassword = stdin.readLine.trim

    System.out.println("Repeat Password: ")
    val repeatedPassword = stdin.readLine.trim

    val agent = authService.createNewAgent(agentName, repeatedPassword, permissionSets)

    AgentView.printAgents(agent :: Nil)
  }
}

@Command(scope = "agents", name = "delete", description = "Delete an agent from the system")
class AgentDeleteCommand extends AgentCommandBase {

  def doCommand() = {

    val agent = authService.getAgent(agentName)
    val deletedAgent = authService.deleteAgent(agent)

    AgentView.printAgents(deletedAgent :: Nil)
  }
}
