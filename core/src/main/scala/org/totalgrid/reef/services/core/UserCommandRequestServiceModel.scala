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

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest }
import org.totalgrid.reef.proto.Commands.CommandAccess
import org.totalgrid.reef.models.{ ApplicationSchema, Command => FepCommandModel }
import org.totalgrid.reef.models.{ UserCommandModel }

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.api.{ Envelope, BadRequestException }

class UserCommandRequestServiceModelFactory(pub: ServiceEventPublishers, commands: ModelFactory[CommandServiceModel], accessFac: ModelFactory[CommandAccessServiceModel])
    extends BasicModelFactory[UserCommandRequest, UserCommandRequestServiceModel](pub, classOf[UserCommandRequest]) {

  def model = new UserCommandRequestServiceModel(subHandler, commands.model, accessFac.model)
  def model(commandModel: CommandServiceModel, accessModel: CommandAccessServiceModel) = new UserCommandRequestServiceModel(subHandler, commandModel, accessModel)
}

class UserCommandRequestServiceModel(protected val subHandler: ServiceSubscriptionHandler, commandModel: CommandServiceModel, accessModel: CommandAccessServiceModel)
    extends SquerylServiceModel[UserCommandRequest, UserCommandModel]
    with EventedServiceModel[UserCommandRequest, UserCommandModel]
    with UserCommandRequestConversion {

  val table = ApplicationSchema.userRequests

  def findExecuting = table.where(t => t.status === CommandStatus.EXECUTING.getNumber).toList
  def findExpired = table.where(t => t.status === CommandStatus.EXECUTING.getNumber and (t.expireTime lte System.currentTimeMillis)).toList

  def findAndMarkExpired = markExpired(findExpired)

  def markExpired(expired: List[UserCommandModel]) = {
    def isExpired(cmd: UserCommandModel) =
      (cmd.expireTime < System.currentTimeMillis) &&
        (cmd.status != CommandStatus.TIMEOUT.getNumber)

    exclusiveUpdate(expired, isExpired _) { cmds =>
      cmds.map(cmd => { cmd.status = CommandStatus.TIMEOUT.getNumber; cmd })
    }
  }

  def markCompleted(cmd: UserCommandModel, status: CommandStatus) = {
    def isExecuting(cmd: UserCommandModel) = cmd.status == CommandStatus.EXECUTING.getNumber

    exclusiveUpdate(cmd, isExecuting _) { cmd =>
      cmd.status = status.getNumber
      cmd
    }
  }

  def issueCommand(command: String, corrId: String, user: String, timeout: Long, serializedCmd: Array[Byte]): UserCommandModel = {
    issueRequest(findCommand(command), corrId, user, timeout, serializedCmd)
  }

  def findCommand(command: String) = {
    // Search for the requested command and validate it exists
    val cmds = commandModel.table.where(t => t.name === command).toList
    if (cmds.length != 1)
      throw new BadRequestException("Command does not exist: " + cmds.length)

    cmds.head
  }

  def issueRequest(cmd: FepCommandModel, corrolationId: String, user: String, timeout: Long, serializedCmd: Array[Byte], atTime: Long = System.currentTimeMillis): UserCommandModel = {
    if (accessModel.userHasSelect(cmd, user, atTime)) {

      val expireTime = atTime + timeout
      val status = CommandStatus.EXECUTING.getNumber
      create(new UserCommandModel(cmd.id, corrolationId, user, status, expireTime, serializedCmd))

    } else {
      throw new BadRequestException("Command not selected")
    }
  }

  override def createFromProto(req: UserCommandRequest): UserCommandModel = {
    import org.totalgrid.reef.services.ServiceProviderHeaders._

    val user = env.userName getOrElse { throw new BadRequestException("User must be in header.") }

    val (id, serialized) = if (req.commandRequest.correlationId.isEmpty) {
      val cid = System.currentTimeMillis + "-" + user
      (cid, req.getCommandRequest.toBuilder.setCorrelationId(cid).build.toByteString.toByteArray)
    } else {
      (req.getCommandRequest.getCorrelationId, req.getCommandRequest.toByteString.toByteArray)
    }

    issueRequest(
      findCommand(req.getCommandRequest.getName),
      id,
      user,
      req.getTimeoutMs,
      serialized)
  }

  override def updateFromProto(req: UserCommandRequest, existing: UserCommandModel): (UserCommandModel, Boolean) = {
    if (existing.status != CommandStatus.EXECUTING.getNumber)
      throw new BadRequestException("Current status was not executing on update", Envelope.Status.NOT_ALLOWED)

    update(existing.copy(status = req.getStatus.getNumber), existing)
  }
}

trait UserCommandRequestConversion extends MessageModelConversion[UserCommandRequest, UserCommandModel] with UniqueAndSearchQueryable[UserCommandRequest, UserCommandModel] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  def getRoutingKey(req: UserCommandRequest) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.uuid ::
      req.user ::
      req.commandRequest.name ::
      req.commandRequest.correlationId :: Nil
  }

  // Relies on implicit to combine LogicalBooleans
  def uniqueQuery(proto: UserCommandRequest, sql: UserCommandModel) = {
    List(
      proto.uuid.uuid.asParam(sql.id === _.toLong),
      proto.commandRequest.correlationId.asParam(sql.corrolationId === _))
  }

  def searchQuery(proto: UserCommandRequest, sql: UserCommandModel) = {
    List(
      proto.user.map(sql.agent === _),
      proto.status.map(st => sql.status === st.getNumber))
  }

  def isModified(existing: UserCommandModel, updated: UserCommandModel): Boolean = {
    existing.status != updated.status || existing.agent != updated.agent
  }

  // TODO: remove createModelEntry from interface
  def createModelEntry(proto: UserCommandRequest): UserCommandModel = throw new Exception("not using interface")

  def convertToProto(entry: UserCommandModel): UserCommandRequest = {
    UserCommandRequest.newBuilder
      .setUuid(makeUuid(entry))
      .setUser(entry.agent)
      .setStatus(CommandStatus.valueOf(entry.status))
      .setCommandRequest(CommandRequest.parseFrom(entry.commandProto))
      .build
  }
}

