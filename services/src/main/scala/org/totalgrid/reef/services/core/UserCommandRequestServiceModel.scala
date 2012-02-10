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
import org.totalgrid.reef.client.service.proto.Commands.{ CommandResult, CommandStatus, CommandRequest, UserCommandRequest }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.models.UUIDConversions._

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.models.{ ApplicationSchema, Command => FepCommandModel, UserCommandModel }
import org.totalgrid.reef.client.service.proto.Commands.CommandRequest.ValType
import org.totalgrid.reef.client.service.proto.Model.CommandType
import org.totalgrid.reef.event.{ SystemEventSink, EventType }
import org.squeryl.dsl.fsm.SelectState
import org.squeryl.dsl.QueryYield
import org.squeryl.dsl.ast.OrderByArg

class UserCommandRequestServiceModel(
  accessModel: CommandLockServiceModel)
    extends SquerylServiceModel[Long, UserCommandRequest, UserCommandModel]
    with EventedServiceModel[UserCommandRequest, UserCommandModel]
    with UserCommandRequestConversion
    with ServiceModelSystemEventPublisher {

  val table = ApplicationSchema.userRequests

  def findExecuting = table.where(t => t.status === CommandStatus.EXECUTING.getNumber).toList
  def findExpired = table.where(t => t.status === CommandStatus.EXECUTING.getNumber and (t.expireTime lte System.currentTimeMillis)).toList

  def findAndMarkExpired(context: RequestContext) = markExpired(context, findExpired)

  def markExpired(context: RequestContext, expired: List[UserCommandModel]) = {
    def isExpired(cmd: UserCommandModel) =
      (cmd.expireTime < System.currentTimeMillis) &&
        (cmd.status != CommandStatus.TIMEOUT.getNumber)

    exclusiveUpdate(context, expired, isExpired _) { cmds =>
      cmds.map(cmd => { cmd.status = CommandStatus.TIMEOUT.getNumber; cmd })
    }
  }

  def markCompleted(context: RequestContext, cmd: UserCommandModel, status: CommandStatus) = {
    def isExecuting(cmd: UserCommandModel) = cmd.status == CommandStatus.EXECUTING.getNumber

    exclusiveUpdate(context, cmd, isExecuting _) { cmd =>
      cmd.status = status.getNumber
      cmd
    }
  }

  def issueCommand(context: RequestContext, command: String, corrId: String, user: String, timeout: Long, cmdRequest: CommandRequest): UserCommandModel = {
    issueRequest(context, findCommand(command), corrId, user, timeout, cmdRequest)
  }

  def findCommand(command: String) = {
    // Search for the requested command and validate it exists
    val cmds = FepCommandModel.findByNames(command :: Nil).toList
    if (cmds.length != 1)
      throw new BadRequestException("Command does not exist: " + cmds.length)

    cmds.head
  }

  def issueRequest(context: RequestContext, cmd: FepCommandModel, corrolationId: String, user: String, timeout: Long, cmdRequest: CommandRequest, atTime: Long = System.currentTimeMillis): UserCommandModel = {
    if (accessModel.userHasSelect(cmd, user, atTime)) {

      def checkCommandType(modelType: Int, requestType: CommandType) {
        if (modelType != requestType.getNumber)
          throw new BadRequestException("Cannot execute command with type: " + CommandType.valueOf(modelType) + " with request of type: " + requestType)
      }

      val (code, valueArg) = cmdRequest._type match {
        case Some(ValType.NONE) | None =>
          checkCommandType(cmd.commandType, CommandType.CONTROL)
          (EventType.Scada.ControlExe, "value" -> "")
        case Some(ValType.INT) =>
          checkCommandType(cmd.commandType, CommandType.SETPOINT_INT)
          (EventType.Scada.UpdatedSetpoint, "value" -> cmdRequest.intVal.get)
        case Some(ValType.DOUBLE) =>
          checkCommandType(cmd.commandType, CommandType.SETPOINT_DOUBLE)
          (EventType.Scada.UpdatedSetpoint, "value" -> cmdRequest.doubleVal.get)
        case Some(ValType.STRING) =>
          checkCommandType(cmd.commandType, CommandType.SETPOINT_STRING)
          (EventType.Scada.UpdatedSetpoint, "value" -> cmdRequest.stringVal.get)
      }
      postSystemEvent(context, code, Some(cmd.entity.value), "command" -> cmd.entityName :: valueArg :: Nil)

      val expireTime = atTime + timeout
      val status = CommandStatus.EXECUTING.getNumber
      create(context, new UserCommandModel(cmd.id, corrolationId, user, status, expireTime, cmdRequest.toByteArray, None))

    } else {
      throw new BadRequestException("Command not selected")
    }
  }

  override def createFromProto(context: RequestContext, req: UserCommandRequest): UserCommandModel = {

    val user = context.getHeaders.userName getOrElse { throw new BadRequestException("User must be in header.") }

    val (id, cmdProto) = if (req.commandRequest.correlationId.isEmpty) {
      val cid = System.currentTimeMillis + "-" + user
      (cid, req.getCommandRequest.toBuilder.setCorrelationId(cid).build)
    } else {
      (req.getCommandRequest.getCorrelationId, req.getCommandRequest)
    }

    issueRequest(context,
      findCommand(req.getCommandRequest.getCommand.getName),
      id,
      user,
      req.getTimeoutMs,
      cmdProto)
  }

  override def updateFromProto(context: RequestContext, req: UserCommandRequest, existing: UserCommandModel): (UserCommandModel, Boolean) = {
    if (existing.status != CommandStatus.EXECUTING.getNumber)
      throw new BadRequestException("Current status was not executing on update, already: " + CommandStatus.valueOf(existing.status), Envelope.Status.NOT_ALLOWED)

    update(context, existing.copy(status = req.getStatus.getNumber), existing)
  }
}

trait UserCommandRequestConversion extends UniqueAndSearchQueryable[UserCommandRequest, UserCommandModel] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  override def getOrdering[R](select: SelectState[R], sql: UserCommandModel): QueryYield[R] = select.orderBy(new OrderByArg(sql.id).desc)

  // don't need to resort list, getOrdering has already sorted them correctly
  def sortResults(list: List[UserCommandRequest]) = list

  def getRoutingKey(req: UserCommandRequest) = ProtoRoutingKeys.generateRoutingKey {
    req.id.value ::
      req.user ::
      req.commandRequest.command.name ::
      req.commandRequest.correlationId :: Nil
  }

  // Relies on implicit to combine LogicalBooleans
  def uniqueQuery(proto: UserCommandRequest, sql: UserCommandModel) = {
    List(
      proto.id.value.asParam(sql.id === _.toLong),
      proto.commandRequest.correlationId.asParam(sql.corrolationId === _))
  }

  def searchQuery(proto: UserCommandRequest, sql: UserCommandModel) = {
    List(
      proto.user.map(sql.agent === _),
      proto.status.map(st => sql.status === st.getNumber),
      proto.commandRequest.command.name.asParam(cname => sql.commandId in FepCommandModel.findIdsByNames(cname :: Nil)))
  }

  def isModified(existing: UserCommandModel, updated: UserCommandModel): Boolean = {
    existing.status != updated.status || existing.agent != updated.agent
  }

  def convertToProto(entry: UserCommandModel): UserCommandRequest = {
    val b = UserCommandRequest.newBuilder
      .setId(makeId(entry))
      .setUser(entry.agent)
      .setStatus(CommandStatus.valueOf(entry.status))
      .setCommandRequest(CommandRequest.parseFrom(entry.commandProto))

    val result = CommandResult.newBuilder.setStatus(CommandStatus.valueOf(entry.status))
    entry.errorMessage.foreach { msg =>
      b.setErrorMessage(msg)
      result.setErrorMessage(msg)
    }

    b.setResult(result)

    b.build
  }
}

