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
      val b = Permission.newBuilder.setAllow(true)
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

    val allRole = seeder.addRole("all", List(all))
    val readOnlyRole = seeder.addRole("read_only", List(readOnly))
    val passwordChangingRole = seeder.addRole("password_updatable", List(updatePassword))

    seeder.addUser("system", systemPassword, allRole.getName)
    seeder.addUser("admin", systemPassword, allRole.getName)
    seeder.addUser("operator", systemPassword, allRole.getName)
    seeder.addUser("services", systemPassword, allRole.getName)
    seeder.addUser("core_application", systemPassword, allRole.getName)
    seeder.addUser("remote_application", systemPassword, allRole.getName)
    seeder.addUser("master_protocol_adapter", systemPassword, allRole.getName)
    seeder.addUser("slave_protocol_adapter", systemPassword, allRole.getName)

    seeder.addUser("guest", systemPassword, readOnlyRole.getName)
    seeder.addUser("user", systemPassword, List(readOnlyRole.getName, passwordChangingRole.getName))
  }

}