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
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }
import java.io.{ InputStreamReader, BufferedReader }
import org.totalgrid.reef.shell.proto.presentation.AgentView

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Auth.Permission
import org.totalgrid.reef.client.service.AgentService

abstract class AgentCommandBase extends ReefCommandSupport {
  lazy val authService: AgentService = services

  def getRepeatedPassword(): String = {
    val stdin = new BufferedReader(new InputStreamReader(System.in))
    System.out.println("   New Password: ")
    val newPassword = stdin.readLine.trim

    System.out.println("Repeat Password: ")
    val repeatedPassword = stdin.readLine.trim

    if (repeatedPassword != newPassword) {
      throw new Exception("Passwords didn't match")
    }

    repeatedPassword
  }
}

abstract class SingleAgentCommandBase extends AgentCommandBase {
  @Argument(index = 0, name = "agent name", description = "agent name", required = true, multiValued = false)
  var agentName: String = null
}

@Command(scope = "agent", name = "password", description = "Sets the password for an agent")
class AgentSetPasswordCommand extends SingleAgentCommandBase {

  def doCommand() = {

    val newPassword = getRepeatedPassword()
    val agent = authService.getAgentByName(agentName)

    authService.setAgentPassword(agent, newPassword)
    System.out.println("Updated password for agent: " + agentName)
  }
}

@Command(scope = "agent", name = "list", description = "View agents on system")
class AgentListCommand extends AgentCommandBase {

  def doCommand() = {

    val agents = authService.getAgents()

    AgentView.printAgents(agents.toList)
  }
}

@Command(scope = "agent-permissions", name = "list", description = "View permission sets")
class AgentPermissionsListCommand extends AgentCommandBase {

  def doCommand() = {

    val permissions = authService.getPermissionSets()

    AgentView.printPermissionSets(permissions.toList)
  }
}

@Command(scope = "agent-permissions", name = "create", description = "Edit a permission set")
class AgentPermissionsCreateCommand extends AgentCommandBase {

  @Argument(index = 0, name = "permissionSetName", description = "Descriptive name for a permission", required = true, multiValued = false)
  var permissionSetName: String = null

  @GogoOption(name = "-a", description = "Allowed Permisson qualifiers of form [OPERATION],[RESOURCE]. Ex: Read,measurements", required = false, multiValued = true)
  var allowed: java.util.List[String] = null

  @GogoOption(name = "-d", description = "Denied Permisson qualifiers of form [OPERATION],[RESOURCE]. Ex: Read,measurements", required = false, multiValued = true)
  var denied: java.util.List[String] = null

  def doCommand() = {

    val permissions = createPermissions(true, allowed) ::: createPermissions(false, denied)

    if (permissions.size == 0) throw new Exception("Must specify atleast 1 allow or 1 deny permission")

    val permissionSet = authService.createPermissionSet(permissionSetName, permissions)

    AgentView.printPermissionSets(permissionSet :: Nil)
  }

  private def createPermissions(allow: Boolean, qualifiers: java.util.List[String]) = {
    Option(qualifiers).map { _.toList }.getOrElse(Nil).map { qualifier =>
      val parts = qualifier.split(',')
      if (parts.size != 2) throw new Exception("Qualifer should be of form \"[OPERATION],[RESOURCE]\". " + qualifier + " not valid.")
      Permission.newBuilder.setAllow(allow).setVerb(parts(0)).setResource(parts(1)).build
    }
  }
}

@Command(scope = "agent-permissions", name = "delete", description = "Delete a PermissionSet")
class AgentPermissionsDeleteCommand extends AgentCommandBase {

  @Argument(index = 0, name = "permissionSetName", description = "Descriptive name for a permission", required = true, multiValued = false)
  var permissionSetName: String = null

  def doCommand() = {

    val permissionSet = authService.getPermissionSet(permissionSetName)

    val deletedPermissionSet = authService.deletePermissionSet(permissionSet)

    AgentView.printPermissionSets(deletedPermissionSet :: Nil)
  }
}

@Command(scope = "agent", name = "create", description = "Create a new agent on system")
class AgentCreateCommand extends SingleAgentCommandBase {

  @GogoOption(name = "-s", description = "Names of wanted permissionSets, must include at least one", required = true, multiValued = true)
  var permissionSets: java.util.List[String] = null

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private var password: String = null

  def doCommand() = {

    val newPassword = Option(password) match {
      case None => getRepeatedPassword()
      case Some(pw) =>
        System.out.println("WARNING: Password will be visible in karaf command history!")
        pw
    }

    val agent = authService.createNewAgent(agentName, newPassword, permissionSets)

    AgentView.printAgents(agent :: Nil)
  }
}

@Command(scope = "agent", name = "delete", description = "Delete an agent from the system")
class AgentDeleteCommand extends SingleAgentCommandBase {

  def doCommand() = {

    val agent = authService.getAgentByName(agentName)
    val deletedAgent = authService.deleteAgent(agent)

    AgentView.printAgents(deletedAgent :: Nil)
  }
}
