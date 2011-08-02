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
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.japi.{ BadRequestException, Envelope }
import org.totalgrid.reef.models.{ ApplicationSchema, Command => FepCommandModel, UserCommandModel }
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.proto.Commands.CommandRequest.ValType
import org.totalgrid.reef.event.{ SystemEventSink, EventType }

class UserCommandRequestServiceModelFactory(
  dependencies: ServiceDependencies,
  accessFac: ModelFactory[CommandAccessServiceModel])
    extends BasicModelFactory[UserCommandRequest, UserCommandRequestServiceModel](dependencies, classOf[UserCommandRequest]) {

  def model = {
    val accessModel = accessFac.model
    val m = new UserCommandRequestServiceModel(subHandler, accessModel, dependencies.eventSink)

    m

  }
  def model(accessModel: CommandAccessServiceModel) =
    new UserCommandRequestServiceModel(subHandler, accessModel, dependencies.eventSink)

}

class UserCommandRequestServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  accessModel: CommandAccessServiceModel,
  val eventSink: SystemEventSink)
    extends SquerylServiceModel[UserCommandRequest, UserCommandModel]
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

      // TODO: move command SystemEvent publishing into async issuer
      val (code, valueArg) = cmdRequest._type match {
        case Some(ValType.NONE) | None => (EventType.Scada.ControlExe, "value" -> "")
        case Some(ValType.INT) => (EventType.Scada.UpdatedSetpoint, "value" -> cmdRequest.intVal.get)
        case Some(ValType.DOUBLE) => (EventType.Scada.UpdatedSetpoint, "value" -> cmdRequest.doubleVal.get)
      }
      postSystemEvent(context, code, args = "command" -> cmd.entityName :: valueArg :: Nil)

      val expireTime = atTime + timeout
      val status = CommandStatus.EXECUTING.getNumber
      create(context, new UserCommandModel(cmd.id, corrolationId, user, status, expireTime, cmdRequest.toByteArray))

    } else {
      throw new BadRequestException("Command not selected")
    }
  }

  override def createFromProto(context: RequestContext, req: UserCommandRequest): UserCommandModel = {

    val user = context.headers.userName getOrElse { throw new BadRequestException("User must be in header.") }

    val (id, cmdProto) = if (req.commandRequest.correlationId.isEmpty) {
      val cid = System.currentTimeMillis + "-" + user
      (cid, req.getCommandRequest.toBuilder.setCorrelationId(cid).build)
    } else {
      (req.getCommandRequest.getCorrelationId, req.getCommandRequest)
    }

    issueRequest(context,
      findCommand(req.getCommandRequest.getName),
      id,
      user,
      req.getTimeoutMs,
      cmdProto)
  }

  override def updateFromProto(context: RequestContext, req: UserCommandRequest, existing: UserCommandModel): (UserCommandModel, Boolean) = {
    if (existing.status != CommandStatus.EXECUTING.getNumber)
      throw new BadRequestException("Current status was not executing on update", Envelope.Status.NOT_ALLOWED)

    update(context, existing.copy(status = req.getStatus.getNumber), existing)
  }
}

trait UserCommandRequestConversion extends MessageModelConversion[UserCommandRequest, UserCommandModel] with UniqueAndSearchQueryable[UserCommandRequest, UserCommandModel] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._ // Implicit squeryl list -> query conversion

  def getRoutingKey(req: UserCommandRequest) = ProtoRoutingKeys.generateRoutingKey {
    req.uid ::
      req.user ::
      req.commandRequest.name ::
      req.commandRequest.correlationId :: Nil
  }

  // Relies on implicit to combine LogicalBooleans
  def uniqueQuery(proto: UserCommandRequest, sql: UserCommandModel) = {
    List(
      proto.uid.asParam(sql.id === _.toLong),
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
      .setUid(makeUid(entry))
      .setUser(entry.agent)
      .setStatus(CommandStatus.valueOf(entry.status))
      .setCommandRequest(CommandRequest.parseFrom(entry.commandProto))
      .build
  }
}

