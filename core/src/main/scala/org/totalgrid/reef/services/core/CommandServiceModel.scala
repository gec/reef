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
import org.totalgrid.reef.util.Optional._

import org.totalgrid.reef.proto.Descriptors

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{ Table, Query }
import org.totalgrid.reef.proto.OptionalProtos._
import SquerylModel._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import java.util.UUID
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.japi.BadRequestException
import org.totalgrid.reef.proto.Model.{ CommandType, Command => CommandProto, Entity => EntityProto }
import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.models.{ UserCommandModel, Command, ApplicationSchema, Entity }

class CommandService(protected val modelTrans: ServiceTransactable[CommandServiceModel])
    extends SyncModeledServiceBase[CommandProto, Command, CommandServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.command

  override def preCreate(context: RequestContext, proto: CommandProto, headers: RequestEnv) = {
    if (!proto.hasName || !proto.hasType || !proto.hasDisplayName) {
      throw new BadRequestException("Must specify name, type and displayName when creating command")
    }
    proto
  }

  override def preUpdate(context: RequestContext, request: CommandProto, existing: Command, headers: RequestEnv) = {
    preCreate(context, request, headers)
  }
}

class CommandServiceModelFactory(dependencies: ServiceDependencies,
  commandHistoryFac: UserCommandRequestServiceModelFactory,
  accessFac: CommandAccessServiceModelFactory)
    extends BasicModelFactory[CommandProto, CommandServiceModel](dependencies, classOf[CommandProto]) {

  def model = {
    val m = new CommandServiceModel(subHandler)
    val accessModel = accessFac.model(m)
    m.setCommandSelectModel(accessModel)
    m.setCommandHistoryModel(commandHistoryFac.model(accessModel))
    m
  }
}

class CommandServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[CommandProto, Command]
    with EventedServiceModel[CommandProto, Command]
    with CommandServiceConversion {

  val table = ApplicationSchema.commands
  def getCommands(names: List[String]): Query[Command] = {
    Command.findByNames(names)
  }
  def createAndSetOwningNode(context: RequestContext, commands: List[String], dataSource: Entity): Unit = {
    if (commands.size == 0) return

    val allreadyExistingCommands = Entity.asType(ApplicationSchema.commands, EQ.findEntitiesByName(commands).toList, Some("Command"))
    val newCommands = commands.diff(allreadyExistingCommands.map(_.entityName).toList)
    if (!newCommands.isEmpty) throw new BadRequestException("Trying to set endpoint for unknown points: " + newCommands)

    val changeCommandOwner = allreadyExistingCommands.filter { c => c.sourceEdge.value.map(_.parentId != dataSource.id) getOrElse (true) }
    changeCommandOwner.foreach(p => {
      p.sourceEdge.value.foreach(EQ.deleteEdge(_))
      EQ.addEdge(dataSource, p.entity.value, "source")
      update(context, p, p)
    })
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

    EQ.deleteEntity(entry.entity.value)
  }

  // ugly link related bolierplate to break circular dependency
  var commandHistoryModelOption: Option[UserCommandRequestServiceModel] = None
  var commandSelectModelOption: Option[CommandAccessServiceModel] = None
  def setCommandHistoryModel(m: UserCommandRequestServiceModel) {

    commandHistoryModelOption = Some(m)
  }
  def setCommandSelectModel(m: CommandAccessServiceModel) {

    commandSelectModelOption = Some(m)
  }

  def commandHistoryModel = commandHistoryModelOption.get
  def commandSelectModel = commandSelectModelOption.get
}

trait CommandServiceConversion extends MessageModelConversion[CommandProto, Command] with UniqueAndSearchQueryable[CommandProto, Command] {

  def getRoutingKey(req: CommandProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.uuid :: req.name :: req.entity.uuid.uuid :: Nil
  }

  def uniqueQuery(proto: CommandProto, sql: Command) = {

    val esearch = EntitySearch(proto.uuid.uuid, proto.name, proto.name.map(x => List("Command")))
    List(
      esearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })),
      proto.entity.map(ent => sql.entityId in EQ.typeIdsFromProtoQuery(ent, "Command")))
  }

  def searchQuery(proto: CommandProto, sql: Command) = Nil

  def createModelEntry(proto: CommandProto): Command = {
    Command.newInstance(proto.getName, proto.getDisplayName, proto.getType.getNumber)
  }

  def isModified(entry: Command, existing: Command) = {
    entry.lastSelectId != existing.lastSelectId
  }

  def convertToProto(sql: Command): CommandProto = {
    // TODO: fill out connected and selected parts of proto
    val b = CommandProto.newBuilder
      .setUuid(makeUuid(sql.entityId))
      .setName(sql.entityName)
      .setDisplayName(sql.displayName)

    //sql.entity.asOption.foreach(e => b.setEntity(EQ.entityToProto(e)))
    sql.entity.asOption match {
      case Some(e) => b.setEntity(EQ.entityToProto(e))
      case None => b.setEntity(EntityProto.newBuilder.setUuid(makeUuid(sql.entityId)))
    }

    sql.logicalNode.value // autoload logicalNode
    sql.logicalNode.asOption.foreach { _.foreach { ln => b.setLogicalNode(EQ.minimalEntityToProto(ln).build) } }
    b.setType(CommandType.valueOf(sql.commandType))
    b.build
  }
}
