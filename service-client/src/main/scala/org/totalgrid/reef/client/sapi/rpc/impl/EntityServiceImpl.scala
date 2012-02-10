/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.rpc.impl

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.sapi.rpc.impl.builders.{ EntityAttributesBuilders, EntityRequestBuilders }
import org.totalgrid.reef.client.service.proto.OptionalProtos._

import net.agileautomata.executor4s.{ Result, Future }
import org.totalgrid.reef.client.sapi.rpc.EntityService
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.service.entity.EntityRelation
import org.totalgrid.reef.client.service.proto.Model.{ EntityAttribute, EntityAttributes, Entity, ReefUUID }

trait EntityServiceImpl extends HasAnnotatedOperations with EntityService {

  override def getEntities() = ops.operation("Couldn't get list of all entities") {
    _.get(EntityRequestBuilders.getAll).map(_.many)
  }

  override def getEntityByUuid(uuid: ReefUUID) = ops.operation("Couldn't get entity with uuid: " + uuid.getValue) {
    _.get(EntityRequestBuilders.getById(uuid)).map(_.one)
  }

  override def getEntityByName(name: String) = ops.operation("Couldn't get entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).map(_.one)
  }

  override def getEntitiesByUuids(uuids: List[ReefUUID]) = ops.operation("Couldn't get entities with uuids: " + uuids) { _ =>
    batchGets(uuids.map { EntityRequestBuilders.getById(_) })
  }

  override def getEntitiesByNames(names: List[String]) = ops.operation("Couldn't get entities with names: " + names) { _ =>
    batchGets(names.map { EntityRequestBuilders.getByName(_) })
  }

  override def findEntityByName(name: String) = ops.operation("Couldn't find entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).map(_.oneOrNone)
  }

  override def getEntitiesWithType(typ: String) = {
    val request = if (typ == "*") EntityRequestBuilders.getAll else EntityRequestBuilders.getByType(typ)
    ops.operation("Couldn't get entities with type: " + typ)(_.get(request).map(_.many))
  }

  override def getEntitiesWithTypes(typ: List[String]) = {
    ops.operation("Couldn't get entities with types: " + typ) {
      _.get(EntityRequestBuilders.getByTypes(typ.toList)).map(_.many)
    }
  }

  override def getEntityRelatedChildrenOfType(parent: ReefUUID, relationship: String, typ: String) = {
    ops.operation("Couldn't get children of entity: " + parent.getValue + " relation: " + relationship + " type: " + typ) { session =>

      val future = session.get(EntityRequestBuilders.getRelatedChildrenOfTypeFromRootId(parent, relationship, typ)).map(_.one)

      flatEntities(future)
    }
  }

  private def flatEntities(result: Future[Result[Entity]]): Future[Result[List[Entity]]] = {
    result.map(_.map(_.getRelationsList.flatMap(_.getEntitiesList).toList))
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String) = {
    ops.operation("Couldn't get immediate children of entity: " + parent.getValue + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootId(parent, relationship)
      flatEntities(session.get(request).map(_.one))

    }
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String, constrainingTypes: List[String]) = {
    ops.operation("Couldn't get immediate children of entity: " + parent.getValue + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootId(parent, relationship, constrainingTypes)
      flatEntities(session.get(request).map(_.one))
    }
  }

  override def getEntityChildren(parent: ReefUUID, relationship: String, depth: Int, constrainingTypes: List[String]) = {
    ops.operation("Couldn't get tree for entity: " + parent.getValue + " relation: " + relationship + " depth: " + depth + " types: " + constrainingTypes.toList) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parent, relationship, depth, constrainingTypes)
      session.get(request).map(_.one)
    }
  }

  override def getEntityChildren(parent: ReefUUID, relationship: String, depth: Int) = {
    ops.operation("Couldn't get tree for entity: " + parent.getValue + " relation: " + relationship + " depth: " + depth) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parent, relationship, depth, Nil)
      session.get(request).map(_.one)
    }
  }

  override def getEntityChildrenFromTypeRoots(parentType: String, relationship: String, depth: Int, constrainingTypes: List[String]) = {
    ops.operation("Couldn't get tree from type roots: " + parentType + " relation: " + relationship + " depth: " + depth + " types: " + constrainingTypes.toList) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parentType, relationship, depth, constrainingTypes)
      session.get(request).map(_.many)
    }
  }

  override def getEntityRelations(parent: ReefUUID, relations: List[EntityRelation]) = {
    ops.operation("Couldn't get tree for entity: " + parent + " relations: " + relations.mkString(", ")) { session =>
      val request = EntityRequestBuilders.getRelatedEntities(parent, relations)
      flatEntities(session.get(request).map(_.one))
    }
  }

  override def getEntityRelationsFromTypeRoots(parentType: String, relations: List[EntityRelation]) = {
    ops.operation("Couldn't get tree from type roots: " + parentType + " relations: " + relations.mkString(", ")) { session =>
      val request = EntityRequestBuilders.getRelatedEntities(parentType, relations)
      session.get(request).map(_.many)
    }
  }

  override def getEntityRelationsForParents(parents: List[ReefUUID], relations: List[EntityRelation]) = {
    ops.operation("Couldn't get tree for parents: " + parents.size + " relations: " + relations.mkString(", ")) { _ =>
      batchGets(parents.map { EntityRequestBuilders.getRelatedEntities(_, relations) })
    }
  }

  override def getEntityRelationsForParentsByName(parents: List[String], relations: List[EntityRelation]) = {
    ops.operation("Couldn't get relations for parentNames: " + parents.size + " relations: " + relations.mkString(", ")) { _ =>
      batchGets(parents.map { EntityRequestBuilders.getRelatedEntitiesByName(_, relations) })
    }
  }

  override def searchForEntityTree(entityTree: Entity) = ops.operation("Couldn't get entity tree matching: " + entityTree) {
    _.get(entityTree).map(_.one)
  }

  override def searchForEntities(entityTree: Entity) = ops.operation("Couldn't get entity trees matching: " + entityTree) {
    _.get(entityTree).map(_.many)
  }

  override def getEntityAttributes(id: ReefUUID) = ops.operation("Couldn't get attributes for entity: " + id.getValue) {
    _.get(EntityAttributesBuilders.getForEntityId(id)).map(_.one)
  }

  override def setEntityAttribute(id: ReefUUID, name: String, value: Boolean) = {
    addSingleAttribute(id, EntityAttributesBuilders.boolAttribute(name, value))
  }

  override def setEntityAttribute(id: ReefUUID, name: String, value: Long) = {
    addSingleAttribute(id, EntityAttributesBuilders.longAttribute(name, value))
  }

  override def setEntityAttribute(id: ReefUUID, name: String, value: Double) = {
    addSingleAttribute(id, EntityAttributesBuilders.doubleAttribute(name, value))
  }

  override def setEntityAttribute(id: ReefUUID, name: String, value: String) = {
    addSingleAttribute(id, EntityAttributesBuilders.stringAttribute(name, value))
  }

  override def setEntityAttribute(id: ReefUUID, name: String, value: Array[Byte]) = {
    addSingleAttribute(id, EntityAttributesBuilders.byteArrayAttribute(name, value))
  }

  override def removeEntityAttribute(id: ReefUUID, attrName: String) = {
    ops.operation("Couldn't remove attribute for entity: " + id + " attrName: " + attrName) { session =>

      val delReq = EntityAttribute.newBuilder
        .setEntity(Entity.newBuilder.setUuid(id))
        .setAttribute(Attribute.newBuilder.setName(attrName).setVtype(Attribute.Type.STRING).build)
        .build

      session.delete(delReq).map(_.one).await

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(id)).build
      session.get(req).map(_.one)
    }
  }

  override def clearEntityAttributes(id: ReefUUID) = {
    ops.operation("Couldn't clear all attributes for entity: " + id.getValue) { session =>

      val results = session.get(EntityAttributesBuilders.getForEntityId(id)).map(_.oneOrNone)

      results.await

      val delReq = EntityAttribute.newBuilder.setEntity(Entity.newBuilder.setUuid(id)).build
      session.delete(delReq).map(_.oneOrNone).await

      results
    }
  }

  protected def addSingleAttribute(id: ReefUUID, attr: Attribute) = {
    ops.operation("Couldn't add attribute for entity: " + id.getValue + " attr: " + attr) { session =>
      // TODO: fix getEntityAttributes futureness

      val prev = getEntityAttributes(id).await
      val prevSet = prev.getAttributesList.toList.filterNot(_.getName == attr.getName)

      val putReq = EntityAttribute.newBuilder
        .setEntity(Entity.newBuilder.setUuid(id))
        .setAttribute(attr)
        .build

      session.put(putReq).map(_.one).await

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(id)).build
      session.get(req).map(_.one)
    }
  }
}

