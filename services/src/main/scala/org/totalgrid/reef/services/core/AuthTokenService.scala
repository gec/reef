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

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Auth._
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.service.proto.Descriptors

import scala.collection.JavaConversions._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.services.framework.SquerylModel._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.event.EventType
import org.totalgrid.reef.models.{ UUIDConversions, Agent, ApplicationSchema, AuthToken => AuthTokenModel, AuthTokenPermissionSetJoin }

/**
 * auth token specific code for searching the sql table and converting from
 */
trait AuthTokenConversions extends UniqueAndSearchQueryable[AuthToken, AuthTokenModel] {

  val table = ApplicationSchema.authTokens

  def sortResults(list: List[AuthToken]) = list.sortBy(_.getExpirationTime)

  def getRoutingKey(req: AuthToken) = ProtoRoutingKeys.generateRoutingKey {
    req.loginLocation :: req.agent.name :: Nil
  }

  def relatedEntities(entries: List[AuthTokenModel]) = {
    entries.map { _.agent.value.entityId }
  }

  def uniqueQuery(proto: AuthToken, sql: AuthTokenModel) = {
    List(
      proto.id.value.asParam(sql.id === _.toInt),
      proto.agent.map(agent => sql.agentId in AgentConversions.uniqueQueryForId(agent, { _.id })),
      proto.loginLocation.asParam(sql.loginLocation === _),
      proto.clientVersion.asParam(sql.clientVersion === _),
      proto.revoked.asParam(sql.revoked === _),
      proto.token.asParam(sql.token === _))
  }

  def searchQuery(proto: AuthToken, sql: AuthTokenModel) = {
    Nil
  }

  def isModified(existing: AuthTokenModel, updated: AuthTokenModel): Boolean = {
    existing.expirationTime != updated.expirationTime
  }

  def convertToProto(entry: AuthTokenModel): AuthToken = {
    val b = AuthToken.newBuilder
    b.setId(UUIDConversions.makeId(entry))
    b.setAgent(AgentConversions.convertToProto(entry.agent.value))
    b.setExpirationTime(entry.expirationTime)
    b.setIssueTime(entry.issueTime)
    b.setRevoked(entry.revoked)
    b.setLoginLocation(entry.loginLocation)
    b.setClientVersion(entry.clientVersion)
    entry.permissionSets.value.foreach(ps => b.addPermissionSets(PermissionSetConversions.convertToProto(ps)))
    if (entry.displayToken) b.setToken(entry.token)
    b.build
  }

}

class AuthTokenServiceModel
    extends SquerylServiceModel[Long, AuthToken, AuthTokenModel]
    with EventedServiceModel[AuthToken, AuthTokenModel]
    with AuthTokenConversions
    with ServiceModelSystemEventPublisher {

  override def createFromProto(context: RequestContext, authToken: AuthToken): AuthTokenModel = {
    logger.info("logging in agent: " + authToken.getAgent.getName)
    val currentTime: Long = System.currentTimeMillis // need one time for authToken DB entry and posted event

    val agentName: String = authToken.agent.name.getOrElse(postLoginException(context, "", Status.BAD_REQUEST, "Cannot login without setting agent name."))

    // check the password, PUNT: maybe replace this with a nonce + MD5 or something better
    val agentRecord: Option[Agent] = AgentConversions.findRecord(context, authToken.getAgent)
    agentRecord match {
      case None =>
        logger.info("unable to find agent: " + authToken.getAgent)
        postLoginException(context, agentName, Status.UNAUTHORIZED, "Invalid agent or password")

      case Some(agent) =>
        if (!agent.checkPassword(authToken.getAgent.getPassword)) {
          postLoginException(context, agentName, Status.UNAUTHORIZED, "Invalid agent or password")
        }
        context.set("agent", agent)
        processLogin(context, agent, authToken, currentTime)
    }
  }

  private def processLogin(context: RequestContext, agent: Agent, authToken: AuthToken, currentTime: Long): AuthTokenModel = {

    val availableSets = agent.permissionSets.value.toList // permissions we can have
    // permissions we are asking for allow the user to request either all of their permission sets or just a subset, barf if they
    // ask for permisions they dont have
    val permissionsRequested = authToken.getPermissionSetsList.toList
    val permissionSets = if (permissionsRequested.size == 1 && permissionsRequested(0).getName == "*") availableSets
    else {
      val setQuerySize = permissionsRequested.map(ps => PermissionSetConversions.searchQuerySize(ps)).sum
      if (setQuerySize > 0) {
        val askedForSets = permissionsRequested.map(ps => PermissionSetConversions.findRecords(context, ps)).flatten.distinct
        val unavailableSets = askedForSets.diff(availableSets)
        if (unavailableSets.size > 0) {
          postLoginException(context, agent.entityName, Status.UNAUTHORIZED, "No access to permission sets: " + unavailableSets)
        }
        askedForSets
      } else {
        availableSets
      }
    }

    // allow the user to set the expiration time explicitly or use the default from the most restrictive permissionset
    val expirationTime = if (authToken.hasExpirationTime) {
      val time = authToken.getExpirationTime
      if (time <= currentTime) {
        postLoginException(context, agent.entityName, Status.BAD_REQUEST, "Expiration time cannot be in the past")
      }
      time
    } else {
      // one month
      currentTime + 18144000000L
    }

    // For now, just warn if the client version is unknown
    val version = if (authToken.hasClientVersion) {
      authToken.getClientVersion
    } else {
      logger.warn("Client attempting to login with unknown version")
      "Unknown"
    }

    // TODO: generate an unguessable security token
    val token = java.util.UUID.randomUUID().toString
    val newAuthToken = table.insert(new AuthTokenModel(token, agent.id, authToken.getLoginLocation, version, false, currentTime, expirationTime))
    // link the token to all of the permisisonsSet they have checked out access to
    permissionSets.foreach(ps => ApplicationSchema.tokenSetJoins.insert(new AuthTokenPermissionSetJoin(ps.id, newAuthToken.id)))

    postSystemEvent(context, EventType.System.UserLogin, args = List("user" -> agent.entity.value.name))
    newAuthToken.displayToken = true
    newAuthToken
  }

  private def postLoginException[A](context: RequestContext, userName: String, status: Status, reason: String): A = {
    postSystemEvent(context, EventType.System.UserLoginFailure, args = "reason" -> reason :: Nil, userId = Some(userName))
    throw new BadRequestException(reason, status)
  }

  override def updateFromProto(context: RequestContext, req: AuthToken, existing: AuthTokenModel): (AuthTokenModel, Boolean) = {
    throw new Exception("cannot update auth tokens")
  }

  // we are faking the delete operation, we actually want to keep the row around forever as an audit log
  override def delete(context: RequestContext, entry: AuthTokenModel): AuthTokenModel = {

    entry.revoked = true
    entry.expirationTime = -1
    table.update(entry)

    postSystemEvent(context, EventType.System.UserLogout, userId = Some(entry.agent.value.entityName))

    onUpdated(context, entry)
    postDelete(context, entry)
    entry
  }

}

import ServiceBehaviors._

class AuthTokenService(protected val model: AuthTokenServiceModel)
    extends SyncModeledServiceBase[AuthToken, AuthTokenModel, AuthTokenServiceModel]
    with GetEnabled
    with PutOnlyCreates
    with DeleteEnabled {

  override val descriptor = Descriptors.authToken

  // we are overriding this to skip the auth step
  override def performCreate(context: RequestContext, model: ServiceModelType, request: ServiceType): ModelType = {
    model.createFromProto(context, request)
  }

  override protected def preRead(context: RequestContext, proto: ServiceType) = {
    if (!proto.hasAgent) {
      // no search terms means we should use self agent
      proto.toBuilder.setAgent(Agent.newBuilder.setName(context.agent.entityName)).build
    } else {
      proto
    }
  }

  override protected def performDelete(context: RequestContext, model: ServiceModelType, request: ServiceType) = {
    val (allButActive, proto) = if (!request.hasAgent && !request.hasToken && !request.hasId) {
      val updated = request.toBuilder.setAgent(Agent.newBuilder.setName(context.agent.entityName)).build
      (true, updated)
    } else {
      (false, request)
    }
    val existing = model.findRecords(context, proto)
    val filtered = if (allButActive) existing.filter(context.getHeaders.getAuthToken != _.token) else existing
    context.auth.authorize(context, componentId, "delete", model.relatedEntities(filtered))
    filtered.foreach(model.delete(context, _))
    filtered
  }
}
