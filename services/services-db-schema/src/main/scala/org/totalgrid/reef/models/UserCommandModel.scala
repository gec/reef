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

import org.totalgrid.reef.client.service.proto.Commands.{ CommandLock => AccessProto }

import org.totalgrid.reef.util.LazyVar
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import java.util.UUID
import org.squeryl.dsl.ast.LogicalBoolean

// Related to UserCommandRequest proto
case class UserCommandModel(
    val commandId: Long,
    val lockId: Long,
    val corrolationId: String,
    var status: Int,
    val expireTime: Long,
    val commandProto: Array[Byte],
    val errorMessage: Option[String]) extends ModelWithId {

  def command = hasOne(ApplicationSchema.commands, commandId)

  val lock = LazyVar(hasOne(ApplicationSchema.commandAccess, lockId))
  val agent = LazyVar(lock.value.agent.value)
}

object CommandLockModel {
  private val blockInt = AccessProto.AccessMode.BLOCKED.getNumber

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
        where((acc.id === id) and
          (acc.deleted === false) and
          (acc.access === blockInt or acc.expireTime.isNull or acc.expireTime > System.currentTimeMillis))
          select (acc)).toList
      select match {
        case List(a) => Some(a)
        case Nil => None
        case _ => throw new Exception("More than 1 active select on command: " + select)
      }
    }
  }

  /**
   * in one db roundtrip go an load all of the visible commands
   */
  def preloadCommands(locks: List[CommandLockModel], vmap: (Query[UUID] => LogicalBoolean) => LogicalBoolean) {
    val accessIds = locks.map { _.id }
    val cmdsById = from(ApplicationSchema.commandAccess, ApplicationSchema.commandToBlocks, ApplicationSchema.commands)((acc, join, cmd) =>
      where((acc.id in accessIds) and (join.accessId === acc.id) and (join.commandId === cmd.id) and (vmap { cmd.entityId in _ }))
        select (acc.id, cmd)).toList.groupBy { _._1 }

    locks.foreach { acc =>
      acc.commands.value = cmdsById(acc.id).map { _._2 }
    }
  }
}

case class CommandLockModel(
    val access: Int,
    val expireTime: Option[Long],
    val agentId: Long,
    var deleted: Boolean) extends ModelWithId with HasRelatedAgent {
  def this() = this(0, Some(0), 0, false)

  val commands = LazyVar(from(ApplicationSchema.commandToBlocks, ApplicationSchema.commands)((join, cmd) =>
    where(join.accessId === id and join.commandId === cmd.id)
      select (cmd)).toList)

}

case class CommandBlockJoin(
  val commandId: Long,
  val accessId: Long) extends ModelWithId

