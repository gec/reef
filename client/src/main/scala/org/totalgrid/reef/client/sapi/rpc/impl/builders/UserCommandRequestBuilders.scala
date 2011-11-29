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
package org.totalgrid.reef.client.sapi.rpc.impl.builders

import org.totalgrid.reef.proto.Commands.{ CommandRequest, UserCommandRequest, CommandAccess }
import org.totalgrid.reef.proto.Model.{ ReefID, ReefUUID, Command }

object UserCommandRequestBuilders {

  def getForId(id: String) = UserCommandRequest.newBuilder.setId(ReefID.newBuilder.setValue(id)).build
  def getForCommand(command: Command) = {
    val cr = CommandRequest.newBuilder.setCommand(command)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

  def getStatus(request: UserCommandRequest) = UserCommandRequest.newBuilder.setId(request.getId).build

  def executeControl(command: Command): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setCommand(command).setType(CommandRequest.ValType.NONE)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

  def executeSetpoint(command: Command, value: Int): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setCommand(command).setType(CommandRequest.ValType.INT).setIntVal(value)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

  def executeSetpoint(command: Command, value: Double): UserCommandRequest = {
    val cr = CommandRequest.newBuilder.setCommand(command).setType(CommandRequest.ValType.DOUBLE).setDoubleVal(value)
    UserCommandRequest.newBuilder.setCommandRequest(cr).build
  }

}