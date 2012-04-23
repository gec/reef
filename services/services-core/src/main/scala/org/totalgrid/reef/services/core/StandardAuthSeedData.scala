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

import org.totalgrid.reef.services.framework.{ RequestContextSource, RequestContext }
import org.totalgrid.reef.client.service.proto.Auth.{ Agent, PermissionSet => RoleProto, Permission, EntitySelector }

/**
 * static seed function to bootstrap users + permissions into the system
 */
object StandardAuthSeedData {

  /**
   * encapsulates the operations to create/update agents and roles. By default we don't update the password of existing
   * agents so this seeding can be run on a live system without overwriting the current passwords
   */
  class AuthSeeder(context: RequestContext, updatePasswords: Boolean, defaultExpirationTime: Long = 18144000000L) {
    private val source = new RequestContextSource {
      def transaction[A](f: (RequestContext) => A) = f(context)
    }

    private val agentService = new AgentService(new AgentServiceModel)
    private val permissionService = new PermissionSetService(new PermissionSetServiceModel)

    def makeSelector(style: String, arguments: List[String] = Nil) = {
      val b = EntitySelector.newBuilder.setStyle(style)
      arguments.foreach(b.addArguments(_))
      b.build
    }

    def makePermission(allow: Boolean, verbs: List[String] = List("*"), resources: List[String] = List("*"), selectors: List[EntitySelector] = Nil) = {
      val b = Permission.newBuilder.setAllow(allow)
      verbs.foreach(b.addVerb(_))
      resources.foreach(b.addResource(_))
      if (selectors.isEmpty) b.addSelector(makeSelector("*"))
      else selectors.foreach(b.addSelector(_))
      b.build
    }

    def addRole(name: String, permissions: List[Permission], expirationTime: Long = defaultExpirationTime): RoleProto = {
      val b = RoleProto.newBuilder.setName(name)
      b.setDefaultExpirationTime(expirationTime)
      permissions.foreach(b.addPermissions(_))
      permissionService.put(source, b.build).expectOne()
    }

    def addUser(userName: String, defaultPassword: String, role: String): Agent = addUser(userName, defaultPassword, List(role))
    def addUser(userName: String, defaultPassword: String, roles: List[String]): Agent = {

      val b = Agent.newBuilder.setName(userName)

      val existingAgent = agentService.get(source, Agent.newBuilder.setName(userName).build).expectMany().headOption
      if (updatePasswords || existingAgent.isEmpty) {
        b.setPassword(defaultPassword)
      }

      roles.foreach(r => b.addPermissionSets(RoleProto.newBuilder.setName(r).build))
      agentService.put(source, b.build).expectOne()
    }
  }

  def seed(context: RequestContext, systemPassword: String) {

    // we don't want to update the passwords of any already existing agents
    val seeder = new AuthSeeder(context, false)

    val all = seeder.makePermission(true)
    val readOnly = seeder.makePermission(true, List("read"))

    val selfAgent = seeder.makeSelector("self")
    val updatePassword = seeder.makePermission(true, List("update"), List("agent_password"), List(selfAgent))
    val readSelfAgent = seeder.makePermission(true, List("read"), List("agent", "auth_token", "entity"), List(selfAgent))

    val readAndDeleteOwnTokens = seeder.makePermission(true, List("read", "delete"), List("auth_token"), List(selfAgent))
    val denyAuthTokens = seeder.makePermission(false, List("read", "delete"), List("auth_token"))

    val readOnlyRole = seeder.addRole("read_only", List(readAndDeleteOwnTokens, denyAuthTokens, readOnly))

    val userAdminPermission = seeder.makePermission(true, List("*"), List("agent", "agent permission_set", "agent_password", "agent_permissions"))
    val userAdminRole = seeder.addRole("user_setup", List(userAdminPermission))

    val appCreate = seeder.makePermission(true, List("create"), List("application_config"))
    val appUpdate = seeder.makePermission(true, List("update", "delete", "read"), List("application_config"), List(selfAgent))
    val statusUpdate = seeder.makePermission(true, List("update", "read"), List("status_snapshot"), List(selfAgent))
    val applicationRole = seeder.addRole("application", List(appCreate, appUpdate, statusUpdate))

    val userRole = seeder.addRole("user_role", List(updatePassword, readSelfAgent, readAndDeleteOwnTokens))

    val protocolAdapter = seeder.makePermission(true, List("read", "update"), List("endpoint_connection", "endpoint_state"))
    val fep = seeder.makePermission(true, List("create", "update"), List("front_end_processor"))
    val protocolRole = seeder.addRole("protocol_adapter", List(protocolAdapter, fep))

    val enableEndpoints = seeder.makePermission(true, List("read", "update"), List("endpoint_connection", "endpoint_enabled"))
    val commsEngineer = seeder.addRole("comms_engineer", List(enableEndpoints))

    val systemViewerRole = seeder.addRole("system_viewer", List(readOnly))

    val ownCommandUpdate = seeder.makePermission(true, List("update", "delete"), List("command_lock"), List(selfAgent))
    val commandIssuer = seeder.addRole("command_issuer", List(ownCommandUpdate))

    val commandCreator = seeder.makePermission(true, List("create"), List("command_lock", "command_lock_select", "command_lock_block", "user_command_request"), List(selfAgent))
    val allCommands = seeder.addRole("command_creator", List(commandCreator))

    val allRole = seeder.addRole("all", List(all))
    val passwordChangingRole = seeder.addRole("password_updatable", List(updatePassword))

    val standardRoles = List("user_role", "system_viewer")

    seeder.addUser("system", systemPassword, List("all"))
    seeder.addUser("admin", systemPassword, List("all"))
    seeder.addUser("user_admin", systemPassword, List("user_setup", "user"))
    seeder.addUser("operator", systemPassword, "command_creater" :: "command_issuer" :: standardRoles)
    seeder.addUser("services", systemPassword, List("all"))
    seeder.addUser("scada", systemPassword, "comms_engineer" :: standardRoles)
    seeder.addUser("core_application", systemPassword, List("all"))
    seeder.addUser("remote_application", systemPassword, List("application"))
    seeder.addUser("fep_application", systemPassword, List("application", "protocol_adapter"))
    seeder.addUser("master_protocol_adapter", systemPassword, "application" :: "protocol_adapter" :: standardRoles)
    seeder.addUser("slave_protocol_adapter", systemPassword, "application" :: "protocol_adapter" :: standardRoles)
    seeder.addUser("hmi_app", systemPassword, "application" :: standardRoles)

    seeder.addUser("guest", systemPassword, List("read_only"))
    seeder.addUser("user", systemPassword, List("read_only", "user_role"))
  }

}