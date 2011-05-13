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

import org.totalgrid.reef.proto.Auth._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.{ ProtoRoutingKeys }
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.{ BadRequestException }
import org.totalgrid.reef.models.{ ApplicationSchema, Agent => AgentModel, AgentPermissionSetJoin }

class AgentService(protected val modelTrans: ServiceTransactable[AgentServiceModel])
    extends BasicSyncModeledService[Agent, AgentModel, AgentServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.agent
}

class AgentServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[Agent, AgentServiceModel](pub, classOf[Agent]) {

  def model = new AgentServiceModel(subHandler)
}

class AgentServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[Agent, AgentModel]
    with EventedServiceModel[Agent, AgentModel]
    with AgentConversions {

  override def createFromProto(req: Agent): AgentModel = {

    if (!req.hasName || !req.hasPassword) throw new BadRequestException("Must include name and password when creating an Agent.")
    if (req.getPermissionSetsCount == 0) throw new BadRequestException("Must specify atleast 1 PermissionSet when creating an Agent.")

    validatePassword(req.getPassword)

    val permissionSets = findRequestedPermissionSets(req)

    val agent = create(AgentModel.createAgentWithPassword(req.getName, req.getPassword))
    permissionSets.foreach { p => ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(p.id, agent.id)) }

    agent
  }

  override def updateFromProto(req: Agent, existing: AgentModel) = {

    val changingPassword = if (req.hasPassword) {
      // if a password was included but it was correct we are not changing anything
      existing.checkPassword(req.getPassword) == false
    } else {
      false
    }

    val (removed, added) = if (req.getPermissionSetsCount > 0) {

      val requestedSets = findRequestedPermissionSets(req)
      val currentSets = existing.permissionSets.value.toList

      (currentSets.diff(requestedSets), requestedSets.diff(currentSets))
    } else {
      (Nil, Nil)
    }

    if ((removed.size > 0 || added.size > 0) && changingPassword) {
      throw new BadRequestException("Cannot update password and permissions in same request, remove password or use old password")
    }

    if (changingPassword) {
      validatePassword(req.getPassword)
      update(existing.copyWithUpdatedPassword(req.getPassword), existing)
    } else {
      if (removed.size > 0 || added.size > 0) {
        added.foreach { p => ApplicationSchema.agentSetJoins.insert(new AgentPermissionSetJoin(p.id, existing.id)) }
        ApplicationSchema.agentSetJoins.deleteWhere(join => join.permissionSetId in removed.map { _.id } and join.agentId === existing.id)
        (existing, true)
      } else {
        (existing, false)
      }
    }
  }

  override def preDelete(entry: AgentModel) {
    if (entry.authTokens.value.size > 0) {
      ApplicationSchema.authTokens.deleteWhere(at => at.id in entry.authTokens.value.map(_.id))
    }
  }

  private def validatePassword(password: String) {
    // TODO: password settings?
    if (password.length < 4) {
      throw new BadRequestException("Password must be atleast 4 characters")
    }
  }

  def findRequestedPermissionSets(req: Agent) = {
    val requestedPermissions = req.getPermissionSetsList.toList
    if (requestedPermissions.exists { p => p.getName == "*" || p.getUuid == "*" }) {
      throw new BadRequestException("Cannot use wildcard in PermissionSet specifiers, must use UUIDs or names: " + requestedPermissions)
    }
    val permissionSets = requestedPermissions.map(PermissionSetConversions.findRecords(_)).flatten

    if (permissionSets.isEmpty) throw new BadRequestException("No PermissionSets were found with names: " + requestedPermissions)

    permissionSets
  }
}

trait AgentConversions
    extends MessageModelConversion[Agent, AgentModel]
    with UniqueAndSearchQueryable[Agent, AgentModel] {

  val table = ApplicationSchema.agents

  def uniqueQuery(proto: Agent, sql: AgentModel) = {
    val eSearch = EntitySearch(proto.uuid.uuid, proto.name, proto.name.map(x => List("Agent")))
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