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
import org.totalgrid.reef.api.sapi.types.Optional._

import org.totalgrid.reef.client.sapi.Descriptors

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{ Table, Query }
import org.totalgrid.reef.client.sapi.OptionalProtos._
import SquerylModel._

import org.totalgrid.reef.services.core.util.UUIDConversions._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.proto.Model.{ CommandType, Command => CommandProto, Entity => EntityProto }
import org.totalgrid.reef.models.{ Command, ApplicationSchema, Entity }

class CommandService(protected val model: CommandServiceModel)
    extends SyncModeledServiceBase[CommandProto, Command, CommandServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.command

  override def preCreate(context: RequestContext, proto: CommandProto) = {
    if (!proto.hasName || !proto.hasType || !proto.hasDisplayName) {
      throw new BadRequestException("Must specify name, type and displayName when creating command")
    }
    proto
  }

  override def preUpdate(context: RequestContext, request: CommandProto, existing: Command) = {
    preCreate(context, request)
  }
}

class CommandServiceModel(commandHistoryModel: UserCommandRequestServiceModel,
  commandSelectModel: CommandAccessServiceModel)
    extends SquerylServiceModel[CommandProto, Command]
    with EventedServiceModel[CommandProto, Command]
    with SimpleModelEntryCreation[CommandProto, Command]
    with CommandServiceConversion {

  val table = ApplicationSchema.commands
  def getCommands(names: List[String]): Query[Command] = {
    Command.findByNames(names)
  }

  override def preDelete(context: RequestContext, entry: Command) {
    entry.logicalNode.value match {
      case Some(parent) =>
        throw new BadRequestException("Cannot delete command: " + entry.entityName + " while it is still assigned to logicalNode " + parent.name)
      case None => // no endpoint so we are free to delete command
    }
    entry.currentActiveSelect.value match {
      case Some(select) =>
        throw new BadRequestException("Cannot delete command: " + entry.entityName + " while there is an active select or block " + select)
      case None => // no selection on command
    }
  }

  override def postDelete(context: RequestContext, entry: Command) {

    val selects = entry.selectHistory.value
    val commandHistory = entry.commandHistory.value

    logger.info("Deleting Command: " + entry.entityName + " selects: " + selects.size + " history: " + commandHistory.size)

    selects.foreach(s => commandSelectModel.removeAccess(context, s))
    commandHistory.foreach(s => commandHistoryModel.delete(context, s))

    EntityQueryManager.deleteEntity(entry.entity.value)
  }

}

trait CommandServiceConversion extends UniqueAndSearchQueryable[CommandProto, Command] {

  def getRoutingKey(req: CommandProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.uuid :: req.name :: req.entity.uuid.uuid :: Nil
  }

  def uniqueQuery(proto: CommandProto, sql: Command) = {

    val esearch = EntitySearch(proto.uuid.uuid, proto.name, proto.name.map(x => List("Command")))
    List(
      esearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })),
      proto.entity.map(ent => sql.entityId in EntityQueryManager.typeIdsFromProtoQuery(ent, "Command")))
  }

  // TODO: add symmetric getCommandsOwnedByEntity and getCommandsBelongingToEndpoint functions to java API - backlog-62
  def searchQuery(proto: CommandProto, sql: Command) = List(
    proto.logicalNode.map(logicalNode => sql.entityId in EntityQueryManager.findIdsOfChildren(logicalNode, "source", "Command")))

  def createModelEntry(proto: CommandProto): Command = {
    Command.newInstance(proto.getName, proto.getDisplayName, proto.getType.getNumber, proto.uuid)
  }

  def isModified(entry: Command, existing: Command) = {
    entry.lastSelectId != existing.lastSelectId
  }

  def convertToProto(sql: Command): CommandProto = {
    val b = CommandProto.newBuilder
      .setUuid(makeUuid(sql.entityId))
      .setName(sql.entityName)
      .setDisplayName(sql.displayName)

    //sql.entity.asOption.foreach(e => b.setEntity(EQ.entityToProto(e)))
    sql.entity.asOption match {
      case Some(e) => b.setEntity(EntityQueryManager.entityToProto(e))
      case None => b.setEntity(EntityProto.newBuilder.setUuid(makeUuid(sql.entityId)))
    }

    sql.logicalNode.value // autoload logicalNode
    sql.logicalNode.asOption.foreach { _.foreach { ln => b.setLogicalNode(EntityQueryManager.minimalEntityToProto(ln).build) } }
    b.setType(CommandType.valueOf(sql.commandType))
    b.build
  }
}
