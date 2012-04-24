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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto }
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import java.util.UUID
import org.totalgrid.reef.models.{ EntityQuery, ApplicationSchema, Entity }

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.squeryl.Query
import org.totalgrid.reef.authz.VisibilityMap
import org.totalgrid.reef.services.framework.{ RequestContext, UniqueAndSearchQueryable }

trait EntitySearches extends UniqueAndSearchQueryable[EntityProto, Entity] {
  val table = ApplicationSchema.entities
  override def uniqueQuery(context: RequestContext, proto: EntityProto, sql: Entity) = {
    List(
      proto.uuid.value.asParam(sql.id === UUID.fromString(_)).unique,
      proto.name.asParam(sql.name === _),
      EntityQuery.noneIfEmpty(proto.types).asParam(sql.id in EntityQuery.entityIdsFromTypes(_)))
  }

  override def searchQuery(context: RequestContext, proto: EntityProto, sql: Entity) = Nil

  private def resourceId = Descriptors.entity.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: Entity) = {
    import org.squeryl.PrimitiveTypeMode._
    sql.id in entitySelector
  }

  override def selector(map: VisibilityMap, sql: Entity) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }
}
object EntitySearches extends EntitySearches

case class EntitySearch(uuid: Option[String], name: Option[String], types: Option[List[String]]) {
  def map[B](f: EntitySearch => B): Option[B] = {
    if (uuid.isDefined || name.isDefined || (types.isDefined && !types.get.isEmpty)) Some(f(this))
    else None
  }
}

trait EntityPartsSearches extends UniqueAndSearchQueryable[EntitySearch, Entity] {
  val table = ApplicationSchema.entities
  override def uniqueQuery(context: RequestContext, proto: EntitySearch, sql: Entity) = {
    List(
      proto.uuid.asParam(sql.id === UUID.fromString(_)).unique,
      proto.name.asParam(sql.name === _),
      EntityQuery.noneIfEmpty(proto.types).asParam(sql.id in EntityQuery.entityIdsFromTypes(_)))
  }

  override def searchQuery(context: RequestContext, proto: EntitySearch, sql: Entity) = Nil

  private def resourceId = Descriptors.entity.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: Entity) = {
    import org.squeryl.PrimitiveTypeMode._
    sql.id in entitySelector
  }

  override def selector(map: VisibilityMap, sql: Entity) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }
}
object EntityPartsSearches extends EntityPartsSearches