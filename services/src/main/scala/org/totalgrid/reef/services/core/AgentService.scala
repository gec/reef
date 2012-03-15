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

import org.totalgrid.reef.client.service.proto.Auth._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.{ SaltedPasswordHelper, ApplicationSchema, Agent => AgentModel, AgentPermissionSetJoin }

import org.totalgrid.reef.models.UUIDConversions._

class AgentService(protected val model: AgentServiceModel)
    extends SyncModeledServiceBase[Agent, AgentModel, AgentServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.agent

  override protected def performUpdate(context: RequestContext, model: ServiceModelType, request: ServiceType, existing: ModelType): Tuple2[ModelType, Boolean] = {
    model.updateFromProto(context, request, existing)
  }
}

class AgentServiceModel
    extends SquerylServiceModel[Long, Agent, AgentModel]
    with EventedServiceModel[Agent, AgentModel]
    with AgentConversions {

  val entityModel = new EntityServiceModel

  def createAgentWithPassword(context: RequestContext, name: String, password: String): AgentModel = {
    import SaltedPasswordHelper._

    val (digest, saltText) = makeDigestAndSalt(password)
    val entity = entityModel.findOrCreate(context, name, "Agent" :: Nil, None)
    val agent = new AgentModel(entity.id, enc64(digest), enc64(saltText))
    agent.entity.value = entity
    agent
  }

  override def createFromProto(context: RequestContext, req: Agent): AgentModel = {

    if (!req.hasName || !req.hasPassword) throw new BadRequestException("Must include name and password when creating an Agent.")
    if (req.getPermissionSetsCount == 0) throw new BadRequestException("Must specify atleast 1 PermissionSet when creating an Agent.")

    validatePassword(req.getPassword)

    val permissionSets = findRequestedPermissionSets(context, req)

    val agent = create(context, createAgentWithPassword(context, req.getName, req.getPassword))
    permissionSets.foreach { p => ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(p.id, agent.id)) }

    agent
  }

  override def updateFromProto(context: RequestContext, req: Agent, existing: AgentModel) = {

    val changingPassword = if (req.hasPassword) {
      // if a password was included but it was correct we are not changing anything
      existing.checkPassword(req.getPassword) == false
    } else {
      false
    }

    val (removed, added) = if (req.getPermissionSetsCount > 0) {

      val requestedSets = findRequestedPermissionSets(context, req)
      val currentSets = existing.permissionSets.value.toList

      (currentSets.diff(requestedSets), requestedSets.diff(currentSets))
    } else {
      (Nil, Nil)
    }

    if ((removed.size > 0 || added.size > 0) && changingPassword) {
      throw new BadRequestException("Cannot update password and permissions in same request, remove password or use old password")
    }

    if (changingPassword) {
      context.auth.authorize(context, "agent_password", "update", List(existing.entity.value))
      validatePassword(req.getPassword)
      update(context, existing.copyWithUpdatedPassword(req.getPassword), existing)
    } else {
      if (removed.size > 0 || added.size > 0) {
        context.auth.authorize(context, "agent_roles", "update", List(existing.entity.value))
        added.foreach { p => ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(p.id, existing.id)) }
        ApplicationSchema.agentSetJoins.deleteWhere(join => join.permissionSetId in removed.map { _.id } and join.agentId === existing.id)

        onUpdated(context, existing)
        (existing, true)
      } else {
        (existing, false)
      }
    }
  }

  override def preDelete(context: RequestContext, entry: AgentModel) {

    if (!entry.applications.value.isEmpty) {
      throw new BadRequestException("Can't delete agent when application is registered with this application")
    }

    if (entry.authTokens.value.size > 0) {
      ApplicationSchema.authTokens.deleteWhere(at => at.id in entry.authTokens.value.map(_.id))
    }
  }

  override def postDelete(context: RequestContext, entry: AgentModel) {
    entityModel.delete(context, entry.entity.value)
  }

  private def validatePassword(password: String) {
    // TODO: password settings?
    if (password.length < 4) {
      throw new BadRequestException("Password must be atleast 4 characters")
    }
  }

  def findRequestedPermissionSets(context: RequestContext, req: Agent) = {
    val requestedPermissions = req.getPermissionSetsList.toList
    if (requestedPermissions.exists { p => p.getName == "*" || p.getUuid == "*" }) {
      throw new BadRequestException("Cannot use wildcard in PermissionSet specifiers, must use UUIDs or names: " + requestedPermissions)
    }
    val permissionSets = requestedPermissions.map(PermissionSetConversions.findRecords(context, _)).flatten

    if (permissionSets.isEmpty) throw new BadRequestException("No PermissionSets were found with names: " + requestedPermissions)

    permissionSets
  }
}

trait AgentConversions
    extends UniqueAndSearchQueryable[Agent, AgentModel] {

  val table = ApplicationSchema.agents

  def sortResults(list: List[Agent]) = list.sortBy(_.getName)

  def relatedEntities(entries: List[AgentModel]) = {
    entries.map { _.entity.value }
  }

  def uniqueQuery(proto: Agent, sql: AgentModel) = {
    val eSearch = EntitySearch(proto.uuid.value, proto.name, proto.name.map(x => List("Agent")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })))
  }

  def searchQuery(proto: Agent, sql: AgentModel) = Nil

  def getRoutingKey(req: Agent) = ProtoRoutingKeys.generateRoutingKey {
    req.name :: Nil
  }

  def isModified(existing: AgentModel, updated: AgentModel): Boolean =
    existing.digest != updated.digest || existing.salt != updated.salt

  def convertToProto(entry: AgentModel): Agent = {
    val b = Agent.newBuilder.setUuid(makeUuid(entry)).setName(entry.entityName)

    entry.permissionSets.value.foreach { p => b.addPermissionSets(PermissionSetConversions.convertToProto(p)) }

    b.build
  }

  def createModelEntry(proto: Agent): AgentModel = throw new Exception("not implemented")
}
object AgentConversions extends AgentConversions