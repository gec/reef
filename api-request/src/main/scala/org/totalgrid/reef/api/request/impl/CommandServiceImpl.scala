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

  def createCommandExecutionLock(id: Command): CommandAccess = createCommandExecutionLock(id :: Nil)

  def createCommandExecutionLock(ids: java.util.List[Command]): CommandAccess = ops {
    _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids)).await().expectOne
  }

  def deleteCommandLock(uid: String): CommandAccess = ops {
    _.delete(CommandAccessRequestBuilders.getForUid(uid)).await().expectOne
  }

  def deleteCommandLock(ca: CommandAccess): CommandAccess = ops {
    _.delete(CommandAccessRequestBuilders.getForUid(ca.getUid)).await().expectOne
  }

  def clearCommandLocks(): java.util.List[CommandAccess] = ops {
    _.delete(CommandAccessRequestBuilders.getAll).await().expectMany()
  }

  def executeCommandAsControl(id: Command): CommandStatus = ops {
    _.put(UserCommandRequestBuilders.executeControl(id)).await().expectOne.getStatus
  }

  def executeCommandAsSetpoint(id: Command, value: Double): CommandStatus = ops {
    _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).await().expectOne.getStatus
  }

  def executeCommandAsSetpoint(id: Command, value: Int): CommandStatus = ops {
    _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).await().expectOne.getStatus
  }

  def createCommandDenialLock(ids: java.util.List[Command]): CommandAccess = ops {
    _.put(CommandAccessRequestBuilders.blockAccessForCommands(ids)).await().expectOne
  }

  def getCommandLocks(): java.util.List[CommandAccess] = ops {
    _.get(CommandAccessRequestBuilders.getAll).await().expectMany()
  }

  def getCommandLock(uid: String) = ops {
    _.get(CommandAccessRequestBuilders.getForUid(uid)).await().expectOne
  }

  def getCommandLockOnCommand(id: Command): CommandAccess = ops {
    _.get(CommandAccessRequestBuilders.getByCommand(id)).await().expectOneOrNone match {
      case Some(x) => x
      case None => null // TODO - Java API, so returning null probably okay, but may want to evaulate throwing
    }
  }

  def getCommandLocksOnCommands(ids: java.util.List[Command]): java.util.List[CommandAccess] = ops {
    _.get(CommandAccessRequestBuilders.getByCommands(ids)).await().expectMany()
  }

  def getCommandHistory(): java.util.List[UserCommandRequest] = ops {
    _.get(UserCommandRequestBuilders.getForUid("*")).await().expectMany()
  }

  def getCommands(): java.util.List[Command] = ops {
    _.get(CommandRequestBuilders.getAll).await().expectMany()
  }

  def getCommandByName(name: String) = ops {
    _.get(CommandRequestBuilders.getByEntityName(name)).await().expectOne
  }
}