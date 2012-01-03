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

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.services.framework._
import java.util.UUID
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.{ EntityToTypeJoins, ApplicationSchema, Entity }
import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto }

object EntityService {
  def seed() {
    import org.squeryl.PrimitiveTypeMode._
    import org.totalgrid.reef.models.{ ApplicationSchema, EntityTypeMetaModel }

    inTransaction {
      if (ApplicationSchema.entityTypeMetaModel.Count.head == 0) {
        val metaModels = allKnownTypes.map { new EntityTypeMetaModel(_) }
        ApplicationSchema.entityTypeMetaModel.insert(metaModels)
      }
    }
  }

  val builtInTypes = List("Point", "Command", "Agent", "PermissionSet", "Application", "ConfigurationFile", "CommunicationEndpoint", "Channel")
  val wellKnownTypes = List("Site", "Region", "Equipment", "EquipmentGroup", "Root")

  val allKnownTypes = builtInTypes ::: wellKnownTypes
}

class EntityService(protected val model: EntityServiceModel)
    extends SyncModeledServiceBase[EntityProto, Entity, EntityServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.entity
}

class EntityServiceModel
    extends SquerylServiceModel[UUID, EntityProto, Entity]
    with EventedServiceModel[EntityProto, Entity] {

  val table = ApplicationSchema.entities

  def findOrCreate(context: RequestContext, name: String, entityTypes: List[String], uuid: Option[UUID]): Entity = {

    val current = EntityQuery.returnSingleOption(table.where(t => t.name === name).toList, "Entity")
    current match {
      case None => createEntity(context, name, entityTypes, uuid)
      case Some(existing) => updateEntity(context, name, entityTypes, existing)._1
    }
  }

  def findRecord(context: RequestContext, req: EntityProto): Option[Entity] = {
    EntityQuery.findEntity(req)
  }

  def findRecords(context: RequestContext, req: EntityProto): List[Entity] = {
    val results = if (req.hasUuid && req.getUuid.getValue == "*") {
      EntityQuery.allQuery
    } else {
      EntityQuery.protoTreeQuery(req).map { resultNode =>
        resultNode.ent.resultNode = Some(resultNode)
        resultNode.ent
      }
    }

    // TODO: Make limits non-superficial
    results.take(context.getHeaders.getResultLimit().getOrElse(100))
  }

  def createFromProto(context: RequestContext, req: EntityProto): Entity = {

    val types = req.getTypesList.toList
    val name = req.getName
    val uuid = req.uuid.map(v => UUID.fromString(v.getValue))

    createEntity(context, name, types, uuid)
  }

  private def createEntity(context: RequestContext, name: String, types: List[String], uuid: Option[UUID]): Entity = {
    import ApplicationSchema.{ entityTypes, entities }

    val entityModel = new Entity(name)
    uuid.foreach { id =>
      // if we are given a UUID it probably means we are importing uuids from another system.
      // we check that the uuids are unique not because we don't believe in uuids working in
      // theory, we just want to make sure to catch errors where the user code is giving us
      // the same uuid everytime
      // TODO: checks unnecessary?
      val existing = from(entities)(e => where(e.id === id) select (e)).toList
      if (!existing.isEmpty) throw new BadRequestException("UUID already in system with name: " + existing.head.name)
      entityModel.id = id
    }
    val ent = create(context, entityModel)
    EntityQuery.addEntityTypes(types.toList)
    types.foreach(t => entityTypes.insert(new EntityToTypeJoins(ent.id, t)))
    ent
  }

  override def updateFromProto(context: RequestContext, proto: EntityProto, existing: Entity): (Entity, Boolean) = {
    val types = proto.getTypesList.toList

    updateEntity(context, proto.getName, types, existing)
  }

  private def updateEntity(context: RequestContext, name: String, types: List[String], existing: Entity): (Entity, Boolean) = {
    if (name != existing.name) throw new BadRequestException("UUID already in system with name: " + existing.name)

    val additionalTypes = types.diff(existing.types.value)

    if (!additionalTypes.isEmpty) {
      val ent = EntityQuery.addTypesToEntity(existing, additionalTypes)
      (ent, true)
    } else {
      (existing, false)
    }
  }

  def sortResults(list: List[EntityProto]): List[EntityProto] = {
    list.sortWith { (a, b) => a.getName.toLowerCase.compareTo(b.getName.toLowerCase) < 0 }
  }

  def getRoutingKey(req: EntityProto): String = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.value :: req.name :: Nil
  }

  def convertToProto(entry: Entity): EntityProto = {
    entry.resultNode match {
      case None => EntityQuery.entityToProto(entry).build
      case Some(result) => result.toProto
    }
  }

  def isModified(entry: Entity, previous: Entity): Boolean = {
    entry.name.compareTo(previous.name) != 0 ||
      entry.types.value.diff(previous.types.value) == Nil
  }

  override protected def postDelete(context: RequestContext, previous: Entity) {
    EntityQuery.cleanupEntities(List(previous))
  }
}
