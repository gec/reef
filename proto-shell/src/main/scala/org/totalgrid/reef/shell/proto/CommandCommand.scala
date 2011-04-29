/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Command, Argument }
import presentation.{ CommandView }

import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.ReefUUID

@Command(scope = "command", name = "list", description = "Lists commands")
class CommandListCommand extends ReefCommandSupport {

  def doCommand() = {
    CommandView.commandList(services.getCommands().toList)
  }
}

@Command(scope = "command", name = "issue", description = "Issues a command")
class CommandIssueCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Command Name", description = "Command name", required = true, multiValued = false)
  private var cmdName: String = null

  def doCommand() = {
    val cmd = services.getCommandByName(cmdName)
    val select = services.createCommandExecutionLock(cmd)
    val response = try {
      services.executeCommandAsControl(cmd)
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

  def doCommand() = {
    Option(id) match {
      case Some(uid) =>
        CommandView.accessInspect(services.getCommandLock(ReefUUID.newBuilder.setUuid(id).build))
      case None =>
        CommandView.printAccessTable(services.getCommandLocks().toList)
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

    val access = services.deleteCommandLock(ReefUUID.newBuilder.setUuid(id).build)
    CommandView.removeBlockResponse(access :: Nil)
  }
}

