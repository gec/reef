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

case class Point(
    val name: String,
    val entityId: Long,
    var abnormal: Boolean) extends ModelWithId {

  def this(n: String, entityId: Long) = this(n, entityId, false)

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))

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

case class Command(
    val name: String,
    val displayName: String,
    val entityId: Long,
    var connected: Boolean,
    var lastSelectId: Option[Long],
    var triggerId: Option[Long]) extends ModelWithId {

  def this() = this("", "", 0, false, Some(0), Some(0))
  def this(name: String, displayName: String, entityId: Long) = this(name, displayName, entityId, false, None, None)
  //def this(name: String, entityId: Long, connected: Boolean, lastSelectId: Option[Long], triggerId: Option[Long]) = this(name, name, entityId, false, None, None)
  def this(name: String, entityId: Long) = this(name, name, entityId, false, None, None)

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))

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
    val entityId: Long,
    val mimeType: String,
    var file: Array[Byte]) extends ModelWithId {

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))
  val owners = LazyVar(EQ.getParents(entity.value.id, "uses").toList)

  /// this flag allows us to tell if we have modified
  @Transient
  var changedOwners = false
}

case class CommunicationEndpoint(
    val entityId: Long,
    val protocol: String,
    var frontEndPortId: Option[Long]) extends ModelWithId {

  def this() = this(0, "", Some(0))
  def this(entityId: Long, protocol: String) = this(entityId, protocol, Some(0))

  val port = LazyVar(mayHaveOne(ApplicationSchema.frontEndPorts, frontEndPortId))
  val frontEndAssignment = LazyVar(ApplicationSchema.frontEndAssignments.where(p => p.endpointId === id).single)
  val measProcAssignment = LazyVar(ApplicationSchema.measProcAssignments.where(p => p.endpointId === id).single)

  val configFiles = LazyVar(Entity.asType(ApplicationSchema.configFiles, EQ.getChildrenOfType(entity.value.id, "uses", "ConfigurationFile").toList, Some("ConfigurationFile")))

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))
  val name = LazyVar(entity.value.name)

  val points = LazyVar(Entity.asType(ApplicationSchema.points, EQ.getChildrenOfType(entity.value.id, "source", "Point").toList, Some("Point")))
  val commands = LazyVar(Entity.asType(ApplicationSchema.commands, EQ.getChildrenOfType(entity.value.id, "source", "Command").toList, Some("Command")))
}
