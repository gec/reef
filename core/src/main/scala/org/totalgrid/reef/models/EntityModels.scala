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

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.util.LazyVar
import java.util.UUID

class Entity(
    val name: String) extends ModelWithUUID {

  val types = LazyVar(from(ApplicationSchema.entityTypes)(t => where(id === t.entityId) select (&(t.entType))).toList)

  val attributes = LazyVar(from(ApplicationSchema.entityAttributes)(t => where(id === t.entityId) select (t)).toList)

  def asType[A <: { val entityId: UUID }](table: Table[A], ofType: String) = {
    if (types.value.find(_ == ofType).isEmpty) {
      throw new Exception("entity: " + id + " didnt have type: " + ofType + " but had: " + types)
    }

    val l = from(table)(t => where(t.entityId === id) select (t)).toList
    if (l.size == 0) throw new Exception("Missing id: " + id + " table: " + table)
    l.head
  }

}
object Entity {

  def asType[A <: { val entityId: UUID }](table: Table[A], entites: List[Entity], ofType: Option[String]) = {
    if (ofType.isDefined && !entites.forall(e => { e.types.value.find(_ == ofType.get).isDefined })) {
      throw new Exception("No all entities had type: " + ofType.get)
    }
    val ids = entites.map(_.id)
    if (ids.size > 0) from(table)(t => where(t.entityId in ids) select (t)).toList
    else Nil
  }
}

class EntityToTypeJoins(
  val entityId: UUID,
  val entType: String) {}

class EntityEdge(
    val parentId: UUID,
    val childId: UUID,
    val relationship: String,
    val distance: Int) extends ModelWithId {

  val parent = LazyVar(hasOneByUuid(ApplicationSchema.entities, parentId))
  val child = LazyVar(hasOneByUuid(ApplicationSchema.entities, childId))
}
class EntityDerivedEdge(
    val edgeId: Long,
    val parentEdgeId: Long) extends ModelWithId {

  val edge = LazyVar(hasOne(ApplicationSchema.edges, edgeId))
  val parent = LazyVar(hasOne(ApplicationSchema.edges, parentEdgeId))
}

class EntityAttribute(
    val entityId: UUID,
    val attrName: String,
    val stringVal: Option[String],
    val boolVal: Option[Boolean],
    val longVal: Option[Long],
    val doubleVal: Option[Double],
    val byteVal: Option[Array[Byte]]) extends ModelWithId {

  val entity = LazyVar(hasOneByUuid(ApplicationSchema.entities, entityId))

  def this() = this(new UUID(0, 0), "", Some(""), Some(true), Some(50L), Some(84.33), Some(Array.empty[Byte]))
}