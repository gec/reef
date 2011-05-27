package org.totalgrid.reef.api.request.impl

/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.totalgrid.reef.api.request.{ EntityService }
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.{ EntityAttributes, Entity, ReefUUID }
import org.totalgrid.reef.proto.Utils.Attribute
import org.totalgrid.reef.api.request.builders.{ EntityAttributesBuilders, EntityRequestBuilders }
import org.totalgrid.reef.proto.OptionalProtos._

trait EntityServiceImpl extends ReefServiceBaseClass with EntityService {

  override def getAllEntities(): java.util.List[Entity] = ops("Couldn't get list of all entities") {
    _.get(EntityRequestBuilders.getAll).await().expectMany()
  }

  override def getEntityByUid(uid: ReefUUID): Entity = ops("Couldn't get entity with uuid: " + uid.uuid) {
    _.get(EntityRequestBuilders.getByUid(uid)).await().expectOne
  }

  override def getEntityByName(name: String): Entity = ops("Couldn't get entity with name: " + name) {
    _.get(EntityRequestBuilders.getByName(name)).await().expectOne
  }

  override def getAllEntitiesWithType(typ: String): java.util.List[Entity] = {
    val request = if (typ == "*") EntityRequestBuilders.getAll else EntityRequestBuilders.getByType(typ)
    ops("Couldn't get entities with type: " + typ) { _.get(request).await().expectMany() }
  }

  override def getAllEntitiesWithTypes(typ: java.util.List[String]): java.util.List[Entity] = {
    ops("Couldn't get entities with types: " + typ) {
      _.get(EntityRequestBuilders.getByTypes(typ.toList)).await().expectMany()
    }
  }

  override def getEntityRelatedChildrenOfType(parent: ReefUUID, relationship: String, typ: String): java.util.List[Entity] = {
    ops("Couldn't get children of entity: " + parent.uuid + " relation: " + relationship + " type: " + typ) { session =>

      val result = session.get(EntityRequestBuilders.getRelatedChildrenOfTypeFromRootUid(parent, relationship, typ)).await().expectOne

      val allEntitiesList: List[Entity] = result.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten
      allEntitiesList
    }
  }

  override def getEntityTree(entityTree: Entity): Entity = ops("Couldn't get entity tree matching: " + entityTree) {
    _.get(entityTree).await().expectOne
  }

  override def getEntities(entityTree: Entity): java.util.List[Entity] = ops("Couldn't get entity trees matching: " + entityTree) {
    _.get(entityTree).await().expectMany()
  }

  override def getEntityAttributes(uid: ReefUUID): EntityAttributes = ops("Couldn't get attributes for entity: " + uid.uuid) {
    _.get(EntityAttributesBuilders.getForEntityUid(uid)).await().expectOne
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Boolean): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.boolAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Long): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.longAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Double): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.doubleAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: String): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.stringAttribute(name, value))
  }

  override def setEntityAttribute(uid: ReefUUID, name: String, value: Array[Byte]): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.byteArrayAttribute(name, value))
  }

  override def removeEntityAttribute(uid: ReefUUID, attrName: String): EntityAttributes = {
    ops("Couldn't remove attribute for entity: " + uid + " attrName: " + attrName) { session =>
      val prev = getEntityAttributes(uid)
      val set = prev.getAttributesList.toList.filterNot(_.getName == attrName)
      session.put(EntityAttributesBuilders.putAttributesToEntityUid(uid, set)).await().expectOne
    }
  }

  override def clearEntityAttributes(uid: ReefUUID): EntityAttributes = {
    ops("Couldn't clear all attributes for entity: " + uid.uuid) {
      _.delete(EntityAttributesBuilders.getForEntityUid(uid)).await().expectOne
    }
  }

  protected def addSingleAttribute(uid: ReefUUID, attr: Attribute): EntityAttributes = {
    ops("Couldn't add attribute for entity: " + uid.uuid + " attr: " + attr) { session =>
      val prev = getEntityAttributes(uid)
      val prevSet = prev.getAttributesList.toList.filterNot(_.getName == attr.getName)

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uid))
      prevSet.foreach(req.addAttributes(_))
      req.addAttributes(attr)

      session.put(req.build).await().expectOne
    }
  }
}

