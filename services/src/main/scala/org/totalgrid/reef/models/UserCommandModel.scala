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
package org.totalgrid.reef.models

import org.totalgrid.reef.proto.Commands.{ CommandLock => AccessProto }
import org.squeryl.PrimitiveTypeMode._

// Related to UserCommandRequest proto
case class UserCommandModel(
    val commandId: Long,
    val corrolationId: String,
    val agent: String,
    var status: Int,
    val expireTime: Long,
    val commandProto: Array[Byte]) extends ModelWithId {

  def command = hasOne(ApplicationSchema.commands, commandId)
}

object CommandLockModel {
  private val blockInt = AccessProto.AccessMode.BLOCKED.getNumber

  def activeSelectsForCommands(ids: List[Long]) = {
    val joinTable = ApplicationSchema.commandToBlocks
    val table = ApplicationSchema.commandAccess

    from(joinTable, table)((join, acc) =>
      where(join.commandId in ids and
        join.accessId === acc.id and (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
        select (acc)).distinct
  }
  def selectsForCommands(ids: List[Long]) = {
    val joinTable = ApplicationSchema.commandToBlocks
    val table = ApplicationSchema.commandAccess

    from(joinTable, table)((join, acc) =>
      where(join.commandId in ids and
        join.accessId === acc.id)
        select (acc))
  }

  def activeSelect(selectId: Option[Long]): Option[CommandLockModel] = {
    selectId.flatMap { id =>
      val select = from(ApplicationSchema.commandAccess)(acc =>
        where((acc.id === id) and (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
          select (acc)).toList
      select match {
        case List(a) => Some(a)
        case Nil => None
        case _ => throw new Exception("More than 1 active select on command: " + select)
      }
    }
  }
}

case class CommandLockModel(
    val access: Int,
    val expireTime: Option[Long],
    val agent: Option[String]) extends ModelWithId {
  def this() = this(0, Some(0), Some(""))

  def isActive = {
    access == AccessProto.AccessMode.BLOCKED.getNumber ||
      (expireTime match {
        case None => true
        case Some(time) => time > System.currentTimeMillis
      })
  }

  def commands: List[Command] = {
    from(ApplicationSchema.commandToBlocks, ApplicationSchema.commands)((join, cmd) =>
      where(join.accessId === id and join.commandId === cmd.id)
        select (cmd)).toList
  }
}

case class CommandBlockJoin(
  val commandId: Long,
  val accessId: Long) extends ModelWithId

