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

import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto, EntityEdge => EntityEdgeProto }

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import org.totalgrid.reef.client.service.proto.OptionalProtos._

import UUIDConversions._
import java.util.UUID
import com.typesafe.scalalogging.slf4j.Logging

object EntityQuery extends Logging {

  import ApplicationSchema._

  def minimalEntityToProto(entry: Entity): EntityProto.Builder = {
    EntityProto.newBuilder.setUuid(makeUuid(entry)).setName(entry.name)
  }

  def entityToProto(entry: Entity): EntityProto.Builder = {
    val b = minimalEntityToProto(entry)
    entry.types.value.sorted.foreach(t => b.addTypes(t))
    b
  }

  // All entities as list, no relationships
  def allQuery: List[Entity] = {
    entities.where(t => true === true).toList
  }

  def entityIdsFromTypes(types: List[String]) = {
    from(entityTypes)(typ =>
      where(typ.entType in types)
        select (typ.entityId))
  }

  def noneIfEmpty(listO: Option[List[String]]) = {
    if (listO.isEmpty || listO.get.size == 0) None else listO
  }

  def returnSingleOption[A](o: List[A], what: String): Option[A] = {
    if (o.size > 1) throw new Exception(what + " does not exist")
    if (o.size == 1) Some(o.head) else None
  }

  def findEntity(proto: EntityProto): Option[Entity] = {
    if (proto.hasUuid) {
      returnSingleOption(entities.where(t => t.id === UUID.fromString(proto.getUuid.getValue)).toList, "Entity")
    } else if (proto.hasName) {
      returnSingleOption(entities.where(t => t.name === proto.getName).toList, "Entity")
    } else {
      throw new Exception("Not valid query")
    }
  }

  def getChildren(rootId: UUID, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId)))
        select (ent))
  }

  def getParents(rootId: UUID, relation: String) = {
    from(entities)(ent =>
      where(ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId)))
        select (ent))
  }

  def getParentsWithDistance(rootId: UUID, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.parentId and edge.childId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  def getChildrenWithDistance(rootId: UUID, relation: String) = {
    from(entities, edges)((ent, edge) =>
      where(ent.id === edge.childId and edge.parentId === rootId and edge.relationship === relation)
        select (ent, edge.distance))
  }

  // Helper for
  def getChildrenOfType(rootId: UUID, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def getChildrenIdsOfType(rootId: UUID, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.parentId === rootId) and (edge.relationship === relation))
            select (edge.childId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent.id))
  }

  def getParentOfType(rootId: UUID, relation: String, entType: String) = {
    from(entities)(ent =>
      where((ent.id in
        from(edges)(edge =>
          where((edge.childId === rootId) and (edge.relationship === relation))
            select (edge.parentId))) and (ent.id in entityIdsFromTypes(List(entType))))
        select (ent))
  }

  def findEdge(proto: EntityEdgeProto): Option[EntityEdge] = {
    proto.uuid.value.flatMap { v =>
      returnSingleOption(edges.where(t => t.id === v.toInt).toList, "Entity Edge")
    }
  }

  def findEntitiesByType(types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(typ.entityId === ent.id and (typ.entType in types))
        select (ent)).distinct
  }
  def findEntities(names: List[String], types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(ent.name in names and
        typ.entityId === ent.id and (typ.entType in types))
        select (ent)).distinct
  }
  def findEntitiesByUuids(uuids: List[UUID]) = {
    from(entities)(ent =>
      where(ent.id in uuids)
        select (ent)).distinct
  }

  def findEntityIds(names: List[String], types: List[String]) = {
    from(entities, entityTypes)((ent, typ) =>
      where(ent.name in names and
        typ.entityId === ent.id and (typ.entType in types))
        select (ent.id)).distinct
  }

  def entityIdsFromType(typ: String) = {
    from(entityTypes)(t =>
      where(t.entType === typ)
        select (t.entityId))
  }

  def findIdsOfChildren(rootNode: EntityProto, relation: String, childType: String): Query[UUID] = {
    if (rootNode.uuid.value == Some("*") || rootNode.name == Some("*")) {
      entityIdsFromType(childType)
    } else {
      findEntity(rootNode).map { rootEnt =>
        getChildrenIdsOfType(rootEnt.id, relation, childType)
      }.getOrElse(from(entities)(e => where(true === false) select (e.id)))
    }
  }

}

