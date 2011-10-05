package org.totalgrid.reef.sapi.request.impl

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
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandAccess, UserCommandRequest }
import org.totalgrid.reef.proto.Model.{ ReefUUID, Command }
import scala.collection.JavaConversions._
import org.totalgrid.reef.japi.request.builders.{ CommandRequestBuilders, UserCommandRequestBuilders, CommandAccessRequestBuilders }
import org.totalgrid.reef.sapi.request.CommandService

import org.totalgrid.reef.sapi.request.framework.ReefServiceBaseClass

trait CommandServiceImpl extends ReefServiceBaseClass with CommandService {

  override def createCommandExecutionLock(id: Command) = createCommandExecutionLock(id :: Nil)

  override def createCommandExecutionLock(id: Command, expirationTimeMilli: Long) = createCommandExecutionLock(id :: Nil, expirationTimeMilli)

  override def createCommandExecutionLock(ids: List[Command]) = {
    ops("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids)).map { _.expectOne }
    }
  }

  override def createCommandExecutionLock(ids: List[Command], expirationTimeMilli: Long) = {
    ops("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids, Option(expirationTimeMilli))).map { _.expectOne }
    }
  }

  override def deleteCommandLock(uid: String) = ops("Couldn't delete command lock with uid: " + uid) {
    _.delete(CommandAccessRequestBuilders.getForUid(uid)).map { _.expectOne }
  }

  override def deleteCommandLock(ca: CommandAccess) = ops("Couldn't delete command lock: " + ca) {
    _.delete(CommandAccessRequestBuilders.getForUid(ca.getUid)).map { _.expectOne }

  }

  override def clearCommandLocks() = ops("Couldn't delete all command locks in system.") {
    _.delete(CommandAccessRequestBuilders.getAll).map { _.expectMany() }
  }

  override def executeCommandAsControl(id: Command) = ops("Couldn't execute control: " + id) {
    _.put(UserCommandRequestBuilders.executeControl(id)).map { _.expectOne.getStatus }
  }

  override def executeCommandAsSetpoint(id: Command, value: Double) = {
    ops("Couldn't execute setpoint: " + id + " with double value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map { _.expectOne.getStatus }
    }
  }

  override def executeCommandAsSetpoint(id: Command, value: Int) = {
    ops("Couldn't execute setpoint: " + id + " with integer value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map { _.expectOne.getStatus }
    }
  }

  override def createCommandDenialLock(ids: List[Command]) = {
    ops("Couldn't create denial lock on ids: " + ids) {
      _.put(CommandAccessRequestBuilders.blockAccessForCommands(ids)).map { _.expectOne }
    }
  }

  override def getCommandLocks() = ops("Couldn't get all command locks in system") {
    _.get(CommandAccessRequestBuilders.getAll).map { _.expectMany() }
  }

  override def getCommandLock(uid: String) = ops("Couldn't get command lock with uid: " + uid) {
    _.get(CommandAccessRequestBuilders.getForUid(uid)).map { _.expectOne }
  }

  override def getCommandLockOnCommand(id: Command) = {
    ops("couldn't find command lock for command: " + id) {
      _.get(CommandAccessRequestBuilders.getByCommand(id)).map {
        _.expectOneOrNone match {
          case Some(x) => x
          case None => null // TODO: change name to findCommandLockOnCommand - reef-149
        }
      }
    }
  }

  override def getCommandLocksOnCommands(ids: List[Command]) = {
    ops("Couldn't get command locks for: " + ids) {
      _.get(CommandAccessRequestBuilders.getByCommands(ids)).map { _.expectMany() }
    }
  }

  override def getCommandHistory() = ops("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForUid("*")).map { _.expectMany() }
  }

  override def getCommandHistory(cmd: Command) = ops("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForName(cmd.getName)).map { _.expectMany() }
  }

  override def getCommands() = ops("Couldn't get all commands") {
    _.get(CommandRequestBuilders.getAll).map { _.expectMany() }
  }

  override def getCommandByName(name: String) = ops("Couldn't get command with name: " + name) {
    _.get(CommandRequestBuilders.getByEntityName(name)).map { _.expectOne }
  }

  override def getCommandsOwnedByEntity(parentUuid: ReefUUID) = {
    ops("Couldn't find commands owned by parent entity: " + parentUuid.getUuid) {
      _.get(CommandRequestBuilders.getOwnedByEntityWithUuid(parentUuid)).map { _.expectMany() }
    }
  }

  override def getCommandsBelongingToEndpoint(endpointUuid: ReefUUID) = {
    ops("Couldn't find commands sourced by endpoint: " + endpointUuid.getUuid) {
      _.get(CommandRequestBuilders.getSourcedByEndpoint(endpointUuid)).map { _.expectMany() }
    }
  }
}