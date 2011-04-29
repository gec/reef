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
package org.totalgrid.reef.models

import org.totalgrid.reef.util.LazyVar
import org.totalgrid.reef.services.core.EQ

import org.squeryl.annotations.Transient
import org.squeryl.PrimitiveTypeMode._
import java.util.UUID
import org.squeryl.Query

class EntityBasedModel(val entityId: UUID) extends ModelWithId {

  val entity = LazyVar(hasOneByUuid(ApplicationSchema.entities, entityId))

  def entityName = entity.value.name

}

object Point {
  def newInstance(name: String, abnormal: Boolean, dataSource: Option[Entity]) = {
    val ent = EQ.findOrCreateEntity(name, "Point")
    val p = new Point(ent.id, false)
    dataSource.foreach(ln => { EQ.addEdge(ln, ent, "source"); p.logicalNode.value = Some(ln) })
    p.entity.value = ent
    p
  }

  def findByName(name: String) = findByNames(name :: Nil)
  def findByNames(names: List[String]): Query[Point] = {
    ApplicationSchema.points.where(_.entityId in EQ.findEntityIds(names, List("Point")))
  }
}

case class Point(
    _entityId: UUID,
    var abnormal: Boolean) extends EntityBasedModel(_entityId) {

  val logicalNode = LazyVar(mayHaveOne(EQ.getParentOfType(entityId, "source", "LogicalNode")))

  val sourceEdge = LazyVar(ApplicationSchema.edges.where(e => e.distance === 1 and e.childId === entityId and e.relationship === "source").headOption)

  /**
   * updated when the abnormal state is changed so we can "tunnel" this update through
   * to the service event.
   * The \@Transient attribute tells squeryl not to put this field in the database
   */
  @Transient
  var abnormalUpdated = false

  val endpoint = LazyVar(logicalNode.value.map(_.asType(ApplicationSchema.endpoints, "LogicalNode")))
}

object Command {
  def newInstance(name: String, displayName: String) = {
    val ent = EQ.findOrCreateEntity(name, "Command")
    val c = new Command(ent.id, displayName, false, None, None)
    c.entity.value = ent
    c
  }
  def newInstance(entity: Entity) = {
    val c = new Command(entity.id, entity.name, false, None, None)
    c.entity.value = entity
    c
  }

  def findByNames(names: List[String]): Query[Command] = {
    ApplicationSchema.commands.where(_.entityId in EQ.findEntityIds(names, List("Command")))
  }
  def findIdsByNames(names: List[String]): Query[Long] = {
    from(ApplicationSchema.commands)(c => where(c.entityId in EQ.findEntityIds(names, List("Command"))) select (&(c.id)))
  }
}

case class Command(
    _entityId: UUID,
    val displayName: String,
    var connected: Boolean,
    var lastSelectId: Option[Long],
    var triggerId: Option[Long]) extends EntityBasedModel(_entityId) {

  def this() = this(new UUID(0, 0), "", false, Some(0), Some(0))

  val logicalNode = LazyVar(mayHaveOne(EQ.getParentOfType(entityId, "source", "LogicalNode")))

  val sourceEdge = LazyVar(ApplicationSchema.edges.where(e => e.distance === 1 and e.childId === entityId and e.relationship === "source").headOption)

  val endpoint = LazyVar(logicalNode.value.map(_.asType(ApplicationSchema.endpoints, "LogicalNode")))

}

case class FrontEndPort(
    val name: String,
    val network: Option[String],
    val location: Option[String],
    val state: Int,
    var proto: Array[Byte]) extends ModelWithId {

  def this() = this("", Some(""), Some(""), org.totalgrid.reef.proto.FEP.CommChannel.State.UNKNOWN.getNumber, Array.empty[Byte])
}

case class ConfigFile(
    val entityId: UUID,
    val mimeType: String,
    var file: Array[Byte]) extends ModelWithId {

  val entity = LazyVar(hasOneByUuid(ApplicationSchema.entities, entityId))
  val owners = LazyVar(EQ.getParents(entity.value.id, "uses").toList)

  /// this flag allows us to tell if we have modified
  @Transient
  var changedOwners = false
}

case class CommunicationEndpoint(
    val entityId: UUID,
    val protocol: String,
    var frontEndPortId: Option[Long]) extends ModelWithId {

  def this() = this(new UUID(0, 0), "", Some(0))
  def this(entityId: UUID, protocol: String) = this(entityId, protocol, Some(0))

  val port = LazyVar(mayHaveOne(ApplicationSchema.frontEndPorts, frontEndPortId))
  val frontEndAssignment = LazyVar(ApplicationSchema.frontEndAssignments.where(p => p.endpointId === id).single)
  val measProcAssignment = LazyVar(ApplicationSchema.measProcAssignments.where(p => p.endpointId === id).single)

  val configFiles = LazyVar(Entity.asType(ApplicationSchema.configFiles, EQ.getChildrenOfType(entity.value.id, "uses", "ConfigurationFile").toList, Some("ConfigurationFile")))

  val entity = LazyVar(hasOneByUuid(ApplicationSchema.entities, entityId))
  val name = LazyVar(entity.value.name)

  val points = LazyVar(Entity.asType(ApplicationSchema.points, EQ.getChildrenOfType(entity.value.id, "source", "Point").toList, Some("Point")))
  val commands = LazyVar(Entity.asType(ApplicationSchema.commands, EQ.getChildrenOfType(entity.value.id, "source", "Command").toList, Some("Command")))
}
