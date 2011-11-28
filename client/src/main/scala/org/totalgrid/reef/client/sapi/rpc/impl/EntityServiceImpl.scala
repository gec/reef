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
import org.totalgrid.reef.proto.Model.{ EntityAttributes, Entity, ReefUUID }
import org.totalgrid.reef.proto.Utils.Attribute
import org.totalgrid.reef.client.sapi.rpc.impl.builders.{ EntityAttributesBuilders, EntityRequestBuilders }
import org.totalgrid.reef.proto.OptionalProtos._

import net.agileautomata.executor4s.{ Result, Future }
import org.totalgrid.reef.client.sapi.rpc.EntityService
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.rpc.entities.EntityRelation

trait EntityServiceImpl extends HasAnnotatedOperations with EntityService {

  override def getAllEntities() = ops.operation("Couldn't get list of all entities") {
    _.get(EntityRequestBuilders.getAll).map(_.many)
  }

  override def getEntityByUid(uid: ReefUUID) = ops.operation("Couldn't get entity with uuid: " + uid.uuid) {
    _.get(EntityRequestBuilders.getByUid(uid)).map(_.one)
  }

  override def getEntityByName(name: String) = ops.operation("Couldn't get entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).map(_.one)
  }

  override def findEntityByName(name: String) = ops.operation("Couldn't find entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).map(_.oneOrNone)
  }

  override def getAllEntitiesWithType(typ: String) = {
    val request = if (typ == "*") EntityRequestBuilders.getAll else EntityRequestBuilders.getByType(typ)
    ops.operation("Couldn't get entities with type: " + typ)(_.get(request).map(_.many))
  }

  override def getAllEntitiesWithTypes(typ: List[String]) = {
    ops.operation("Couldn't get entities with types: " + typ) {
      _.get(EntityRequestBuilders.getByTypes(typ.toList)).map(_.many)
    }
  }

  override def getEntityRelatedChildrenOfType(parent: ReefUUID, relationship: String, typ: String) = {
    ops.operation("Couldn't get children of entity: " + parent.uuid + " relation: " + relationship + " type: " + typ) { session =>

      val future = session.get(EntityRequestBuilders.getRelatedChildrenOfTypeFromRootUid(parent, relationship, typ)).map(_.one)

      flatEntities(future)
    }
  }

  private def flatEntities(result: Future[Result[Entity]]): Future[Result[List[Entity]]] = {
    result.map(_.map(_.getRelationsList.flatMap(_.getEntitiesList).toList))
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String) = {
    ops.operation("Couldn't get immediate children of entity: " + parent.getValue + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootUid(parent, relationship)
      flatEntities(session.get(request).map(_.one))

    }
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String, constrainingTypes: List[String]) = {
    ops.operation("Couldn't get immediate children of entity: " + parent.getValue + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootUid(parent, relationship, constrainingTypes)
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

  def getEntityRelations(parent: ReefUUID, relations: List[EntityRelation]) = {
    ops.operation("Couldn't get tree for entity: " + parent + " relations: " + relations.mkString(", ")) { session =>
      val request = EntityRequestBuilders.getRelatedEntities(parent, relations)
      flatEntities(session.get(request).map(_.one))
    }
  }

  def getEntityRelationsFromTypeRoots(parentType: String, relations: List[EntityRelation]) = {
    ops.operation("Couldn't get tree from type roots: " + parentType + " relations: " + relations.mkString(", ")) { session =>
      val request = EntityRequestBuilders.getRelatedEntities(parentType, relations)
      session.get(request).map(_.many)
    }
  }

  override def getEntityTree(entityTree: Entity) = ops.operation("Couldn't get entity tree matching: " + entityTree) {
    _.get(entityTree).map(_.one)
  }

  override def getEntities(entityTree: Entity) = ops.operation("Couldn't get entity trees matching: " + entityTree) {
    _.get(entityTree).map(_.many)
  }

  override def getEntityAttributes(uid: ReefUUID) = ops.operation("Couldn't get attributes for entity: " + uid.uuid) {
    _.get(EntityAttributesBuilders.getForEntityUid(uid)).map(_.one)
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Boolean) = {
    addSingleAttribute(uid, EntityAttributesBuilders.boolAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Long) = {
    addSingleAttribute(uid, EntityAttributesBuilders.longAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Double) = {
    addSingleAttribute(uid, EntityAttributesBuilders.doubleAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: String) = {
    addSingleAttribute(uid, EntityAttributesBuilders.stringAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Array[Byte]) = {
    addSingleAttribute(uid, EntityAttributesBuilders.byteArrayAttribute(name, value))
  }

  override def removeEntityAttribute(uid: ReefUUID, attrName: String) = {
    ops.operation("Couldn't remove attribute for entity: " + uid + " attrName: " + attrName) { session =>
      val prev = getEntityAttributes(uid).await
      val set = prev.getAttributesList.toList.filterNot(_.getName == attrName)
      session.put(EntityAttributesBuilders.putAttributesToEntityUid(uid, set)).map(_.one)
    }
  }

  override def clearEntityAttributes(uid: ReefUUID) = {
    ops.operation("Couldn't clear all attributes for entity: " + uid.uuid) {
      _.delete(EntityAttributesBuilders.getForEntityUid(uid)).map(_.oneOrNone)
    }
  }

  protected def addSingleAttribute(uid: ReefUUID, attr: Attribute) = {
    ops.operation("Couldn't add attribute for entity: " + uid.uuid + " attr: " + attr) { session =>
      // TODO: fix getEntityAttributes futureness
      val prev = getEntityAttributes(uid).await
      val prevSet = prev.getAttributesList.toList.filterNot(_.getName == attr.getName)

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uid))
      prevSet.foreach(req.addAttributes)
      req.addAttributes(attr)

      session.put(req.build).map(_.one)
    }
  }
}

