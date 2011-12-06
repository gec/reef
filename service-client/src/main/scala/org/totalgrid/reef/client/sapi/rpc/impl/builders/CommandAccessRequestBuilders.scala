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

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Commands.CommandLock
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, Command }
import scala.None

object CommandLockRequestBuilders {

  def allowAccessForCommand(command: Command): CommandLock =
    CommandLock.newBuilder.addCommands(command).setAccess(CommandLock.AccessMode.ALLOWED).build

  def blockAccessForCommand(command: Command): CommandLock =
    CommandLock.newBuilder.addCommands(command).setAccess(CommandLock.AccessMode.BLOCKED).build

  def allowAccessForCommands(commands: List[Command], expirationTimeMilli: Option[Long]): CommandLock = {
    val access = CommandLock.newBuilder.addAllCommands(commands).setAccess(CommandLock.AccessMode.ALLOWED)
    expirationTimeMilli match {
      case Some(time) => access.setExpireTime(time)
      case None =>
    }
    access.build
  }

  def allowAccessForCommands(commands: java.util.List[Command]): CommandLock = allowAccessForCommands(commands, None)

  def allowAccessForCommands(commands: java.util.List[Command], expirationTimeMilli: Option[Long]): CommandLock = allowAccessForCommands(commands.toList,
    expirationTimeMilli)

  def blockAccessForCommands(commands: List[Command]): CommandLock = {
    CommandLock.newBuilder.addAllCommands(commands).setAccess(CommandLock.AccessMode.BLOCKED).build
  }
  def blockAccessForCommands(commands: java.util.List[Command]): CommandLock = blockAccessForCommands(commands.toList)

  def getAll() = CommandLock.newBuilder.setId(ReefID.newBuilder.setValue("*")).build
  def getByCommand(command: Command) = CommandLock.newBuilder.addCommands(command).build
  def getByCommands(commands: java.util.List[Command]) = CommandLock.newBuilder.addAllCommands(commands).build

  def getForId(id: ReefID) = CommandLock.newBuilder.setId(id).build

  def getForUser(user: String) = CommandLock.newBuilder.setUser(user).build

  def deleteById(id: String) = CommandLock.newBuilder.setId(ReefID.newBuilder.setValue(id)).build
  def delete(cmd: CommandLock) = CommandLock.newBuilder.setId(cmd.getId).build
}