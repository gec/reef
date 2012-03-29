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
package org.totalgrid.reef.authz

import java.util.UUID
import org.totalgrid.reef.models.ApplicationSchema

import org.squeryl.PrimitiveTypeMode._

sealed trait ResourceSelector {
  def includes(uuids: List[UUID]): List[Option[Boolean]]

  def allow: Boolean

  def resourceDependent: Boolean
}

class WildcardMatcher extends ResourceSelector {

  def includes(uuids: List[UUID]) = if (uuids.isEmpty) List(Some(true)) else uuids.map { x => Some(true) }

  val allow = true
  val resourceDependent = false
  override def toString() = "*"
}

class EntityTypeIncludes(types: List[String]) extends ResourceSelector {
  val allow = true
  val resourceDependent = true

  def includes(uuids: List[UUID]): List[Option[Boolean]] = {
    val entitiesWithType = from(ApplicationSchema.entityTypes)(sql =>
      where((sql.entType in types) and (sql.entityId in uuids))
        select (sql.entityId)).toList

    uuids.map { x =>
      entitiesWithType.find(_ == x) match {
        case Some(ent) => Some(true)
        case None => None
      }
    }
  }
  override def toString() = "entity.types include " + types.mkString("(", ",", ")")
}

class EntityTypeDoesntInclude(types: List[String]) extends ResourceSelector {
  val allow = false
  val resourceDependent = true

  private val includeMatcher = new EntityTypeIncludes(types)

  def includes(uuids: List[UUID]) = includeMatcher.includes(uuids)

  override def toString() = "entity.types doesnt include " + types.mkString("(", ",", ")")
}

class EntityHasName(names: List[String]) extends ResourceSelector {
  val allow = true
  val resourceDependent = true

  def includes(uuids: List[UUID]) = {
    val entityWithName = from(ApplicationSchema.entities)(sql =>
      where((sql.name in names) and (sql.id in uuids))
        select (sql.id)).toList

    uuids.map { x =>
      entityWithName.find(_ == x) match {
        case Some(ent) => Some(true)
        case None => None
      }
    }
  }

  override def toString() = "entity.name is not " + names.mkString("(", ",", ")")
}

class EntityParentIncludes(parentNames: List[String]) extends ResourceSelector {
  val allow = true
  val resourceDependent = true

  def includes(uuids: List[UUID]): List[Option[Boolean]] = {
    val matching = from(ApplicationSchema.entities, ApplicationSchema.edges, ApplicationSchema.entities)((parent, edge, child) =>
      where(
        (parent.name in parentNames) and (child.id in uuids)
          and ((child.name in parentNames)
            or ((parent.id === edge.parentId)
              and (child.id === edge.childId)
              and (edge.relationship === "owns"))))
        select (child.id)).toList

    uuids.map { x =>
      matching.find(_ == x) match {
        case Some(ent) => Some(true)
        case None => None
      }
    }
  }
  override def toString() = "entity.parents include " + parentNames.mkString("(", ",", ")")
}

