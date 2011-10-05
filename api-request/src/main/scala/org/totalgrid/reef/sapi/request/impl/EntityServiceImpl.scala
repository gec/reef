package org.totalgrid.reef.sapi.request.impl

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
import org.totalgrid.reef.sapi.request.EntityService
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ EntityAttributes, Entity, ReefUUID }
import org.totalgrid.reef.proto.Utils.Attribute
import org.totalgrid.reef.japi.request.builders.{ EntityAttributesBuilders, EntityRequestBuilders }
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.sapi.request.framework.ReefServiceBaseClass

trait EntityServiceImpl extends ReefServiceBaseClass with EntityService {

  override def getAllEntities() = ops("Couldn't get list of all entities") {
    _.get(EntityRequestBuilders.getAll).map { _.expectMany() }
  }

  override def getEntityByUid(uid: ReefUUID) = ops("Couldn't get entity with uuid: " + uid.uuid) {
    _.get(EntityRequestBuilders.getByUid(uid)).map { _.expectOne }
  }

  override def getEntityByName(name: String) = ops("Couldn't get entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).map { _.expectOne }
  }

  override def getAllEntitiesWithType(typ: String) = {
    val request = if (typ == "*") EntityRequestBuilders.getAll else EntityRequestBuilders.getByType(typ)
    ops("Couldn't get entities with type: " + typ) { _.get(request).map { _.expectMany() } }
  }

  override def getAllEntitiesWithTypes(typ: List[String]) = {
    ops("Couldn't get entities with types: " + typ) {
      _.get(EntityRequestBuilders.getByTypes(typ.toList)).map { _.expectMany() }
    }
  }

  override def getEntityRelatedChildrenOfType(parent: ReefUUID, relationship: String, typ: String) = {
    ops("Couldn't get children of entity: " + parent.uuid + " relation: " + relationship + " type: " + typ) { session =>

      val result = session.get(EntityRequestBuilders.getRelatedChildrenOfTypeFromRootUid(parent, relationship, typ)).map { _.expectOne }

      flatEntities(result)
    }
  }

  private def flatEntities(result: Promise[Entity]) = {
    result.map { _.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten }
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String) = {
    ops("Couldn't get immediate children of entity: " + parent.getUuid + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootUid(parent, relationship)
      val result = session.get(request).map { _.expectOne }
      flatEntities(result)
    }
  }

  override def getEntityImmediateChildren(parent: ReefUUID, relationship: String, constrainingTypes: List[String]) = {
    ops("Couldn't get immediate children of entity: " + parent.getUuid + " relation: " + relationship) { session =>
      val request = EntityRequestBuilders.getDirectChildrenFromRootUid(parent, relationship, constrainingTypes)
      val result = session.get(request).map { _.expectOne }
      flatEntities(result)
    }
  }

  override def getEntityChildren(parent: ReefUUID, relationship: String, depth: Int, constrainingTypes: List[String]) = {
    ops("Couldn't get tree for entity: " + parent.getUuid + " relation: " + relationship + " depth: " + depth + " types: " + constrainingTypes.toList) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parent, relationship, depth, constrainingTypes)
      session.get(request).map { _.expectOne }
    }
  }

  override def getEntityChildren(parent: ReefUUID, relationship: String, depth: Int) = {
    ops("Couldn't get tree for entity: " + parent.getUuid + " relation: " + relationship + " depth: " + depth) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parent, relationship, depth, Nil)
      session.get(request).map { _.expectOne }
    }
  }

  override def getEntityChildrenFromTypeRoots(parentType: String, relationship: String, depth: Int, constrainingTypes: List[String]) = {
    ops("Couldn't get tree for from type roots: " + parentType + " relation: " + relationship + " depth: " + depth + " types: " + constrainingTypes.toList) { session =>
      val request = EntityRequestBuilders.getChildrenAtDepth(parentType, relationship, depth, constrainingTypes)
      session.get(request).map { _.expectMany() }
    }
  }

  override def getEntityTree(entityTree: Entity) = ops("Couldn't get entity tree matching: " + entityTree) {
    _.get(entityTree).map { _.expectOne }
  }

  override def getEntities(entityTree: Entity) = ops("Couldn't get entity trees matching: " + entityTree) {
    _.get(entityTree).map { _.expectMany() }
  }

  override def getEntityAttributes(uid: ReefUUID) = ops("Couldn't get attributes for entity: " + uid.uuid) {
    _.get(EntityAttributesBuilders.getForEntityUid(uid)).map { _.expectOne }
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
    ops("Couldn't remove attribute for entity: " + uid + " attrName: " + attrName) { session =>
      val prev = getEntityAttributes(uid).await
      val set = prev.getAttributesList.toList.filterNot(_.getName == attrName)
      session.put(EntityAttributesBuilders.putAttributesToEntityUid(uid, set)).map { _.expectOne }
    }
  }

  override def clearEntityAttributes(uid: ReefUUID) = {
    ops("Couldn't clear all attributes for entity: " + uid.uuid) {
      _.delete(EntityAttributesBuilders.getForEntityUid(uid)).map { _.expectOne }
    }
  }

  protected def addSingleAttribute(uid: ReefUUID, attr: Attribute) = {
    ops("Couldn't add attribute for entity: " + uid.uuid + " attr: " + attr) { session =>
      // TODO: fix getEntityAttributes futureness
      val prev = getEntityAttributes(uid).await
      val prevSet = prev.getAttributesList.toList.filterNot(_.getName == attr.getName)

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uid))
      prevSet.foreach(req.addAttributes(_))
      req.addAttributes(attr)

      session.put(req.build).map { _.expectOne }
    }
  }
}

