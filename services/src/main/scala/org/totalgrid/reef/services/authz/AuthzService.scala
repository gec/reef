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
import org.totalgrid.reef.models.ApplicationSchema
import org.totalgrid.reef.client.exception.UnauthorizedException
import com.weiglewilczek.slf4s.Logging

import java.util.UUID

trait AuthzService {

  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]): Unit

  // load up the permissions sets
  def prepare(context: RequestContext)
}

class NullAuthzService extends AuthzService {
  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {}
  def prepare(context: RequestContext) {}
}

class SqlAuthzService(filteringService: AuthzFilteringService) extends AuthzService with Logging {

  def this() = this(AuthzFilter)

  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {

    val permissions = context.get[List[Permission]]("permissions")
      .getOrElse(throw new UnauthorizedException(context.get[String]("auth_error").get))

    // just pass in a single boolean value, if it gets filtered we know we are not auhorized
    val filtered = filteringService.filter(permissions, componentId, action, List(true), List(uuids))

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

    if (authTokens.isEmpty) context.set("auth_error", "No auth tokens in envelope header")
    else {
      // lookup the tokens that are not expired
      val now = System.currentTimeMillis

      val tokens = ApplicationSchema.authTokens.where(t => t.token in authTokens and t.expirationTime.~ > now).toList
      if (tokens.isEmpty) context.set("auth_error", "All tokens unknown or expired")
      else {

        val permissionSets = tokens.map { _.permissionSets.value.toList }.flatten
        val permissions = permissionSets.map { ps => ps.proto }

        val agent = tokens.head.agent.value

        context.set("agent", agent)

        val agentName = agent.entityName

        val convertedPermissions = permissions.map { Permission.fromProto(_, agentName) }.flatten
        context.set("permissions", convertedPermissions)
      }
    }
  }

}