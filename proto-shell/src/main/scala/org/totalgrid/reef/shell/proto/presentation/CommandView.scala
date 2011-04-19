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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Model.Command
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandAccess, UserCommandRequest }

object CommandView {

  def commandList(list: List[Command]) = {
    val rows = list.map { cmd =>
      ("[" + cmd.getUuid.getUuid + "]") :: cmd.getName :: ("\"" + cmd.getDisplayName + "\"") :: Nil
    }
    Table.justifyColumns(rows).foreach { line => println(line mkString " ") }
  }

  //def selectResponse(resp: CommandAccess)
  def commandResponse(resp: UserCommandRequest) = {
    val rows = ("ID: " :: "[" + resp.getUuid.getUuid + "]" :: Nil) ::
      ("Command:" :: resp.getCommandRequest.getName :: Nil) ::
      ("User:" :: resp.getUser :: Nil) ::
      ("Status:" :: resp.getStatus.toString :: Nil) :: Nil

    Table.justifyColumns(rows).foreach(line => println(line.mkString(" ")))
  }
  def commandResponse(resp: CommandStatus) = {
    val rows = ("Status:" :: resp.toString :: Nil) :: Nil

    Table.justifyColumns(rows).foreach(line => println(line.mkString(" ")))
  }

  def removeBlockResponse(removed: List[CommandAccess]) = {
    val rows = removed.map(acc => "Removed:" :: "[" + acc.getUuid.getUuid + "]" :: Nil)
    Table.renderRows(rows, " ")
  }

  def blockResponse(acc: CommandAccess) = {
    println("Block successful.")
    accessInspect(acc)
  }

  def accessInspect(acc: CommandAccess) = {
    val commands = acc.getCommandsList.toList
    val first = commands.headOption.getOrElse("")
    val tail = commands.tail

    val rows: List[List[String]] = ("ID:" :: "[" + acc.getUuid.getUuid + "]" :: Nil) ::
      ("Mode:" :: acc.getAccess.toString :: Nil) ::
      ("User:" :: acc.getUser :: Nil) ::
      ("Expires:" :: timeString(acc) :: Nil) ::
      ("Commands:" :: first :: Nil) :: Nil

    val cmdRows = tail.map(cmd => ("" :: cmd.toString :: Nil))

    Table.renderRows(rows ::: cmdRows, " ")
  }

  def timeString(acc: CommandAccess) = new java.util.Date(acc.getExpireTime).toString

  def accessHeader = {
    "Id" :: "Mode" :: "User" :: "Commands" :: "Expire Time" :: Nil
  }

  def accessRow(acc: CommandAccess): List[String] = {
    val commands = commandsEllipsis(acc.getCommandsList.toList)
    val time = new java.util.Date(acc.getExpireTime).toString
    acc.getUuid.getUuid :: acc.getAccess.toString :: acc.getUser :: commands :: time :: Nil
  }

  def commandsEllipsis(names: List[String]) = {
    names.length match {
      case 0 => ""
      case 1 => names.head
      case _ => "(" + names.head + ", ...)"
    }
  }

  def printAccessTable(list: List[CommandAccess]) = {
    Table.printTable(accessHeader, list.map(accessRow(_)))
  }
}