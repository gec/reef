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
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.CommandAccess
import org.totalgrid.reef.proto.Model.{ ReefUUID, Command }

object CommandAccessRequestBuilders {

  def allowAccessForCommand(command: Command): CommandAccess = allowAccessForCommandName(command.getName)
  def allowAccessForCommandName(command: String): CommandAccess = {
    CommandAccess.newBuilder.addCommands(command).setAccess(CommandAccess.AccessMode.ALLOWED).build
  }

  def blockAccessForCommand(command: Command): CommandAccess = blockAccessForCommandName(command.getName)
  def blockAccessForCommandName(command: String): CommandAccess = {
    CommandAccess.newBuilder.addCommands(command).setAccess(CommandAccess.AccessMode.BLOCKED).build
  }

  // we defer the execution to the java list implmentation because thats what the proto addAllX methods use
  def allowAccessForCommandNames(commands: List[String]): CommandAccess = allowAccessForCommandNames(commands: java.util.List[String])
  def allowAccessForCommandNames(commands: java.util.List[String]): CommandAccess = {
    CommandAccess.newBuilder.addAllCommands(commands).setAccess(CommandAccess.AccessMode.ALLOWED).build
  }

  // we defer to scala implementation to collect names b/c there is no good way to handle java lists in scala
  def allowAccessForCommands(commands: List[Command]): CommandAccess = {
    CommandAccess.newBuilder.addAllCommands(commands.map { _.getName }).setAccess(CommandAccess.AccessMode.ALLOWED).build
  }
  def allowAccessForCommands(commands: java.util.List[Command]): CommandAccess = allowAccessForCommands(commands.toList)

  def blockAccessForCommandNames(commands: List[String]): CommandAccess = blockAccessForCommandNames(commands: java.util.List[String])
  def blockAccessForCommandNames(commands: java.util.List[String]): CommandAccess = {
    CommandAccess.newBuilder.addAllCommands(commands).setAccess(CommandAccess.AccessMode.BLOCKED).build
  }

  def blockAccessForCommands(commands: List[Command]): CommandAccess = {
    CommandAccess.newBuilder.addAllCommands(commands.map { _.getName }).setAccess(CommandAccess.AccessMode.BLOCKED).build
  }
  def blockAccessForCommands(commands: java.util.List[Command]): CommandAccess = blockAccessForCommands(commands.toList)

  def getAll() = CommandAccess.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build
  def getByCommand(command: Command) = CommandAccess.newBuilder.addCommands(command.getName).build
  def getByCommands(commands: java.util.List[Command]) = CommandAccess.newBuilder.addAllCommands(commands.map { _.getName }).build
  def getByCommandName(command: String) = CommandAccess.newBuilder.addCommands(command).build
  def getByCommandNames(commands: java.util.List[String]) = CommandAccess.newBuilder.addAllCommands(commands).build

  def getForUid(uid: ReefUUID) = CommandAccess.newBuilder.setUuid(uid).build

  def getForUser(user: String) = CommandAccess.newBuilder.setUser(user).build

  def deleteByUid(uid: ReefUUID) = CommandAccess.newBuilder.setUuid(uid).build
  def delete(cmd: CommandAccess) = CommandAccess.newBuilder.setUuid(cmd.getUuid).build
}