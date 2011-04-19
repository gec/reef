package org.totalgrid.reef.api.request.builders

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.totalgrid.reef.proto.Commands.{ CommandRequest, UserCommandRequest, CommandAccess }
import org.totalgrid.reef.proto.Model.{ ReefUUID, Command }

object UserCommandRequestBuilders {

  def getForUid(uid: ReefUUID) = UserCommandRequest.newBuilder.setUuid(uid).build

  def getStatus(request: UserCommandRequest) = UserCommandRequest.newBuilder.setUuid(request.getUuid).build

  def executeControl(command: Command): UserCommandRequest = executeControl(command.getName)
  def executeControl(command: String): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setName(command).setType(CommandRequest.ValType.NONE)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

  def executeSetpoint(command: Command, value: Int): UserCommandRequest = executeSetpoint(command.getName, value)
  def executeSetpoint(command: String, value: Int): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setName(command).setType(CommandRequest.ValType.INT).setIntVal(value)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

  def executeSetpoint(command: Command, value: Double): UserCommandRequest = executeSetpoint(command.getName, value)
  def executeSetpoint(command: String, value: Double): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setName(command).setType(CommandRequest.ValType.DOUBLE).setDoubleVal(value)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

}