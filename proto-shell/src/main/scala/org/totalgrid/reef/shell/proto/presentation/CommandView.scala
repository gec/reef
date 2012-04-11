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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.client.service.proto.Model.Command
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.util.Table
import org.totalgrid.reef.client.service.proto.Commands.{ CommandResult, CommandStatus, CommandLock, UserCommandRequest }

object CommandView {

  def commandList(cmds: List[Command]) = {
    Table.printTable(commandHeader, cmds.map(commandRow(_)))
  }

  def commandHeader = {
    "Name" :: "DisplayName" :: "Type" :: "Endpoint" :: Nil
  }

  def commandRow(a: Command) = {
    a.getName ::
      a.getDisplayName ::
      a.getType.toString ::
      a.endpoint.name.getOrElse("") ::
      Nil
  }

  //def selectResponse(resp: CommandLock)
  def commandResponse(resp: UserCommandRequest) = {
    val rows = ("ID: " :: "[" + resp.getId + "]" :: Nil) ::
      ("Command:" :: resp.commandRequest.command.name.getOrElse("unknown") :: Nil) ::
      ("User:" :: resp.getUser :: Nil) ::
      ("Status:" :: resp.getStatus.toString :: Nil) ::
      ("Message:" :: resp.getErrorMessage :: Nil) :: Nil

    Table.renderRows(rows, " ")
  }
  def commandResponse(resp: CommandResult) = {

    val msgEntries = resp.errorMessage.map { msg: String => List(" ErrorMessage: ", msg) }.getOrElse(List.empty[String])

    val rows = ("Status:" :: resp.getStatus.toString :: msgEntries) :: Nil

    Table.renderRows(rows, " ")
  }

  def removeBlockResponse(removed: List[CommandLock]) = {
    val rows = removed.map(acc => "Removed:" :: "[" + acc.getId + "]" :: Nil)
    Table.renderRows(rows, " ")
  }

  def blockResponse(acc: CommandLock) = {
    println("Block successful.")
    accessInspect(acc)
  }

  def accessInspect(acc: CommandLock) = {
    val commands = acc.getCommandsList.toList
    val first = commands.headOption.map { _.name }.flatten.toString
    val tail = commands.tail

    val rows: List[List[String]] = ("ID:" :: acc.getId.getValue :: Nil) ::
      ("Mode:" :: acc.getAccess.toString :: Nil) ::
      ("User:" :: acc.getUser :: Nil) ::
      ("Expires:" :: timeString(acc) :: Nil) ::
      ("Deleted:" :: acc.getDeleted.toString :: Nil) :: Nil
    ("Commands:" :: first :: Nil) :: Nil

    val cmdRows = tail.map(cmd => ("" :: cmd.name.toString :: Nil))

    Table.renderRows(rows ::: cmdRows, " ")
  }

  def timeString(acc: CommandLock) = new java.util.Date(acc.getExpireTime).toString

  def accessHeader = {
    "Id" :: "Mode" :: "User" :: "Commands" :: "Expire Time" :: "Deleted?" :: Nil
  }

  def accessRow(acc: CommandLock): List[String] = {
    val commands = commandsEllipsis(acc.getCommandsList.toList)
    val time = new java.util.Date(acc.getExpireTime).toString
    acc.getId.getValue :: acc.getAccess.toString :: acc.getUser :: commands :: time :: acc.getDeleted.toString :: Nil
  }

  def commandsEllipsis(names: List[Command]) = {
    names.length match {
      case 0 => ""
      case 1 => names.head.name.getOrElse("unknown")
      case _ => "(" + names.head.name.getOrElse("unknown") + ", ...)"
    }
  }

  def printAccessTable(list: List[CommandLock]) = {
    Table.printTable(accessHeader, list.map(accessRow(_)))
  }

  def printHistoryTable(history: List[UserCommandRequest]) = {
    Table.printTable(historyHeader, history.map(historyRow(_)))
  }

  def historyHeader = {
    "Id" :: "Command" :: "Status" :: "Message" :: "User" :: "Type" :: "Value" :: Nil
  }

  private def commandValueString(cr: OptCommandsCommandRequest) = {
    cr.intVal.map { _.toString }.orElse(cr.doubleVal.map { _.toString }).orElse(cr.stringVal).getOrElse("")
  }

  def historyRow(a: UserCommandRequest) = {
    a.id.value.getOrElse("unknown") ::
      a.commandRequest.command.name.getOrElse("unknown") ::
      a.status.map { _.toString }.getOrElse("unknown") ::
      a.errorMessage.getOrElse("") ::
      a.user.getOrElse("unknown") ::
      a.commandRequest._type.map { _.toString }.getOrElse("unknown") ::
      commandValueString(a.commandRequest) ::
      Nil
  }
}