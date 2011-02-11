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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.models.{ Command, ApplicationSchema, Entity }
import org.totalgrid.reef.proto.Model.{ Command => CommandProto, Entity => EntityProto }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.util.Optional._

import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.Descriptors

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{ Table, Query }
import org.totalgrid.reef.messaging.OptionalProtos._
import SquerylModel._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

class CommandService(protected val modelTrans: ServiceTransactable[CommandServiceModel])
    extends BasicProtoService[CommandProto, Command, CommandServiceModel] {

  override val descriptor = Descriptors.command
}

class CommandServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[CommandProto, CommandServiceModel](pub, classOf[CommandProto]) {

  def model = new CommandServiceModel(subHandler)
}

class CommandServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[CommandProto, Command]
    with EventedServiceModel[CommandProto, Command]
    with CommandServiceConversion {

  val table = ApplicationSchema.commands
  def getCommands(names: List[String]): Query[Command] = {
    table.where(_.name in names)
  }
  def createAndSetOwningNode(commands: List[String], dataSource: Entity): Unit = {
    if (commands.size == 0) return

    val allreadyExistingCommands = Entity.asType(ApplicationSchema.commands, EQ.findEntitiesByName(commands).toList, Some("Command"))
    val changeCommandOwner = allreadyExistingCommands.filter { c => c.sourceEdge.value.map(_.parentId != dataSource.id) getOrElse (true) }
    changeCommandOwner.foreach(p => {
      p.sourceEdge.value.foreach(EQ.deleteEdge(_))
      EQ.addEdge(dataSource, p.entity.value, "source")
      update(p, p)
    })

    val newCommands = commands.diff(allreadyExistingCommands.map(_.name).toList)
    newCommands.foreach(c => {
      val ent = EQ.findOrCreateEntity(c, "Command")
      EQ.addEdge(dataSource, ent, "source")
      // TODO: cleaner way of doing entity bound models
      val cmd = new Command(c, ent.id)
      cmd.entity.value = ent
      create(cmd)
    })
  }
}

trait CommandServiceConversion extends MessageModelConversion[CommandProto, Command] with UniqueAndSearchQueryable[CommandProto, Command] {

  def getRoutingKey(req: CommandProto) = ProtoRoutingKeys.generateRoutingKey {
    hasGet(req.hasUid, req.getUid) ::
      hasGet(req.hasName, req.getName) ::
      hasGet(req.hasEntity, req.getEntity.getUid) :: Nil
  }

  def uniqueQuery(proto: CommandProto, sql: Command) = {
    List(
      proto.entity.map(entity => sql.entityId in EntitySearches.searchQueryForId(entity, { _.id })))
  }

  def searchQuery(proto: CommandProto, sql: Command) = Nil

  def createModelEntry(proto: CommandProto): Command = {
    val ent = EQ.findOrCreateEntity(proto.getName, "Command")

    val cmd = new Command(proto.getName, ent.id)
    cmd.entity.value = ent
    cmd
  }

  def isModified(entry: Command, existing: Command) = {
    entry.lastSelectId != existing.lastSelectId
  }

  def convertToProto(sql: Command): CommandProto = {
    // TODO: fill out connected and selected parts of proto
    val b = CommandProto.newBuilder
    b.setName(sql.name).setUid(sql.name)
    //sql.entity.asOption.foreach(e => b.setEntity(EQ.entityToProto(e)))
    sql.entity.asOption match {
      case Some(e) => b.setEntity(EQ.entityToProto(e))
      case None => b.setEntity(EntityProto.newBuilder.setUid(sql.entityId.toString))
    }

    sql.logicalNode.asOption.foreach(_.foreach(ln => EQ.entityToProto(ln)))
    b.build
  }
}
