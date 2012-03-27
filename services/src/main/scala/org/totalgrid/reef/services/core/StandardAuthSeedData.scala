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

import org.totalgrid.reef.services.framework.RequestContext
import org.totalgrid.reef.client.service.proto.Auth.{ PermissionSet => RoleProto, Permission, EntitySelector }
import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.models.{ PermissionSet, AgentPermissionSetJoin }

/**
 * static seed function to bootstrap users + permissions into the system
 * TODO: remove static user seed data
 */
object StandardAuthSeedData {

  def seed(context: RequestContext, systemPassword: String) = {

    val entityModel = new EntityServiceModel
    val agentModel = new AgentServiceModel

    val system = ApplicationSchema.agents.insert(agentModel.createAgentWithPassword(context, "system", systemPassword))

    val allSelector = EntitySelector.newBuilder.setStyle("*").build

    val all = Permission.newBuilder.setAllow(true).addVerb("*").addResource("*").addSelector(allSelector).build
    val readOnly = Permission.newBuilder.setAllow(true).addVerb("read").addResource("*").addSelector(allSelector).build

    val selfAgent = EntitySelector.newBuilder.setStyle("self").build
    val updatePassword = Permission.newBuilder.setAllow(true).addVerb("update").addResource("agent_password").addSelector(selfAgent).build

    val allRole = RoleProto.newBuilder.setName("all").addPermissions(all)
    val guestRole = RoleProto.newBuilder.setName("read_only").addPermissions(readOnly).addPermissions(updatePassword)

    val defaultExpirationTime = 18144000000L // one month

    def addPermissionSet(proto: RoleProto.Builder) = {
      proto.setDefaultExpirationTime(defaultExpirationTime)
      val entity = entityModel.findOrCreate(context, proto.getName, "PermissionSet" :: Nil, None)
      val permissionSet = new PermissionSet(entity.id, proto.build.toByteArray)
      ApplicationSchema.permissionSets.insert(permissionSet)
    }

    val allSet = addPermissionSet(allRole)
    val readOnlySet = addPermissionSet(guestRole)

    ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(allSet.id, system.id))

    (allSet, readOnlySet)
  }

}