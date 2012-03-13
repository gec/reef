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
package org.totalgrid.reef.services.authz

import org.totalgrid.reef.services.framework.RequestContext
import org.totalgrid.reef.authz._
import org.totalgrid.reef.models.{ ApplicationSchema, Entity, AuthPermission }
import org.totalgrid.reef.client.exception.{ InternalServiceException, UnauthorizedException }
import com.weiglewilczek.slf4s.Logging

trait AuthzService {

  def authorize(context: RequestContext, componentId: String, action: String, entities: => List[Entity]): Unit

  // load up the permissions sets
  def prepare(context: RequestContext)
}

class NullAuthzService extends AuthzService {
  def authorize(context: RequestContext, componentId: String, action: String, entities: => List[Entity]) {}
  def prepare(context: RequestContext) {}
}

class SqlAuthzService extends AuthzService with Logging {

  import AuthzFilteringService._

  def authorize(context: RequestContext, componentId: String, action: String, entities: => List[Entity]) {

    val permissions = context.get[List[Permission]]("permissions")
      .getOrElse(throw new UnauthorizedException(context.get[String]("auth_error").get))

    val convertedEntities = if (entities.isEmpty) {
      // HACK, just pass in a temporary entity for now so we have something to get filtered out
      List(new AuthEntity {
        def name = "SYSTEM"
        def types = Nil
      })
    } else {
      entities.map { toAuthEntity(_) }
    }
    logger.info(componentId + ":" + action + "  " + convertedEntities.map { _.name }.mkString("(", ",", ")"))

    val filtered = filter(permissions, componentId, action, convertedEntities, convertedEntities)

    filtered.find(!_.isAllowed) match {
      case Some(filterResult) => throw new UnauthorizedException(filterResult.toString)
      case None =>
    }
  }

  def prepare(context: RequestContext) = {
    // load the permissions by forcing an auth attempt
    loadPermissions(context)
  }

  def loadPermissions(context: RequestContext) {

    import org.squeryl.PrimitiveTypeMode._

    val authTokens = context.getHeaders.authTokens

    if (authTokens.size == 0) context.set("auth_error", "No auth tokens in envelope header")
    else {
      // lookup the tokens that are not expired
      val now = System.currentTimeMillis

      val tokens = ApplicationSchema.authTokens.where(t => t.token in authTokens and t.expirationTime.~ > now).toList
      if (tokens.size == 0) context.set("auth_error", "All tokens unknown or expired")
      else {

        val permissions = tokens.map(token => token.permissionSets.value.toList.map(ps => ps.permissions.value).flatten).flatten.distinct

        val userName = tokens.head.agent.value.entityName

        // loaded valid permissions, store them on the context
        context.modifyHeaders(_.setUserName(userName))

        val convertedPermissions = permissions.map { toAuthPermission(context, _) }

        context.set("permissions", convertedPermissions)
      }
    }
  }

  def toAuthPermission(context: RequestContext, permission: AuthPermission) = {

    // TODO: remove agent_password:update special casing
    val selectors = if (permission.resource == "agent_password" && permission.verb == "update") {
      List(new ResourceSet(List(new EntityHasName(context.getHeaders.userName.get))))
    } else List(new ResourceSet(List(new AllMatcher)))

    new Permission(permission.allow, permission.resource, permission.verb, selectors)
  }

  def toAuthEntity(entity: Entity) = {
    new AuthEntity {
      def name = entity.name

      def types = entity.types.value
    }
  }
}