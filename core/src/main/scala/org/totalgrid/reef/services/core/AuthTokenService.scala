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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.protoapi.ServiceException

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Auth._
import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Envelope.Status
import org.totalgrid.reef.services.core.util._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.models.{ ApplicationSchema, AuthToken => AuthTokenModel, AuthTokenPermissionSetJoin, Agent => AgentModel, PermissionSet => PermissionSetModel, AuthPermission, EventStore }
import org.totalgrid.reef.messaging.Descriptors

import scala.collection.JavaConversions._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.messaging.OptionalProtos._
import SquerylModel._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

// Implicit squeryl list -> query conversion

/**
 * static seed function to bootstrap users + permissions into the system
 * TODO: remove static user seed data
 */
object AuthTokenService {
  import org.totalgrid.reef.models.{ Agent, AuthPermission, PermissionSet, PermissionSetJoin, AgentPermissionSetJoin }
  def seed() {

    transaction {
      if (ApplicationSchema.agents.Count.head == 0) {
        val core = ApplicationSchema.agents.insert(new Agent("core", "core"))
        val op = ApplicationSchema.agents.insert(new Agent("operator", "operator"))
        val guest = ApplicationSchema.agents.insert(new Agent("guest", "guest"))

        val read_only = ApplicationSchema.permissions.insert(new AuthPermission(true, "*", "get"))
        val all = ApplicationSchema.permissions.insert(new AuthPermission(true, "*", "*"))

        val timeout = 18144000000L // one month

        val read_set = ApplicationSchema.permissionSets.insert(new PermissionSet("read_only", timeout))
        ApplicationSchema.permissionSetJoins.insert(new PermissionSetJoin(read_set.id, read_only.id))

        val all_set = ApplicationSchema.permissionSets.insert(new PermissionSet("all", timeout))
        ApplicationSchema.permissionSetJoins.insert(new PermissionSetJoin(all_set.id, all.id))

        ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(all_set.id, core.id))
        ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(read_set.id, core.id))
        ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(all_set.id, op.id))
        ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(read_set.id, guest.id))
      }
    }
  }
}

/**
 * auth token specicic code for searching the sql table and converting from 
 */
trait AuthTokenConversions
    extends MessageModelConversion[AuthToken, AuthTokenModel]
    with UniqueAndSearchQueryable[AuthToken, AuthTokenModel] {

  val table = ApplicationSchema.authTokens

  def getRoutingKey(req: AuthToken) = ProtoRoutingKeys.generateRoutingKey {
    req.loginLocation :: req.agent.name :: Nil
  }

  def uniqueQuery(proto: AuthToken, sql: AuthTokenModel) = {
    List(
      proto.agent.map(agent => sql.agentId in AgentConversions.uniqueQueryForId(agent, { _.id })),
      proto.loginLocation.asParam(sql.loginLocation === _),
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
    b.setAgent(AgentConversions.convertToProto(entry.agent.value))
    b.setExpirationTime(entry.expirationTime)
    entry.permissionSets.value.foreach(ps => b.addPermissionSets(PermissionSetConversions.convertToProto(ps)))
    b.setToken(entry.token).build
  }

  // TODO: remove createModelEntry from MessageModelConversion trait
  def createModelEntry(proto: AuthToken): AuthTokenModel = throw new Exception
}

class AuthTokenServiceModel(protected val subHandler: ServiceSubscriptionHandler, eventSink: Event => EventStore)
    extends SquerylServiceModel[AuthToken, AuthTokenModel]
    with EventedServiceModel[AuthToken, AuthTokenModel]
    with AuthTokenConversions {

  override def createFromProto(req: AuthToken): AuthTokenModel = {

    val currentTime = System.currentTimeMillis // need one time for authToken DB entry and posted event.

    // check the password, PUNT: maybe replace this with a nonce + MD5 or something better
    val agent: AgentModel = AgentConversions.findRecord(req.getAgent).getOrElse(postLoginException("", currentTime, Status.UNAUTHORIZED, "Invalid agent or password")) // no agent field or unknown agent!
    if (req.getAgent.getPassword != agent.password) postLoginException(agent.name, currentTime, Status.UNAUTHORIZED, "Invalid agent or password") // invalid password

    val availableSets = agent.permissionSets.value.toList // permissions we can have
    val permissionsRequested = req.getPermissionSetsList.toList // permissions we are asking for

    // allow the user to request either all of their permission sets or just a subset, barf if they ask for permisions they dont have
    val setQuerySize = permissionsRequested.map(ps => PermissionSetConversions.searchQuerySize(ps)).sum
    val permissionSets = if (setQuerySize > 0) {
      val askedForSets = permissionsRequested.map(ps => PermissionSetConversions.findRecords(ps)).flatten.distinct
      val unavailableSets = askedForSets.diff(availableSets)
      if (unavailableSets.size > 0) postLoginException(agent.name, currentTime, Status.UNAUTHORIZED, "No access to permission sets: " + unavailableSets)
      askedForSets
    } else {
      availableSets
    }

    // allow the user to set the expiration time explicitly or use the default from the
    // most restrictive permissionset 
    val expirationTime = if (req.hasExpirationTime) {
      val time = req.getExpirationTime
      if (time <= currentTime) postLoginException(agent.name, currentTime, Status.BAD_REQUEST, "Expiration time cannot be in the past")
      time
    } else {
      currentTime + permissionSets.map(ps => ps.defaultExpirationTime).min
    }

    // TODO: generate an unguessable security token
    val token = java.util.UUID.randomUUID().toString

    val authToken = table.insert(new AuthTokenModel(token, agent.id, req.getLoginLocation, expirationTime))

    // link the token to all of the permisisonsSet they have checked out access to
    permissionSets.foreach(ps => ApplicationSchema.tokenSetJoins.insert(new AuthTokenPermissionSetJoin(ps.id, authToken.id)))

    postLoginEvent(agent.name, currentTime)
    authToken
  }

  def postLoginEvent(agentName: String, currentTime: Long, status: Status = Status.OK, reason: String = ""): Unit = {
    import org.totalgrid.reef.event.EventType._

    val alist = new AttributeList
    alist += ("status" -> AttributeString(status.toString))
    alist += ("reason" -> AttributeString(reason))

    eventSink(Event.newBuilder
      .setTime(currentTime)
      .setEventType(System.UserLogin)
      .setSubsystem("Core") // TODO: Should access a constant somewhere for this.
      .setUserId(agentName)
      .setArgs(alist.toProto)
      .build)
  }

  def postLoginException(agentName: String, currentTime: Long, status: Status, reason: String): AgentModel = {
    postLoginEvent(agentName, currentTime, status, reason)
    throw new ServiceException(reason, status)
  }

  override def updateFromProto(req: AuthToken, existing: AuthTokenModel): (AuthTokenModel, Boolean) = {
    throw new Exception("cannot update auth tokens")
  }

  // we are faking the delete operation, we actually want to keep the row around forever as an audit log
  override def delete(entry: AuthTokenModel): AuthTokenModel = {
    import org.totalgrid.reef.event.EventType._

    entry.expirationTime = -1
    table.update(entry)
    eventSink(Event.newBuilder
      .setTime(java.lang.System.currentTimeMillis)
      .setEventType(System.UserLogout)
      .setSubsystem("Core") // TODO: Should access a constant somewhere for this.
      .setUserId(entry.agent.value.name)
      .build)
    onUpdated(entry)
    postDelete(entry)
    entry
  }
}

class AuthTokenServiceModelFactory(pub: ServiceEventPublishers, eventSink: Event => EventStore)
    extends BasicModelFactory[AuthToken, AuthTokenServiceModel](pub, classOf[AuthToken]) {

  def model = new AuthTokenServiceModel(subHandler, eventSink)
}

class AuthTokenService(protected val modelTrans: ServiceTransactable[AuthTokenServiceModel])
    extends BaseProtoService[AuthToken, AuthTokenModel, AuthTokenServiceModel]
    with BaseProtoService.GetEnabled
    with BaseProtoService.PostLikeEnabled // every PUT should create a new entry
    with BaseProtoService.DeleteEnabled
    with BaseProtoService.SubscribeDisabled {
  override val useAuth = false
  override val descriptor = Descriptors.authToken
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// TODO: move agent and permissions converstion traits to own files when services are implemented
////////////////////////////////////////////////////////////////////////////////////////////////////
trait AgentConversions
    extends MessageModelConversion[Agent, AgentModel]
    with UniqueAndSearchQueryable[Agent, AgentModel] {

  val table = ApplicationSchema.agents

  def uniqueQuery(proto: Agent, sql: AgentModel) = {
    List(
      proto.uid.asParam(sql.id === _.toInt),
      proto.name.asParam(sql.name === _))
  }

  def searchQuery(proto: Agent, sql: AgentModel) = Nil

  def getRoutingKey(req: Agent) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: AgentModel, updated: AgentModel): Boolean = false

  def convertToProto(entry: AgentModel): Agent = {
    Agent.newBuilder.setUid(entry.id.toString).setName(entry.name).build
  }

  def createModelEntry(proto: Agent): AgentModel = throw new Exception
}
object AgentConversions extends AgentConversions

trait PermissionSetConversions
    extends MessageModelConversion[PermissionSet, PermissionSetModel]
    with UniqueAndSearchQueryable[PermissionSet, PermissionSetModel] {

  val table = ApplicationSchema.permissionSets

  def uniqueQuery(proto: PermissionSet, sql: PermissionSetModel) = {
    List(
      proto.uid.asParam(sql.id === _.toInt),
      proto.name.asParam(sql.name === _))
  }

  def searchQuery(proto: PermissionSet, sql: PermissionSetModel) = Nil

  def getRoutingKey(req: PermissionSet) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: PermissionSetModel, updated: PermissionSetModel): Boolean = false

  def convertToProto(entry: PermissionSetModel): PermissionSet = {
    val b = PermissionSet.newBuilder.setUid(entry.id.toString)
    b.setName(entry.name)
    b.setDefaultExpirationTime(entry.defaultExpirationTime)
    entry.permissions.value.foreach(p => b.addPermissions(PermissionConversion.convertToProto(p)))
    b.build
  }

  def createModelEntry(proto: PermissionSet): PermissionSetModel = throw new Exception
}
object PermissionSetConversions extends PermissionSetConversions

trait PermissionConversion
    extends UniqueAndSearchQueryable[Permission, AuthPermission] {

  val table = ApplicationSchema.permissions

  def convertToProto(entry: AuthPermission): Permission = {
    val b = Permission.newBuilder.setUid(entry.id.toString)
    b.setAllow(entry.allow)
    b.setResource(entry.resource)
    b.setVerb(entry.verb)
    b.build
  }

  def uniqueQuery(proto: Permission, sql: AuthPermission) = {
    List(
      proto.uid.asParam(sql.id === _.toInt))
  }

  def searchQuery(proto: Permission, sql: AuthPermission) = {
    List(
      proto.allow.asParam(sql.allow === _),
      proto.resource.asParam(sql.resource === _),
      proto.verb.asParam(sql.verb === _))
  }
}
object PermissionConversion extends PermissionConversion