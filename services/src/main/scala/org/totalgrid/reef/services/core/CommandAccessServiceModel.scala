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

import org.squeryl.{ Table, Query }

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Model.{ Command => FepCommandProto }
import org.totalgrid.reef.client.service.proto.Commands.{ CommandLock => AccessProto }
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess._
import scala.collection.JavaConversions._
import org.totalgrid.reef.models.UUIDConversions._

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.{ BadRequestException, UnauthorizedException }

import org.totalgrid.reef.models.{ UserCommandModel, ApplicationSchema, CommandLockModel => AccessModel, Command => CommandModel, CommandBlockJoin }
import java.util.Date

class CommandLockServiceModel
    extends SquerylServiceModel[Long, AccessProto, AccessModel]
    with EventedServiceModel[AccessProto, AccessModel]
    with CommandLockConversion {

  def commandModel = modelOption.get
  var modelOption: Option[CommandServiceModel] = None
  def setCommandModel(commandModel: CommandServiceModel) = modelOption = Some(commandModel)

  import org.squeryl.PrimitiveTypeMode._

  val table = ApplicationSchema.commandAccess

  override def createFromProto(context: RequestContext, req: AccessProto): AccessModel = {

    val user = context.agent.entityName
    req.user.foreach { u => if (user != u) throw new BadRequestException("User name in request doesn't match any auth token owners, correct name or leave blank.") }

    val commands = findCommands(req.getCommandsList.toList)

    context.auth.authorize(context, "command_lock", "create", commands.map { _.entityId })
    if (req.getAccess == AccessProto.AccessMode.ALLOWED) {
      context.auth.authorize(context, "command_lock_select", "create", commands.map { _.entityId })
      // process the time here. On requests the time is relative, on responses it is 
      // an absolute UTC time
      val time = req.expireTime match {
        case Some(t) => Some(t + System.currentTimeMillis)
        case None => None
      }
      // Do the select on the model, given the requested list of commands
      selectCommands(context, user, time, commands)
    } else {
      context.auth.authorize(context, "command_lock_block", "create", commands.map { _.entityId })
      blockCommands(context, user, commands)
    }
  }

  def userHasSelect(cmd: CommandModel, agentId: Long, forTime: Long): Option[AccessModel] = {
    val joinTable = ApplicationSchema.commandToBlocks
    val selectInt = AccessProto.AccessMode.ALLOWED.getNumber

    val lookup = from(joinTable, table)((join, acc) =>
      where(join.commandId === cmd.id and
        join.accessId === acc.id and acc.deleted === false and acc.access === selectInt and acc.agentId === agentId and acc.expireTime > forTime)
        select (acc))

    if (lookup.size == 1) lookup.headOption
    else None
  }

  private def areAnyBlockedById(ids: List[Long]): List[AccessModel] = {
    val joinTable = ApplicationSchema.commandToBlocks
    val blockInt = AccessProto.AccessMode.BLOCKED.getNumber

    from(joinTable, table)((join, acc) =>
      where(join.commandId in ids and
        join.accessId === acc.id and acc.deleted === false and (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
        select (acc)).toList
  }

  protected def addEntryForAll(request: RequestContext, entry: AccessModel, cmds: List[CommandModel]) = {
    try {
      commandModel.exclusiveUpdate(request, cmds.toList, (cmd: CommandModel) => cmd.lastSelectId != Some(entry.id)) { cmdList =>
        // Update all commands to have this access id
        cmdList.map { cmd =>
          ApplicationSchema.commandToBlocks.insert(new CommandBlockJoin(cmd.id, entry.id))
          cmd.lastSelectId = Some(entry.id)
          cmd
        }
      }
    } catch {
      case ex: AcquireConditionNotMetException =>
        // Race condition, return failure
        throw new UnauthorizedException("Some or all commands selected")
    }
  }

  private def blockCommands(context: RequestContext, user: String, commands: List[CommandModel]): AccessModel = {

    val accEntry = create(context, new AccessModel(AccessProto.AccessMode.BLOCKED.getNumber, None, context.agent.id, false))
    addEntryForAll(context, accEntry, commands.toList)
    accEntry
  }

  private def findCommands(commands: List[FepCommandProto]): List[CommandModel] = {
    // just remove duplicate names from request
    val commandNames = commands.map { _.getName }.distinct
    val foundCommands = CommandModel.findByNames(commandNames).toList

    if (foundCommands.size != commandNames.size) {
      val missing = commandNames.diff(foundCommands.map { _.entityName })
      throw new BadRequestException("Commands not found: " + missing)
    }
    foundCommands
  }

  private def selectCommands(context: RequestContext, user: String, expireTime: Option[Long], cmds: List[CommandModel]): AccessModel = {

    val cmdIds = cmds.map { _.id }
    val blocked = areAnyBlockedById(cmdIds)
    if (!blocked.isEmpty) {
      val msgs = blocked.map { acc =>
        "( " + acc.commands.map { _.entityName }.mkString(", ") +
          " locked by: " + acc.agent.value.entityName +
          " until: " + acc.expireTime.map { t => new Date(t).toString }.getOrElse(" unblocked") +
          " )"
      }.mkString(", ")
      throw new UnauthorizedException("Some commands are blocked: " + msgs)
    }

    val accEntry = create(context, new AccessModel(AccessProto.AccessMode.ALLOWED.getNumber, expireTime, context.agent.id, false))
    addEntryForAll(context, accEntry, cmds.toList)
    accEntry
  }

  def removeAccess(context: RequestContext, access: AccessModel): Unit = {
    // don't delete, just mark as deleted
    update(context, access.copy(deleted = true), access)

    val cmds = commandModel.table.where(cmd => cmd.lastSelectId === access.id).toList

    context.auth.authorize(context, "command_lock", "delete", List(access.agent.value.entityId))
    if (cmds.length > 0) {

      // Remove last select (since it doesn't refer to anything real) on all commands
      commandModel.exclusiveUpdate(context, cmds, (cmd: CommandModel) => cmd.lastSelectId == Some(access.id)) { cmdList =>
        cmdList.map { cmd =>
          cmd.lastSelectId = None
          cmd
        }
      }
    }
  }

  // really delete the access logs when we delete the last associated command
  def unlinkLocks(context: RequestContext, command: CommandModel) {

    val selectsOnCommand = AccessModel.selectsForCommands(List(command.id)).toList

    val selectsOnOnlyThisCommand = selectsOnCommand.filter { s =>
      val links = from(ApplicationSchema.commandToBlocks)(join =>
        where((join.accessId === s.id))
          select (join.id)).toList
      links.size == 1
    }

    ApplicationSchema.commandToBlocks.deleteWhere(t => t.commandId === command.id)

    selectsOnOnlyThisCommand.foreach { delete(context, _) }
  }
}

trait CommandLockConversion
    extends UniqueAndSearchQueryable[AccessProto, AccessModel] {

  import org.squeryl.PrimitiveTypeMode._
  import AccessProto._
  import org.totalgrid.reef.client.service.proto.OptionalProtos._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  import org.squeryl.dsl.fsm.SelectState
  import org.squeryl.dsl.QueryYield
  import org.squeryl.dsl.ast.OrderByArg

  override def getOrdering[R](select: SelectState[R], sql: AccessModel): QueryYield[R] = select.orderBy(new OrderByArg(sql.id).desc)
  def sortResults(list: List[AccessProto]) = list.reverse

  def getRoutingKey(req: AccessProto) = ProtoRoutingKeys.generateRoutingKey {
    req.id.value ::
      req.access ::
      req.user :: Nil
  }

  def relatedEntities(entries: List[AccessModel]) = {
    entries.map { _.commands.map { _.entityId } }.flatten
  }

  def uniqueQuery(proto: AccessProto, sql: AccessModel) = {
    List(
      proto.id.value.asParam(id => sql.id === id.toLong))
  }

  def searchQuery(proto: AccessProto, sql: AccessModel) = {
    val commandsListOption = if (proto.getCommandsCount > 0) Some(proto.getCommandsList.toList.map { _.getName }) else None
    List(
      proto.access.asParam(ac => sql.access === ac.getNumber),
      proto.deleted.asParam(deleted => sql.deleted === deleted),
      commandsListOption.map(names => sql.id in findAccessesByCommandNames(names)))
  }

  private def findAccessesByCommandNames(names: List[String]) = {
    from(ApplicationSchema.commandToBlocks, ApplicationSchema.commands, ApplicationSchema.commandAccess)((selectJoin, cmd, access) =>
      where(
        (selectJoin.commandId === cmd.id) and
          (cmd.id in CommandModel.findIdsByNames(names)) and
          (selectJoin.accessId === access.id) and
          (access.deleted === false))
        select (selectJoin.accessId)).distinct
  }

  def createModelEntry(proto: AccessProto): AccessModel = throw new Exception("Wrong interface")

  def isModified(entry: AccessModel, existing: AccessModel): Boolean = {
    entry.access != existing.access || entry.expireTime != entry.expireTime || entry.deleted != existing.deleted
  }

  def convertToProto(entry: AccessModel): AccessProto = {
    val b = AccessProto.newBuilder
      .setId(makeId(entry))
      .setAccess(AccessMode.valueOf(entry.access))

    entry.commands.foreach { cmd =>
      b.addCommands(FepCommandProto.newBuilder.setName(cmd.entityName).setUuid(makeUuid(cmd)))
    }

    // optional sql fields
    entry.expireTime.foreach(b.setExpireTime(_))
    b.setUser(entry.agent.value.entityName)
    b.setDeleted(entry.deleted)

    b.build
  }
}