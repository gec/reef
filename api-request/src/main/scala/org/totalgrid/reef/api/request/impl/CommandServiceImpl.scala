package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.proto.Commands.{ UserCommandRequest, CommandStatus, CommandAccess }
import org.totalgrid.reef.proto.Model.Command
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.request.builders.{ CommandRequestBuilders, UserCommandRequestBuilders, CommandAccessRequestBuilders }
import org.totalgrid.reef.api.ServiceTypes
import org.totalgrid.reef.api.request.{ ReefUUID, CommandService }

trait CommandServiceImpl extends ReefServiceBaseClass with CommandService {

  def createCommandExecutionLock(id: Command): CommandAccess = createCommandExecutionLock(id :: Nil)
  def createCommandExecutionLock(ids: java.util.List[Command]): CommandAccess = {
    ops.putOneOrThrow(CommandAccessRequestBuilders.allowAccessForCommands(ids))
  }

  def deleteCommandLock(uuid: ReefUUID): CommandAccess = {
    ops.deleteOneOrThrow(CommandAccessRequestBuilders.getForUid(uuid.getUuid))
  }
  def deleteCommandLock(ca: CommandAccess): CommandAccess = {
    ops.deleteOneOrThrow(CommandAccessRequestBuilders.getForUid(ca.getUid))
  }

  def clearCommandLocks(): java.util.List[CommandAccess] = {
    ops.deleteOrThrow(CommandAccessRequestBuilders.getAll)
  }

  def executeCommandAsControl(id: Command): CommandStatus = {
    val result = ops.putOneOrThrow(UserCommandRequestBuilders.executeControl(id))
    result.getStatus
  }

  def executeCommandAsSetpoint(id: Command, value: Double): CommandStatus = {
    val result = ops.putOneOrThrow(UserCommandRequestBuilders.executeSetpoint(id, value))
    result.getStatus
  }

  def createCommandDenialLock(ids: java.util.List[Command]): CommandAccess = {
    ops.putOneOrThrow(CommandAccessRequestBuilders.blockAccessForCommands(ids))
  }

  def getCommandLocks(): java.util.List[CommandAccess] = {
    ops.getOrThrow(CommandAccessRequestBuilders.getAll)
  }

  def getCommandLockOnCommand(id: Command): CommandAccess = {
    ops.getOne(CommandAccessRequestBuilders.getByCommand(id)) match {
      case ServiceTypes.SingleSuccess(status, lock) => lock
      case ServiceTypes.Failure(status, str) => null
    }
  }

  def getCommandLocksOnCommands(ids: java.util.List[Command]): java.util.List[CommandAccess] = {
    ops.getOrThrow(CommandAccessRequestBuilders.getByCommands(ids))
  }

  def getCommandHistory(): java.util.List[UserCommandRequest] = {
    ops.getOrThrow(UserCommandRequestBuilders.getForUid("*"))
  }

  def getCommands(): java.util.List[Command] = {
    ops.getOrThrow(CommandRequestBuilders.getAll)
  }
}