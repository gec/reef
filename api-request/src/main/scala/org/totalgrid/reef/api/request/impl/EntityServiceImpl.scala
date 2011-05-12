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

trait EntityServiceImpl extends ReefServiceBaseClass with EntityService {

  def getAllEntities(): java.util.List[Entity] = {
    ops { _.getOrThrow(EntityRequestBuilders.getAll) }
  }

  def getEntityByUid(uid: ReefUUID): Entity = {
    ops { _.getOneOrThrow(EntityRequestBuilders.getByUid(uid)) }
  }

  def getEntityByName(name: String): Entity = {
    ops { _.getOneOrThrow(EntityRequestBuilders.getByName(name)) }
  }
  def getAllEntitiesWithType(typ: String): java.util.List[Entity] = {
    if (typ == "*") ops { _.getOrThrow(EntityRequestBuilders.getAll) }
    else ops { _.getOrThrow(EntityRequestBuilders.getByType(typ)) }
  }

  def getAllEntitiesWithTypes(typ: java.util.List[String]): java.util.List[Entity] = {
    ops { _.getOrThrow(EntityRequestBuilders.getByTypes(typ.toList)) }
  }

  def getEntityRelatedChildrenOfType(parent: ReefUUID, relationship: String, typ: String): java.util.List[Entity] = {
    ops { session =>
      val result = session.getOneOrThrow(EntityRequestBuilders.getRelatedChildrenOfTypeFromRootUid(parent, relationship, typ))

      val allEntitiesList: List[Entity] = result.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten
      allEntitiesList
    }
  }
  def getEntityTree(entityTree: Entity): Entity = {
    ops { _.getOneOrThrow(entityTree) }
  }

  def getEntities(entityTree: Entity): java.util.List[Entity] = {
    ops { _.getOrThrow(entityTree) }
  }

  def getEntityAttributes(uid: ReefUUID): EntityAttributes = {
    ops { _.getOneOrThrow(EntityAttributesBuilders.getForEntityUid(uid)) }
  }

  def setEntityAttribute(uid: ReefUUID, name: String, value: Boolean): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.boolAttribute(name, value))
  }

  def setEntityAttribute(uid: ReefUUID, name: String, value: Long): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.longAttribute(name, value))
  }

  def setEntityAttribute(uid: ReefUUID, name: String, value: Double): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.doubleAttribute(name, value))
  }

  def setEntityAttribute(uid: ReefUUID, name: String, value: String): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.stringAttribute(name, value))
  }

  def setEntityAttribute(uid: ReefUUID, name: String, value: Array[Byte]): EntityAttributes = {
    addSingleAttribute(uid, EntityAttributesBuilders.byteArrayAttribute(name, value))
  }

  def removeEntityAttribute(uid: ReefUUID, attrName: String): EntityAttributes = {
    ops { session =>
      val prev = getEntityAttributes(uid)
      val set = prev.getAttributesList.toList.filterNot(_.getName == attrName)
      session.putOneOrThrow(EntityAttributesBuilders.putAttributesToEntityUid(uid, set))
    }
  }

  def clearEntityAttributes(uid: ReefUUID): EntityAttributes = {
    ops { _.deleteOneOrThrow(EntityAttributesBuilders.getForEntityUid(uid)) }
  }

  protected def addSingleAttribute(uid: ReefUUID, attr: Attribute): EntityAttributes = {
    ops { session =>
      val prev = getEntityAttributes(uid)
      val prevSet = prev.getAttributesList.toList.filterNot(_.getName == attr.getName)

      val req = EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uid))
      prevSet.foreach(req.addAttributes(_))
      req.addAttributes(attr)

      session.putOneOrThrow(req.build)
    }
  }
}

