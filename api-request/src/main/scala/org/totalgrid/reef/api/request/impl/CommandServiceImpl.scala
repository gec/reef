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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandAccess, UserCommandRequest }
import org.totalgrid.reef.proto.Model.Command
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.request.builders.{ CommandRequestBuilders, UserCommandRequestBuilders, CommandAccessRequestBuilders }
import org.totalgrid.reef.api.request.{ CommandService }

trait CommandServiceImpl extends ReefServiceBaseClass with CommandService {

  override def createCommandExecutionLock(id: Command): CommandAccess = createCommandExecutionLock(id :: Nil)

  override def createCommandExecutionLock(id: Command, expirationTimeMilli: Long): CommandAccess = createCommandExecutionLock(id :: Nil, expirationTimeMilli)

  override def createCommandExecutionLock(ids: java.util.List[Command]): CommandAccess = {
    ops("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids)).await().expectOne
    }
  }

  override def createCommandExecutionLock(ids: java.util.List[Command], expirationTimeMilli: Long): CommandAccess = {
    ops("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids, Option(expirationTimeMilli))).await().expectOne
    }
  }

  override def deleteCommandLock(uid: String): CommandAccess = ops("Couldn't delete command lock with uid: " + uid) {
    _.delete(CommandAccessRequestBuilders.getForUid(uid)).await().expectOne
  }

  override def deleteCommandLock(ca: CommandAccess): CommandAccess = ops("Couldn't delete command lock: " + ca) {
    _.delete(CommandAccessRequestBuilders.getForUid(ca.getUid)).await().expectOne

  }

  override def clearCommandLocks(): java.util.List[CommandAccess] = ops("Couldn't delete all command locks in system.") {
    _.delete(CommandAccessRequestBuilders.getAll).await().expectMany
  }

  override def executeCommandAsControl(id: Command): CommandStatus = ops("Couldn't execute control: " + id) {
    _.put(UserCommandRequestBuilders.executeControl(id)).await().expectOne.getStatus
  }

  override def executeCommandAsSetpoint(id: Command, value: Double): CommandStatus = {
    ops("Couldn't execute setpoint: " + id + " with double value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).await().expectOne.getStatus
    }
  }

  override def executeCommandAsSetpoint(id: Command, value: Int): CommandStatus = {
    ops("Couldn't execute setpoint: " + id + " with integer value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).await().expectOne.getStatus
    }
  }

  override def createCommandDenialLock(ids: java.util.List[Command]): CommandAccess = {
    ops("Couldn't create denial lock on ids: " + ids) {
      _.put(CommandAccessRequestBuilders.blockAccessForCommands(ids)).await().expectOne
    }
  }

  override def getCommandLocks(): java.util.List[CommandAccess] = ops("Couldn't get all command locks in system") {
    _.get(CommandAccessRequestBuilders.getAll).await().expectMany
  }

  override def getCommandLock(uid: String) = ops("Couldn't get command lock with uid: " + uid) {
    _.get(CommandAccessRequestBuilders.getForUid(uid)).await().expectOne
  }

  override def getCommandLockOnCommand(id: Command): CommandAccess = {
    ops("couldn't find command lock for command: " + id) {
      // TODO: better error message for getCommandLockOnCommand
      _.get(CommandAccessRequestBuilders.getByCommand(id)).await().expectOneOrNone match {
        case Some(x) => x
        case None => null // TODO - Java API, so returning null probably okay, but may want to evaulate throwing
      }
    }
  }

  override def getCommandLocksOnCommands(ids: java.util.List[Command]): java.util.List[CommandAccess] = {
    ops("Couldn't get command locks for: " + ids) {
      _.get(CommandAccessRequestBuilders.getByCommands(ids)).await().expectMany
    }
  }

  override def getCommandHistory(): java.util.List[UserCommandRequest] = ops("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForUid("*")).await().expectMany
  }

  override def getCommands(): java.util.List[Command] = ops("Couldn't get all commands") {
    _.get(CommandRequestBuilders.getAll).await().expectMany
  }

  override def getCommandByName(name: String) = ops("Couldn't get command with name: " + name) {
    _.get(CommandRequestBuilders.getByEntityName(name)).await().expectOne
  }
}