/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }
import presentation.{ CommandView }

import scala.collection.JavaConversions._

import org.totalgrid.reef.util.Conversion
import org.totalgrid.reef.client.service.proto.Model.ReefID

@Command(scope = "command", name = "list", description = "Lists commands")
class CommandListCommand extends ReefCommandSupport {

  def doCommand() = {
    CommandView.commandList(services.getCommands().toList)
  }
}

@Command(scope = "command", name = "hist", description = "Shows recent commands executions")
class CommandHistoryCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Command Name", description = "Command name", required = false, multiValued = false)
  private var cmdName: String = null

  def doCommand() = {
    val history = Option(cmdName) match {
      case Some(name) => services.getCommandHistory(services.getCommandByName(name))
      case None => services.getCommandHistory()
    }
    CommandView.printHistoryTable(history.toList)
  }
}

@Command(scope = "command", name = "issue", description = "Issues a command")
class CommandIssueCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Command Name", description = "Command name", required = true, multiValued = false)
  private var cmdName: String = null

  @Argument(index = 1, description = "Setpoint value", required = false, multiValued = false)
  private var value: String = null

  def doCommand() = {
    val cmd = services.getCommandByName(cmdName)
    val select = services.createCommandExecutionLock(cmd)
    val response = try {
      Option(value) match {
        case Some(s) => Conversion.convertStringToType(s) match {
          case x: Int => services.executeCommandAsSetpoint(cmd, x)
          case x: Double => services.executeCommandAsSetpoint(cmd, x)
          case x: String => services.executeCommandAsSetpoint(cmd, x)
        }
        case None => services.executeCommandAsControl(cmd)
      }
    } finally {
      services.deleteCommandLock(select)
    }

    CommandView.commandResponse(response)
  }
}

@Command(scope = "access", name = "access", description = "View command select/blocks.")
class AccessCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Command Access id", required = false, multiValued = false)
  private var id: String = null

  @GogoOption(name = "-d", description = "Include inactive locks", required = false, multiValued = false)
  var includeInactive: Boolean = false

  def doCommand() = {
    Option(id) match {
      case Some(id) =>
        CommandView.accessInspect(services.getCommandLockById(ReefID.newBuilder.setValue(id).build))
      case None =>
        val locks = if (includeInactive) services.getCommandLocksIncludingDeleted() else services.getCommandLocks()
        CommandView.printAccessTable(locks.toList)
    }
  }
}

@Command(scope = "access", name = "block", description = "Block specified commands.")
class AccessBlockCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Names of commands to block.", required = true, multiValued = true)
  private var commandNames: java.util.List[String] = null

  def doCommand(): Unit = {

    val commands = commandNames.map { cmdName => services.getCommandByName(cmdName) }
    val block = services.createCommandDenialLock(commands)
    CommandView.blockResponse(block)
  }
}

@Command(scope = "access", name = "remove", description = "Remove block/select.")
class AccessRemoveCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Block/select id.", required = true, multiValued = false)
  private var id: String = null

  def doCommand(): Unit = {

    val access = services.deleteCommandLock(ReefID.newBuilder.setValue(id).build)
    CommandView.removeBlockResponse(access :: Nil)
  }
}

