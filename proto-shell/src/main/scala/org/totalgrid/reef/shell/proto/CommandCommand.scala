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
import presentation.{ EntityView, CommandView }
import request.{ EntityRequest, CommandRequest }

import scala.collection.JavaConversions._

@Command(scope = "command", name = "list", description = "Lists commands")
class CommandListCommand extends ReefCommandSupport {

  def doCommand() = {
    EntityView.printList(EntityRequest.getAllOfType("Command", this))
  }
}

@Command(scope = "command", name = "issue", description = "Issues a command")
class CommandIssueCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Command uid/name", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = getUser match {
    case Some(user) => {
      CommandView.commandResponse(CommandRequest.issueForId(id, user, this))
    }
    case None => throw new Exception("Cannot issue command, user is not defined!")
  }
}

@Command(scope = "command", name = "status", description = "Views status of previously executed command.")
class CommandStatusCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Command Request id", required = true, multiValued = false)
  private var id: String = null

  def doCommand() = {
    CommandView.commandResponse(CommandRequest.statusOf(id, this))
  }
}

@Command(scope = "access", name = "access", description = "View command select/blocks.")
class AccessCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Command Access id", required = false, multiValued = false)
  private var id: String = null

  def doCommand() = {
    Option(id) match {
      case Some(uid) =>
        CommandView.accessInspect(CommandRequest.getAccessEntry(uid, this))
      case None =>
        CommandView.printAccessTable(CommandRequest.getAllAccessEntries(this))
    }
  }
}

@Command(scope = "access", name = "block", description = "Block specified commands.")
class AccessBlockCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Uid/nanme of commands to block.", required = false, multiValued = true)
  private var commands: java.util.List[String] = null

  def doCommand(): Unit = {
    if (getUser.isEmpty) throw new Exception("User not found")

    val cmdIds = Option(commands).map(_.toList) getOrElse Nil
    if (cmdIds.isEmpty) {
      println("Must specify at least one command.")
      return
    }

    CommandView.blockResponse(CommandRequest.blockCommands(cmdIds, getUser.get, this))
  }
}

@Command(scope = "access", name = "remove", description = "Remove blocks/selects.")
class AccessRemoveCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "id", description = "Block/select id.", required = false, multiValued = true)
  private var ids: java.util.List[String] = null

  def doCommand(): Unit = {
    if (getUser.isEmpty) throw new Exception("User not found")

    val accIds = Option(ids).map(_.toList) getOrElse Nil
    if (accIds.isEmpty) {
      println("Must specify at least one block/select.")
      return
    }

    CommandView.removeBlockResponse(CommandRequest.removeSelects(accIds, getUser.get, this))
  }
}

