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

import org.squeryl.{ Table, Query }

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.proto.Model.{ Command => FepCommandProto }
import org.totalgrid.reef.proto.Commands.{ CommandAccess => AccessProto }
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess._
import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.api.{ Envelope, BadRequestException, UnauthorizedException }
import org.totalgrid.reef.models.{ ApplicationSchema, CommandAccessModel => AccessModel, Command => CommandModel, CommandBlockJoin }
import collection.mutable.Set

class CommandAccessServiceModelFactory(pub: ServiceEventPublishers, commands: ModelFactory[CommandServiceModel])
    extends BasicModelFactory[AccessProto, CommandAccessServiceModel](pub, classOf[AccessProto]) {

  def model = new CommandAccessServiceModel(subHandler, commands.model)
  def model(commandModel: CommandServiceModel) = new CommandAccessServiceModel(subHandler, commandModel)
}

class CommandAccessServiceModel(protected val subHandler: ServiceSubscriptionHandler, commandModel: CommandServiceModel)
    extends SquerylServiceModel[AccessProto, AccessModel]
    with EventedServiceModel[AccessProto, AccessModel]
    with CommandAccessConversion {

  import org.squeryl.PrimitiveTypeMode._

  link(commandModel)

  val table = ApplicationSchema.commandAccess

  override def createFromProto(req: AccessProto): AccessModel = {
    import org.totalgrid.reef.services.ServiceProviderHeaders._

    val user = env.userName getOrElse { throw new BadRequestException("User must be in header.") }
    req.user.foreach { u => if (user != u) throw new BadRequestException("User name in request doesn't match any auth token owners, correct name or leave blank.") }

    if (req.getAccess == AccessProto.AccessMode.ALLOWED) {

      // process the time here. On requests the time is relative, on responses it is 
      // an absolute UTC time
      val time = req.expireTime match {
        case Some(t) => Some(t + System.currentTimeMillis)
        case None => None
      }
      // Do the select on the model, given the requested list of commands
      selectCommands(user, time, req.getCommandsList.toList)
    } else {
      blockCommands(user, req.getCommandsList.toList)
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

  def areAnyBlocked(commands: List[String]): Boolean = {
    areAnyBlockedById(CommandModel.findByNames(commands).map { _.id }.toList)
  }
  def areAnyBlockedById(ids: List[Long]): Boolean = {
    val joinTable = ApplicationSchema.commandToBlocks
    val blockInt = AccessProto.AccessMode.BLOCKED.getNumber

    val lookup = from(joinTable, table)((join, acc) =>
      where(join.commandId in ids and
        join.accessId === acc.id and (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
        select (acc))

    lookup.size != 0
  }

  protected def addEntryForAll(entry: AccessModel, cmds: List[CommandModel]) = {
    try {
      commandModel.exclusiveUpdate(cmds.toList, (cmd: CommandModel) => cmd.lastSelectId != Some(entry.id)) { cmdList =>
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
        // TODO: useful statistic
        throw new UnauthorizedException("Some or all commands selected")
    }
  }

  def blockCommands(user: String, commands: List[String]): AccessModel = {
    val foundCommands = CommandModel.findByNames(commands)

    if (foundCommands.size != commands.size) {
      val notFound: Set[String] = Set.empty[String]
      notFound.addAll(commands)
      commands.map(notFound.remove(_))
      throw new BadRequestException("Commands not found: " + commands)
    }

    val accEntry = create(new AccessModel(AccessProto.AccessMode.BLOCKED.getNumber, None, Some(user)))
    addEntryForAll(accEntry, foundCommands.toList)
    accEntry
  }

  def selectCommands(user: String, expireTime: Option[Long], commands: List[String]): AccessModel = {
    val cmds = CommandModel.findByNames(commands)

    if (cmds.size != commands.size)
      throw new BadRequestException("Commands not found")

    val cmdIds = from(cmds)(t => select(t.id))
    if (areAnyBlockedById(cmdIds.toList))
      throw new UnauthorizedException("One or more commands are blocked")

    val accEntry = create(new AccessModel(AccessProto.AccessMode.ALLOWED.getNumber, expireTime, Some(user)))
    addEntryForAll(accEntry, cmds.toList)
    accEntry
  }

  def removeAccess(access: AccessModel): Unit = {
    delete(access)
    ApplicationSchema.commandToBlocks.deleteWhere(t => t.accessId === access.id)

    val cmds = commandModel.table.where(cmd => cmd.lastSelectId === access.id).toList
    if (cmds.length > 0) {

      // Remove last select (since it doesn't refer to anything real) on all commands
      commandModel.exclusiveUpdate(cmds, (cmd: CommandModel) => cmd.lastSelectId == Some(access.id)) { cmdList =>
        cmdList.map { cmd =>
          cmd.lastSelectId = None
          cmd
        }
      }
    }
  }
}

trait CommandAccessConversion
    extends MessageModelConversion[AccessProto, AccessModel]
    with UniqueAndSearchQueryable[AccessProto, AccessModel] {

  import org.squeryl.PrimitiveTypeMode._
  import AccessProto._
  import org.totalgrid.reef.proto.OptionalProtos._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  def getRoutingKey(req: AccessProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uid ::
      req.access ::
      req.user :: Nil
  }

  def uniqueQuery(proto: AccessProto, sql: AccessModel) = {
    List(
      proto.uid.asParam(uid => sql.id === uid.toLong))
  }

  def searchQuery(proto: AccessProto, sql: AccessModel) = {
    val commandsListOption = if (proto.getCommandsCount > 0) Some(proto.getCommandsList.toList) else None
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
      .setUid(makeUid(entry))
      .addAllCommands(entry.commands.map(cmd => cmd.entityName))
      .setAccess(AccessMode.valueOf(entry.access))

    // optional sql fields
    entry.expireTime.foreach(b.setExpireTime(_))
    entry.agent.foreach(b.setUser(_))

    b.build
  }
}