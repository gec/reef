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
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.services.core.util.UUIDConversions._

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.models.Entity
import org.totalgrid.reef.services.framework._

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

class EntityService extends ServiceEntryPoint[EntityProto] with AuthorizesEverything {

  override val descriptor = Descriptors.entity

  override def putAsync(source: RequestContextSource, req: EntityProto)(callback: (Response[EntityProto]) => Unit) {
    callback(source.transaction { context =>
      if (!req.hasName || req.getTypesCount == 0) {
        throw new BadRequestException("Must include name and atleast one entity type to create entity.")
      }

      authorizeRead(context, req)
      val types = req.getTypesList.toList
      val name = req.getName
      val list = EntityQueryManager.nameTypeQuery(Some(name), None)

      var (status, ent) = list match {
        case List(ent, _) => throw new BadRequestException("more than one entity matched: " + name + " types:" + types)
        case List(ent) => (Status.NOT_MODIFIED, list.head)
        case Nil =>
          authorizeCreate(context, req)
          (Status.CREATED, EntityQueryManager.addEntity(name, types, req.uuid))
      }

      val additionalTypes = types.diff(ent.types.value)

      if (!additionalTypes.isEmpty) {
        authorizeUpdate(context, req)
        ent = EntityQueryManager.addTypesToEntity(ent, additionalTypes)
        if (status == Status.NOT_MODIFIED) status = Status.UPDATED
      }
      Response(status, EntityQueryManager.entityToProto(ent).build :: Nil)
    })
  }

  override def getAsync(source: RequestContextSource, req: EntityProto)(callback: (Response[EntityProto]) => Unit) {
    callback(source.transaction { context =>
      authorizeRead(context, req)
      val result = EntityQueryManager.fullQuery(req)
      if (result.size == 0) {
        EntityQueryManager.checkAllTypesInSystem(req)
      }
      Response(Status.OK, result)
    })
  }

  override def deleteAsync(source: RequestContextSource, req: EntityProto)(callback: (Response[EntityProto]) => Unit) {
    callback(source.transaction { context =>
      authorizeRead(context, req)
      authorizeDelete(context, req)
      val entities = EntityQueryManager.fullQueryAsModels(req);

      val (results, status) = entities match {
        case Nil => (req :: Nil, Status.NOT_MODIFIED)
        case l: List[Entity] =>
          EntityQueryManager.deleteEntities(entities)
          (entities.map { EntityQueryManager.entityToProto(_).build }, Status.DELETED)
      }
      Response(status, results)
    })
  }
}

import org.totalgrid.reef.client.service.proto.Model.{ EntityEdge => EntityEdgeProto }
import org.totalgrid.reef.models.{ EntityEdge }

class EntityEdgeService extends ServiceEntryPoint[EntityEdgeProto] with AuthorizesCreate with AuthorizesUpdate with AuthorizesDelete with AuthorizesRead {

  override val descriptor = Descriptors.entityEdge

  def convertToProto(entry: EntityEdge): EntityEdgeProto = {
    val b = EntityEdgeProto.newBuilder()
    import org.totalgrid.reef.services.framework.SquerylModel._
    b.setParent(EntityProto.newBuilder.setUuid(makeUuid(entry.parentId)))
    b.setChild(EntityProto.newBuilder.setUuid(makeUuid(entry.childId)))
    b.setRelationship(entry.relationship)
    b.build
  }

  override def putAsync(source: RequestContextSource, req: EntityEdgeProto)(callback: (Response[EntityEdgeProto]) => Unit) {
    callback(source.transaction { context =>
      authorizeRead(context, req)

      val parentEntity = EntityQueryManager.findEntity(req.getParent).getOrElse(throw new BadRequestException("cannot find parent: " + req.getParent))
      val childEntity = EntityQueryManager.findEntity(req.getChild).getOrElse(throw new BadRequestException("cannot find child: " + req.getChild))
      val existingEdge = EntityQueryManager.findEdge(parentEntity, childEntity, req.getRelationship)

      val (edge, status) = existingEdge match {
        case Some(edge) => (edge, Status.NOT_MODIFIED)
        case None =>
          authorizeCreate(context, req)
          (EntityQueryManager.addEdge(parentEntity, childEntity, req.getRelationship), Status.CREATED)
      }
      val proto = convertToProto(edge)
      Response(status, proto :: Nil)
    })
  }

  override def deleteAsync(source: RequestContextSource, req: EntityEdgeProto)(callback: (Response[EntityEdgeProto]) => Unit) {
    callback(source.transaction { context =>
      authorizeRead(context, req)
      authorizeDelete(context, req)
      val existingEdge: Option[EntityEdge] = EntityQueryManager.findEntity(req.getParent).flatMap { parent =>
        EntityQueryManager.findEntity(req.getChild).flatMap { child =>
          EntityQueryManager.findEdge(parent, child, req.getRelationship)
        }
      }

      val (proto, status) = existingEdge match {
        case Some(edge) =>
          EntityQueryManager.deleteEdge(edge)
          (convertToProto(edge), Status.DELETED)
        case None => (req, Status.NOT_MODIFIED)
      }
      Response(status, proto :: Nil)
    })
  }

  override def getAsync(source: RequestContextSource, req: EntityEdgeProto)(callback: (Response[EntityEdgeProto]) => Unit) {
    callback(source.transaction { context =>
      authorizeRead(context, req)
      // TODO: add edge searching
      import org.squeryl.PrimitiveTypeMode._
      val edges = EntityQueryManager.edges.where(t => true === true).toList
      Response(Status.OK, edges.map { convertToProto(_) })
    })
  }
}
