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

import org.totalgrid.reef.util.LazyVar

import org.squeryl.annotations.Transient
import org.squeryl.PrimitiveTypeMode._
import java.util.UUID
import org.squeryl.{ Table, Query }
import org.squeryl.dsl.ast.LogicalBoolean

object Point {

  def findByName(name: String) = findByNames(name :: Nil)
  def findByNames(names: List[String]): Query[Point] = {
    ApplicationSchema.points.where(_.entityId in EntityQuery.findEntityIds(names, List("Point")))
  }
}

case class Point(
    _entityId: UUID,
    pointType: Int,
    unit: String,
    var abnormal: Boolean) extends EntityBasedModel(_entityId) with HasLogicalNodeAndEndpoint {

  /**
   * updated when the abnormal state is changed so we can "tunnel" this update through
   * to the service event.
   * The \@Transient attribute tells squeryl not to put this field in the database
   */
  @Transient
  var abnormalUpdated = false

  val triggers = LazyVar(ApplicationSchema.triggerSets.where(t => t.pointId === id).toList.map { p => p.point.value = this; p })

  val overrides = LazyVar(ApplicationSchema.overrides.where(t => t.pointId === id).toList.map { p => p.point.value = this; p })

  val calculations = LazyVar(ApplicationSchema.calculations.where(t => t.outputPointId === id).toList.map { p => p.outputPoint.value = this; p })
}

object Command {

  def findByNames(names: List[String]): Query[Command] = {
    ApplicationSchema.commands.where(_.entityId in EntityQuery.findEntityIds(names, List("Command")))
  }
  def findIdsByNames(names: List[String]): Query[Long] = {
    from(ApplicationSchema.commands)(c => where(c.entityId in EntityQuery.findEntityIds(names, List("Command"))) select (&(c.id)))
  }
}

case class Command(
    _entityId: UUID,
    val displayName: String,
    val commandType: Int,
    var lastSelectId: Option[Long],
    var triggerId: Option[Long]) extends EntityBasedModel(_entityId) with HasLogicalNodeAndEndpoint {

  def this() = this(new UUID(0, 0), "", -1, Some(0), Some(0))

  val currentActiveSelect = LazyVar(CommandLockModel.activeSelect(lastSelectId))

  val selectHistory = LazyVar(CommandLockModel.selectsForCommands(id :: Nil))

  val commandHistory = LazyVar(ApplicationSchema.userRequests.where(u => u.commandId === id).toList)
}

case class FrontEndPort(
    _entityId: UUID,
    val network: Option[String],
    val location: Option[String],
    val state: Int,
    var proto: Array[Byte]) extends EntityBasedModel(_entityId) {

  def this() = this(new UUID(0, 0), Some(""), Some(""), 0, Array.empty[Byte])

  val endpoints = LazyVar(ApplicationSchema.endpoints.where(ce => ce.frontEndPortId === Some(entityId)).toList)
}

case class ConfigFile(
    _entityId: UUID,
    val mimeType: String,
    var file: Array[Byte]) extends EntityBasedModel(_entityId) {

  val owners = LazyVar(EntityQuery.getParents(entity.value.id, "uses").toList)

  /// this flag allows us to tell if we have modified
  @Transient
  var changedOwners = false
}

case class CommunicationEndpoint(
    _entityId: UUID,
    val protocol: String,
    val autoAssigned: Boolean,
    var frontEndPortId: Option[UUID],
    val dataSource: Boolean) extends EntityBasedModel(_entityId) {

  def this() = this(new UUID(0, 0), "", true, Some(new UUID(0, 0)), false)
  def this(entityId: UUID, protocol: String, autoAssigned: Boolean, dataSource: Boolean) = this(entityId, protocol, autoAssigned, Some(new UUID(0, 0)), dataSource)

  val port = LazyVar(mayHaveOneByEntityUuid(ApplicationSchema.frontEndPorts, frontEndPortId))
  val frontEndAssignment = LazyVar(ApplicationSchema.frontEndAssignments.where(p => (p.endpointId === id) and (p.active === true)).single)
  val measProcAssignment = LazyVar(ApplicationSchema.measProcAssignments.where(p => p.endpointId === id).single)

  val configFilesIds = LazyVar(EntityQuery.getChildrenOfType(entity.value.id, "uses", "ConfigurationFile").map { _.id }.toList)

  def relationship = if (dataSource) "source" else "sink"

  val points = LazyVar(
    Entity.asType(ApplicationSchema.points, EntityQuery.getChildrenOfType(entity.value.id, relationship, "Point").toList, Some("Point")))
  val commands = LazyVar(
    Entity.asType(ApplicationSchema.commands, EntityQuery.getChildrenOfType(entity.value.id, relationship, "Command").toList, Some("Command")))
}

object CommunicationEndpoint {

  def preloadConfigFileIds(entries: List[CommunicationEndpoint]) {
    val configFileIds = PreloadQueries.childrenToEntityMap(entries.map { _.entityId }, "uses", "ConfigurationFile")
    entries.map { entry =>
      entry.configFilesIds.value = configFileIds.get(entry.entityId) match {
        case Some(files) => files.map { _.id }
        case None => Nil
      }
    }
  }

  def preloadPoints(entries: List[CommunicationEndpoint], vmap: (Query[UUID] => LogicalBoolean) => LogicalBoolean) {
    val endpointIdToPointMap = preloadEndpointChildObjects(entries, ApplicationSchema.points, vmap)
    entries.foreach { entry =>
      entry.points.value = endpointIdToPointMap.get(entry.id).getOrElse(Nil)
    }
  }
  def preloadCommands(entries: List[CommunicationEndpoint], vmap: (Query[UUID] => LogicalBoolean) => LogicalBoolean) {
    val endpointIdToCommandMap = preloadEndpointChildObjects(entries, ApplicationSchema.commands, vmap)
    entries.foreach { entry =>
      entry.commands.value = endpointIdToCommandMap.get(entry.id).getOrElse(Nil)
    }
  }

  private def preloadEndpointChildObjects[A <: EntityBasedModel](entries: List[CommunicationEndpoint], table: Table[A], vmap: (Query[UUID] => LogicalBoolean) => LogicalBoolean) = {
    from(ApplicationSchema.edges, ApplicationSchema.endpoints, ApplicationSchema.entities, table)((edge, endpoint, payloadEntity, payload) =>
      where(
        (endpoint.entityId in entries.map { _.entityId }) and
          (edge.parentId === endpoint.entityId) and
          (edge.childId === payloadEntity.id) and
          (((endpoint.dataSource === true) and (edge.relationship === "source")) or ((endpoint.dataSource === false) and (edge.relationship === "sink"))) and
          (payload.entityId === payloadEntity.id) and
          (vmap { payload.entityId in _ }))
        select (endpoint.id, payload, payloadEntity)).toList.groupBy(_._1).mapValues {
      _.map {
        case (id, payload, payloadEntity) =>
          payload.entity.value = payloadEntity
          payload
      }
    }
  }
}
