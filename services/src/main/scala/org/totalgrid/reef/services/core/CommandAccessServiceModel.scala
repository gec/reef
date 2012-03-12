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

import org.totalgrid.reef.models.{ ApplicationSchema, CommandLockModel => AccessModel, Command => CommandModel, CommandBlockJoin }
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

    val user = context.getHeaders.userName getOrElse { throw new BadRequestException("User must be in header.") }
    req.user.foreach { u => if (user != u) throw new BadRequestException("User name in request doesn't match any auth token owners, correct name or leave blank.") }

    val commands = findCommands(req.getCommandsList.toList)

    context.auth.authorize(context, "command_lock", "create", commands.map { _.entity.value })
    if (req.getAccess == AccessProto.AccessMode.ALLOWED) {
      context.auth.authorize(context, "command_lock_select", "create", commands.map { _.entity.value })
      // process the time here. On requests the time is relative, on responses it is 
      // an absolute UTC time
      val time = req.expireTime match {
        case Some(t) => Some(t + System.currentTimeMillis)
        case None => None
      }
      // Do the select on the model, given the requested list of commands
      selectCommands(context, user, time, commands)
    } else {
      context.auth.authorize(context, "command_lock_block", "create", commands.map { _.entity.value })
      blockCommands(context, user, commands)
    }
  }

  def userHasSelect(cmd: CommandModel, user: String, forTime: Long): Boolean = {
    val joinTable = ApplicationSchema.commandToBlocks
    val selectInt = AccessProto.AccessMode.ALLOWED.getNumber

    val lookup = from(joinTable, table)((join, acc) =>
      where(join.commandId === cmd.id and
        join.accessId === acc.id and acc.access === selectInt and acc.agent === Some(user) and acc.expireTime > forTime)
        select (acc))

    lookup.size == 1
  }

  def areAnyBlocked(commands: List[CommandModel]): Boolean = {
    val blocked = areAnyBlockedById(commands.map { _.id }.toList)
    !blocked.isEmpty
  }
  def areAnyBlockedById(ids: List[Long]): List[AccessModel] = {
    val joinTable = ApplicationSchema.commandToBlocks
    val blockInt = AccessProto.AccessMode.BLOCKED.getNumber

    from(joinTable, table)((join, acc) =>
      where(join.commandId in ids and
        join.accessId === acc.id and (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
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

  def blockCommands(context: RequestContext, user: String, commands: List[CommandModel]): AccessModel = {

    val accEntry = create(context, new AccessModel(AccessProto.AccessMode.BLOCKED.getNumber, None, Some(user)))
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

  def selectCommands(context: RequestContext, user: String, expireTime: Option[Long], cmds: List[CommandModel]): AccessModel = {

    val cmdIds = cmds.map { _.id }
    val blocked = areAnyBlockedById(cmdIds)
    if (!blocked.isEmpty) {
      val msgs = blocked.map { acc =>
        "( " + acc.commands.map { _.entityName }.mkString(", ") +
          " locked by: " + acc.agent +
          " until: " + acc.expireTime.map { t => new Date(t).toString }.getOrElse(" unblocked") +
          " )"
      }.mkString(", ")
      throw new UnauthorizedException("Some commands are blocked: " + msgs)
    }

    val accEntry = create(context, new AccessModel(AccessProto.AccessMode.ALLOWED.getNumber, expireTime, Some(user)))
    addEntryForAll(context, accEntry, cmds.toList)
    accEntry
  }

  def removeAccess(context: RequestContext, access: AccessModel): Unit = {
    //context.auth.authorize("command_lock", "delete", access.agent)
    delete(context, access)
    ApplicationSchema.commandToBlocks.deleteWhere(t => t.accessId === access.id)

    val cmds = commandModel.table.where(cmd => cmd.lastSelectId === access.id).toList

    context.auth.authorize(context, "command_lock", "delete", cmds.map { _.entity.value })
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
}

trait CommandLockConversion
    extends UniqueAndSearchQueryable[AccessProto, AccessModel] {

  import org.squeryl.PrimitiveTypeMode._
  import AccessProto._
  import org.totalgrid.reef.client.service.proto.OptionalProtos._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  def sortResults(list: List[AccessProto]) = list.sortBy(_.getExpireTime)

  def getRoutingKey(req: AccessProto) = ProtoRoutingKeys.generateRoutingKey {
    req.id.value ::
      req.access ::
      req.user :: Nil
  }

  def uniqueQuery(proto: AccessProto, sql: AccessModel) = {
    List(
      proto.id.value.asParam(id => sql.id === id.toLong))
  }

  def searchQuery(proto: AccessProto, sql: AccessModel) = {
    val commandsListOption = if (proto.getCommandsCount > 0) Some(proto.getCommandsList.toList.map { _.getName }) else None
    List(
      proto.access.asParam(ac => sql.access === ac.getNumber),
      proto.user.asParam(sql.agent === Some(_)),
      commandsListOption.map(names => sql.id in findAccessesByCommandNames(names)))
  }

  private def findAccessesByCommandNames(names: List[String]) = {
    from(ApplicationSchema.commandToBlocks, ApplicationSchema.commands)((selectJoin, cmd) =>
      where(selectJoin.commandId === cmd.id and (cmd.id in CommandModel.findIdsByNames(names)))
        select (selectJoin.accessId)).distinct
  }

  def createModelEntry(proto: AccessProto): AccessModel = {
    new AccessModel(
      proto.getAccess.getNumber,
      proto.expireTime.map(_ + System.currentTimeMillis),
      proto.user)
  }

  def isModified(entry: AccessModel, existing: AccessModel): Boolean = {
    entry.access != existing.access || entry.agent != entry.agent || entry.expireTime != entry.expireTime
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
    entry.agent.foreach(b.setUser(_))

    b.build
  }
}