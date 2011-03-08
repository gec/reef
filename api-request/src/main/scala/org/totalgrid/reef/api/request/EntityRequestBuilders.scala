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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Model.{ Entity, Relationship }

object EntityRequestBuilders {

  def getAll = Entity.newBuilder.setUid("*").build

  def getByUid(uid: String) = Entity.newBuilder.setUid(uid).build
  def getByUid(entity: Entity): Entity = getByUid(entity.getUid)

  def getByName(name: String) = Entity.newBuilder.setName(name).build

  def getByType(typ: String) = getByTypes(List(typ))
  def getByTypes(typs: List[String]) = {
    val req = Entity.newBuilder
    typs.foreach(typ => req.addTypes(typ))
    req.build
  }

  def getOwnedChildrenOfTypeFromRootName(rootNodeName: String, typ: String) = {
    Entity.newBuilder.setName(rootNodeName).addRelations(childrenRelatedWithType("owns", typ)).build
  }

  def getOwnedChildrenOfTypeFromRootUid(rootUid: String, typ: String): Entity = {
    Entity.newBuilder.setUid(rootUid).addRelations(childrenRelatedWithType("owns", typ)).build
  }
  def getOwnedChildrenOfTypeFromRootUid(rootNode: Entity, typ: String): Entity = {
    Entity.newBuilder.setUid(rootNode.getUid).addRelations(childrenRelatedWithType("owns", typ)).build
  }

  private def childrenRelatedWithType(relationship: String, typ: String) = {
    Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship("owns")
      .addEntities(Entity.newBuilder.addTypes(typ))
  }

  def getAllRelatedChildrenFromRootUid(rootUid: String, relationship: String) = {
    val rel = Relationship.newBuilder.setDescendantOf(true).setRelationship(relationship)
    Entity.newBuilder.setUid(rootUid).addRelations(rel).build
  }

  def getDirectChildrenFromRootUid(rootUid: String, relationship: String) = {
    val rel = Relationship.newBuilder.setDescendantOf(true).setRelationship(relationship).setDistance(1)
    Entity.newBuilder.setUid(rootUid).addRelations(rel).build
  }

  def getAllPointsSortedByOwningEquipment(rootUid: String) = {
    Entity.newBuilder.setUid(rootUid).addRelations(
      Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
        Entity.newBuilder.addTypes("Equipment").addRelations(
          Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
            Entity.newBuilder.addTypes("Point"))))).build
  }

  def getAllPointsAndRelatedFeedbackCommands() = {
    Entity.newBuilder.addTypes("Point").addRelations(
      Relationship.newBuilder.setDescendantOf(true).setRelationship("feedback").addEntities(
        Entity.newBuilder.addTypes("Command"))).build
  }

}