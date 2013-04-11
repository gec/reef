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
import org.totalgrid.reef.client.exception.UnauthorizedException
import com.typesafe.scalalogging.slf4j.Logging

import java.util.UUID
import org.totalgrid.reef.models.{ Agent, ApplicationSchema }
import org.totalgrid.reef.client.service.proto.Auth.PermissionSet
import org.squeryl.Query

object AuthzService {
  def agent = "agent"
  def permissions = "permissions"
  def filterService = "filterService"
  def authError = "auth_error"
}
trait AuthzService {

  def filter[A](context: RequestContext, componentId: String, action: String, payload: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]]

  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]): Unit

  def visibilityMap(context: RequestContext): VisibilityMap

  // load up the permissions sets
  def prepare(context: RequestContext)
}

class NullAuthzService extends AuthzService {
  def filter[A](context: RequestContext, componentId: String, action: String, payload: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]] = {
    payload.map(Allowed(_, new Permission(true, List(), List(), new WildcardMatcher)))
  }
  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {}
  def visibilityMap(context: RequestContext) = VisibilityMap.empty
  def prepare(context: RequestContext) {}
}

object SqlAuthzService {
  import org.squeryl.PrimitiveTypeMode._

  case class AuthLookup(agent: Agent, permissionSets: List[PermissionSet])

  def lookupTokens(tokenList: List[String]): Option[AuthLookup] = {
    val now = System.currentTimeMillis

    import ApplicationSchema.{ authTokens, permissionSets, agents, tokenSetJoins }
    import org.totalgrid.reef.models.{ AuthToken => SqlToken, PermissionSet => SqlSet }

    val results: List[(SqlToken, Option[SqlSet])] =
      from(authTokens, permissionSets.leftOuter)((tok, set) =>
        where(tok.token in tokenList and tok.expirationTime.~ > now and
          (set.map(s => s.id) in from(tokenSetJoins)(j => where(j.authTokenId === tok.id) select (j.permissionSetId))))
          select (tok, set)).toList

    if (!results.isEmpty) {
      val agent = results.head._1.agent.value
      val permissions = results.flatMap(_._2.map(p => p.proto))
      Some(AuthLookup(agent, permissions))
    } else {
      None
    }
  }
}

class SqlAuthzService(filteringService: AuthzFilteringService) extends AuthzService with Logging {
  import SqlAuthzService._

  def this() = this(AuthzFilter)

  private def getContextPermissions(context: RequestContext) = {
    context.get[List[Permission]](AuthzService.permissions)
      .getOrElse(throw new UnauthorizedException(context.get[String](AuthzService.authError).getOrElse("Not logged in")))
  }

  def filter[A](context: RequestContext, componentId: String, action: String, payload: List[A], uuids: => List[List[UUID]]): List[FilteredResult[A]] = {

    val permissions = getContextPermissions(context)

    filteringService.filter(permissions, componentId, action, payload, uuids)
  }

  def authorize(context: RequestContext, componentId: String, action: String, uuids: => List[UUID]) {

    val permissions = getContextPermissions(context)

    // just pass in a single boolean value, if it gets filtered we know we are not auhorized
    val filtered = filteringService.filter(permissions, componentId, action, List(true), List(uuids))

    filtered.find(!_.isAllowed) match {
      case Some(filterResult) => throw new UnauthorizedException(filterResult.toString)
      case None =>
    }
  }

  def visibilityMap(context: RequestContext) = {
    // TODO: this is optional only to support bootstrap code
    val permissions = context.get[List[Permission]](AuthzService.permissions)
    permissions.map { filteringService.visibilityMap(_) }.getOrElse(VisibilityMap.empty)
  }

  def prepare(context: RequestContext) {
    // load the permissions by forcing an auth attempt
    loadPermissions(context)
    // TODO: evaluate different way to pass this to AuthFilterService
    context.set(AuthzService.filterService, filteringService)
  }

  private def loadPermissions(context: RequestContext) {

    if (!context.getHeaders.hasAuthToken) {
      context.set(AuthzService.authError, "No auth tokens in envelope header")
    } else {

      lookupTokens(List(context.getHeaders.getAuthToken)) match {
        case None => context.set(AuthzService.authError, "All tokens unknown or expired")
        case Some(AuthLookup(agent, permSets)) =>
          context.set(AuthzService.agent, agent)
          context.set(AuthzService.permissions, permSets.flatMap { Permission.fromProto(_, agent.entityName) })
      }
    }
  }

}