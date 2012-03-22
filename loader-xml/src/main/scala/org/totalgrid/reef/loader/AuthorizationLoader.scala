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
package org.totalgrid.reef.loader

import authorization._
import com.weiglewilczek.slf4s.Logging
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Auth.{ EntitySelector, Permission, PermissionSet, Agent => ProtoAgent }

class AuthorizationLoader(modelLoader: ModelLoader, exceptionCollector: ExceptionCollector)
    extends Logging {

  import AuthorizationLoader._

  def load(auth: Authorization) {

    val roles = mapRoles(auth)

    roles.foreach(modelLoader.putOrThrow(_))

    val agents = mapAgents(auth)

    agents.foreach(modelLoader.putOrThrow(_))
  }
}

object AuthorizationLoader {
  def mapPermission(where: String, allow: Boolean, access: Access): Permission = {
    val b = Permission.newBuilder
      .setAllow(allow)

    if (access.isSetActions) {
      val actions = access.getActions.split(' ').toList

      val validActions = List("*", "create", "read", "update", "delete")
      val invalidActions = actions.diff(validActions)
      if (!invalidActions.isEmpty) {
        throw new LoadingException("Invalid actions: " + invalidActions + " should be one of: " + validActions)
      }

      actions.foreach(b.addVerb(_))
    } else {
      throw new LoadingException("Must set actions in permission: " + where)
    }

    if (access.isSetResources) {
      val resources = access.getResources.split(' ')
      resources.foreach(b.addResource(_))
    } else {
      throw new LoadingException("Must set resources in permission: " + where)
    }

    if (access.isSetSelectStyle) {
      val sb = EntitySelector.newBuilder()

      val validStyles = List("*", "self", "type", "parent")

      val style = access.getSelectStyle
      if (validStyles.find(_ == style).isEmpty) {
        throw new LoadingException("Invalid selectorStyle: " + style + " should be one of: " + validStyles)
      }

      sb.setStyle(style)
      if (access.isSetSelectArguments) {
        val arguments = access.getSelectArguments.split(' ').toList
        sb.addAllArguments(arguments)
      }

      b.addSelector(sb)
    } else {
      if (access.isSetSelectArguments) throw new LoadingException("Cannot set selectArguments without selectType")
    }

    b.build
  }

  def mapRole(role: Role): PermissionSet = {
    val name = role.getName

    val b = PermissionSet.newBuilder
      .setName(name)

    val perms = role.getAllowOrDeny.map {
      case a: Allow => mapPermission(name, true, a)
      case d: Deny => mapPermission(name, false, d)
    }

    perms.foreach(b.addPermissions(_))

    b.build
  }

  def mapRoles(auth: Authorization): List[PermissionSet] = {
    if (auth.isSetRoles) {
      val rolesObject = auth.getRoles
      val rolesList = rolesObject.getRole.toList

      rolesList.map(mapRole(_))
    } else {
      Nil
    }
  }

  def mapAgent(agent: Agent): ProtoAgent = {
    val name = agent.getName
    val roles = agent.getRoles.split(' ')

    val b = ProtoAgent.newBuilder
      .setName(name)
      .setPassword(name)

    roles.map(n => PermissionSet.newBuilder().setName(n).build()).foreach(b.addPermissionSets(_))

    b.build
  }

  def mapAgents(auth: Authorization): List[ProtoAgent] = {
    if (auth.isSetAgents) {
      auth.getAgents.getAgent.toList.map(mapAgent(_))
    } else {
      Nil
    }
  }
}

