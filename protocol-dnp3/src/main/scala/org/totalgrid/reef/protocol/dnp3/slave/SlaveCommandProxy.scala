/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.protocol.dnp3.slave

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Model.{ CommandType, Command }
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.service.proto.Mapping.{ CommandMap, CommandType => ProtoCommandType, IndexMapping }
import org.totalgrid.reef.protocol.dnp3._
import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus => ProtoCommandStatus, CommandResult => ProtoCommandResult }
import org.totalgrid.reef.protocol.dnp3.master.DNPTranslator
import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.client.sapi.rpc.CommandService

class SlaveCommandProxy(service: CommandService, mapping: IndexMapping)
    extends ICommandAcceptor with Logging {

  private case class Index(isSetpoint: Boolean, index: Long) {
    override def toString() = (if (isSetpoint) "setpoint" else "control") + " index: " + index
  }

  private val commandMap = mapping.getCommandmapList.toList.map { e =>
    Index(isSetpoint(e.getType), e.getIndex) -> e
  }.toMap

  private def isSetpoint(typ: ProtoCommandType) = typ match {
    case ProtoCommandType.SETPOINT => true
    case _ => false
  }

  private def handleCommandResult(result: ProtoCommandResult): ProtoCommandStatus = {
    if (result.getStatus != ProtoCommandStatus.SUCCESS) {
      logger.warn("Proxied command failed: " + result.getStatus + " msg: " + result.getErrorMessage)
    }
    result.getStatus
  }

  final override def AcceptCommand(obj: BinaryOutput, index: Long, seq: Int, accept: IResponseAcceptor) = {
    handleCommand(Index(false, index), seq, accept) { (command, config) =>
      val rawCode = DNPTranslator.translateCommandType(config.getType)
      if (obj.GetCode() != rawCode) {
        logger.warn("Got wrong command type for command: " + command.getName + " got: " + obj.GetCode() + " not: " + rawCode)
        ProtoCommandStatus.FORMAT_ERROR
      } else {
        handleCommandResult(service.executeCommandAsControl(command).await)
      }
    }
  }

  final override def AcceptCommand(obj: Setpoint, index: Long, seq: Int, accept: IResponseAcceptor) = {
    handleCommand(Index(true, index), seq, accept) { (command, config) =>
      import SetpointEncodingType._
      obj.GetOptimalEncodingType() match {
        case SPET_AUTO_DOUBLE | SPET_DOUBLE | SPET_FLOAT | SPET_AUTO_INT | SPET_INT16 | SPET_INT32 =>
          fireProxiedRequest(command, config, obj.GetValue())
        case _ =>
          logger.error("Unknown setpoint encoding type: " + obj.GetOptimalEncodingType())
          ProtoCommandStatus.FORMAT_ERROR
      }
    }
  }

  /**
   * we need to fire the command with the correct local type which we get from the command.getType
   */
  private def fireProxiedRequest(command: Command, config: CommandMap, value: Double): ProtoCommandStatus = {

    // We can scale the input if we are using an old style "fixed point" scaling of 100 or 1000 times the value
    // we really want for our local command
    val scaledValue = if (config.hasScaling) value * config.getScaling else value

    command.getType match {
      case CommandType.SETPOINT_DOUBLE => handleCommandResult(service.executeCommandAsSetpoint(command, scaledValue).await)
      case CommandType.SETPOINT_INT => handleCommandResult(service.executeCommandAsSetpoint(command, scaledValue.toInt).await)
      case _ =>
        logger.error("Unhandled command type: " + command.getType)
        ProtoCommandStatus.FORMAT_ERROR
    }

  }

  private def proxyCommandRequest(commandMapping: CommandMap, executeCommand: (Command, CommandMap) => ProtoCommandStatus): ProtoCommandStatus = {
    try {
      val command = service.getCommandByName(commandMapping.getCommandName).await
      val lock = service.createCommandExecutionLock(command).await
      try {
        executeCommand(command, commandMapping)
      } catch {
        case ex: ReefServiceException =>
          logger.warn("Error trying to proxy command: " + commandMapping.getCommandName + " - " + ex.getMessage, ex)
          ProtoCommandStatus.HARDWARE_ERROR
      } finally {
        service.deleteCommandLock(lock).await
      }
    } catch {
      case rse: ReefServiceException =>
        logger.error("Error locking remote command: " + commandMapping.getCommandName + " msg: " + rse.getMessage, rse)
        ProtoCommandStatus.NO_SELECT
    }
  }

  private def handleCommand(index: Index, seq: Int, accept: IResponseAcceptor)(executeCommand: (Command, CommandMap) => ProtoCommandStatus) = {
    val commandStatus = commandMap.get(index) match {
      case None =>
        logger.warn("Got unknown command request: " + index)
        ProtoCommandStatus.NOT_SUPPORTED
      case Some(commandMapping) =>
        proxyCommandRequest(commandMapping, executeCommand)
    }
    val response = new CommandResponse(DNPTranslator.translateCommandStatus(commandStatus))
    accept.AcceptResponse(response, seq)
  }

}