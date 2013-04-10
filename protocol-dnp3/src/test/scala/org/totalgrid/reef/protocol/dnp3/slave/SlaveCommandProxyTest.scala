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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.mockito.{ Mockito, Matchers }
import org.totalgrid.reef.client.service.proto.Model.{ Command, CommandType => ModelCommandType }
import org.totalgrid.reef.client.service.proto.Mapping._
import org.totalgrid.reef.client.service.proto.Commands.{ CommandResult, CommandStatus => ProtoCommandStatus, CommandLock }
import org.totalgrid.reef.client.sapi.client.ServiceTestHelpers._
import org.totalgrid.reef.protocol.dnp3._
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.sapi.rpc.CommandService

@RunWith(classOf[JUnitRunner])
class SlaveCommandProxyTest extends FunSuite with ShouldMatchers {

  val commandName = "TestCommand"

  test("Only handles configured controls") {

    val commandService = getWorkingCommandService(commandName, ProtoCommandStatus.SUCCESS, ModelCommandType.CONTROL)

    val mapping = makeMappings(
      makeMapping(commandName, 0, CommandType.PULSE),
      makeMapping(commandName, 1, CommandType.PULSE_TRIP),
      makeMapping(commandName, 0, CommandType.SETPOINT),
      makeMapping(commandName, 2, CommandType.SETPOINT))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    // check controls with valid indices
    tryControl(proxy, 99, 0, ControlCode.CC_PULSE, CommandStatus.CS_SUCCESS)
    tryControl(proxy, 150, 1, ControlCode.CC_PULSE_TRIP, CommandStatus.CS_SUCCESS)

    // try wrong format controls
    tryControl(proxy, 150, 0, ControlCode.CC_LATCH_OFF, CommandStatus.CS_FORMAT_ERROR)
    tryControl(proxy, 150, 1, ControlCode.CC_LATCH_OFF, CommandStatus.CS_FORMAT_ERROR)

    // try a few controls on invalid indices
    tryControl(proxy, 166, 10, ControlCode.CC_PULSE, CommandStatus.CS_NOT_SUPPORTED)
    tryControl(proxy, 166, 2, ControlCode.CC_LATCH_OFF, CommandStatus.CS_NOT_SUPPORTED)
    tryControl(proxy, 166, 5, ControlCode.CC_LATCH_ON, CommandStatus.CS_NOT_SUPPORTED)
  }

  test("Only handles configured setpoints") {

    val commandService = getWorkingCommandService(commandName, ProtoCommandStatus.SUCCESS, ModelCommandType.SETPOINT_DOUBLE)

    val mapping = makeMappings(
      makeMapping(commandName, 0, CommandType.PULSE),
      makeMapping(commandName, 1, CommandType.PULSE_TRIP),
      makeMapping(commandName, 0, CommandType.SETPOINT),
      makeMapping(commandName, 2, CommandType.SETPOINT))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    // check setpoints (notice indices overlap with binaries)
    trySetpoint(proxy, 1, 0, 100.1, CommandStatus.CS_SUCCESS)
    trySetpoint(proxy, 66, 2, 55.0, CommandStatus.CS_SUCCESS)

    // try bad indicies for setpoint
    trySetpoint(proxy, 77, 100, 55, CommandStatus.CS_NOT_SUPPORTED)
  }

  test("Only handles configured setpoints (int)") {

    val commandService = getWorkingCommandService(commandName, ProtoCommandStatus.SUCCESS, ModelCommandType.SETPOINT_INT)

    val mapping = makeMappings(
      makeMapping(commandName, 0, CommandType.PULSE),
      makeMapping(commandName, 1, CommandType.PULSE_TRIP),
      makeMapping(commandName, 0, CommandType.SETPOINT),
      makeMapping(commandName, 2, CommandType.SETPOINT))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    // check setpoints (notice indices overlap with binaries)
    trySetpoint(proxy, 1, 0, 100.1, CommandStatus.CS_SUCCESS)
    trySetpoint(proxy, 66, 2, 55.0, CommandStatus.CS_SUCCESS)

    // try bad indicies for setpoint
    trySetpoint(proxy, 77, 100, 55, CommandStatus.CS_NOT_SUPPORTED)
  }

  test("Handles missing command in services") {
    val commandService = getMissingCommandService()

    val mapping = makeMappings(makeMapping(commandName, 0, CommandType.PULSE))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    tryControl(proxy, 99, 0, ControlCode.CC_PULSE, CommandStatus.CS_NO_SELECT)
  }

  test("Handles unavailable lock") {
    val commandService = getNoLockCommandService(commandName)

    val mapping = makeMappings(makeMapping(commandName, 0, CommandType.PULSE))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    tryControl(proxy, 99, 0, ControlCode.CC_PULSE, CommandStatus.CS_NO_SELECT)
  }

  test("Handles execution failure") {
    val commandService = getExecutionFailureService(commandName)

    val mapping = makeMappings(makeMapping(commandName, 0, CommandType.PULSE))
    val proxy = new SlaveCommandProxy(commandService, mapping)

    tryControl(proxy, 99, 0, ControlCode.CC_PULSE, CommandStatus.CS_HARDWARE_ERROR)
  }

  def tryControl(proxy: ICommandAcceptor, seq: Int, index: Int, code: ControlCode, status: CommandStatus) = {
    checkResponse(seq, status) { response =>
      proxy.AcceptCommand(new BinaryOutput(code), index, seq, response)
    }
  }

  def trySetpoint(proxy: ICommandAcceptor, seq: Int, index: Int, value: Double, status: CommandStatus) = {
    checkResponse(seq, status) { response =>
      proxy.AcceptCommand(new Setpoint(value), index, seq, response)
    }
  }

  def trySetpoint(proxy: ICommandAcceptor, seq: Int, index: Int, value: Int, status: CommandStatus) = {
    checkResponse(seq, status) { response =>
      proxy.AcceptCommand(new Setpoint(value), index, seq, response)
    }
  }

  def checkResponse[A](seq: Int, status: CommandStatus)(func: IResponseAcceptor => A) {
    var called = false
    val response = new IResponseAcceptor() {
      override def AcceptResponse(cmdResponse: CommandResponse, seq: Int) {
        seq should equal(seq)
        cmdResponse.getMResult should equal(status)
        called = true
      }
    }
    func(response)
    called should equal(true)
  }

  def getWorkingCommandService(commandName: String, _result: ProtoCommandStatus, cmdType: ModelCommandType) = {
    val commandService = Mockito.mock(classOf[CommandService], new MockitoStubbedOnly)
    val resultantCommand = success(Command.newBuilder.setName(commandName).setType(cmdType).build)
    val lock = success(CommandLock.newBuilder.build)
    val result = success(CommandResult.newBuilder.setStatus(_result).build)
    Mockito.doReturn(resultantCommand).when(commandService).getCommandByName(commandName)
    Mockito.doReturn(lock).when(commandService).createCommandExecutionLock(resultantCommand.await)
    Mockito.doReturn(lock).when(commandService).deleteCommandLock(lock.await)
    // we will only accept calls to the service that match the type of the command, anything else is a failure
    cmdType match {
      case ModelCommandType.SETPOINT_DOUBLE =>
        Mockito.doReturn(result).when(commandService).executeCommandAsSetpoint(Matchers.eq(resultantCommand.await), Matchers.anyDouble)
      case ModelCommandType.SETPOINT_INT =>
        Mockito.doReturn(result).when(commandService).executeCommandAsSetpoint(Matchers.eq(resultantCommand.await), Matchers.anyInt)
      case ModelCommandType.CONTROL =>
        Mockito.doReturn(result).when(commandService).executeCommandAsControl(resultantCommand.await)
    }
    commandService
  }
  def getMissingCommandService() = {
    val commandService = Mockito.mock(classOf[CommandService], new MockitoStubbedOnly)
    val fail = failure(new BadRequestException("Command not found"))
    Mockito.doReturn(fail).when(commandService).getCommandByName(Matchers.anyString)
    commandService
  }
  def getNoLockCommandService(commandName: String) = {
    val commandService = Mockito.mock(classOf[CommandService], new MockitoStubbedOnly)
    val resultantCommand = success(Command.newBuilder.setName(commandName).build)
    val fail = failure(new BadRequestException("Can't lock command"))
    Mockito.doReturn(resultantCommand).when(commandService).getCommandByName(commandName)
    Mockito.doReturn(fail).when(commandService).createCommandExecutionLock(resultantCommand.await)
    commandService
  }

  def getExecutionFailureService(commandName: String) = {
    val commandService = Mockito.mock(classOf[CommandService], new MockitoStubbedOnly)
    val resultantCommand = success(Command.newBuilder.setName(commandName).build)
    val lock = success(CommandLock.newBuilder.build)
    val fail = failure(new BadRequestException("Can't execute command"))
    Mockito.doReturn(resultantCommand).when(commandService).getCommandByName(commandName)
    Mockito.doReturn(lock).when(commandService).createCommandExecutionLock(resultantCommand.await)
    Mockito.doReturn(lock).when(commandService).deleteCommandLock(lock.await)
    Mockito.doReturn(fail).when(commandService).executeCommandAsControl(resultantCommand.await)
    commandService
  }

  def makeMapping(name: String, index: Int, typ: CommandType) =
    CommandMap.newBuilder.setIndex(index).setCommandName(name).setType(typ).build

  def makeMappings(list: CommandMap*) = {
    val map = IndexMapping.newBuilder
    map.setDeviceId("test")
    list.foreach { map.addCommandmap(_) }
    map.build
  }
}