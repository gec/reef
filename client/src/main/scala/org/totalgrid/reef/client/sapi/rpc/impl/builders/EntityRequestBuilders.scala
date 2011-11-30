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
package org.totalgrid.reef.client.sapi.rpc.impl.builders

import org.totalgrid.reef.proto.Model.{ Entity, Relationship, ReefUUID }

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.entities.EntityRelation
import org.totalgrid.reef.clientapi.exceptions.BadRequestException

object EntityRequestBuilders {

  def getAll = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build

  def getById(uuid: ReefUUID) = Entity.newBuilder.setUuid(uuid).build
  def getById(entity: Entity): Entity = getById(entity.getUuid)

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

  def getOwnedChildrenOfTypeFromRootId(rootId: ReefUUID, typ: String): Entity = {
    Entity.newBuilder.setUuid(rootId).addRelations(childrenRelatedWithType("owns", typ)).build
  }
  def getOwnedChildrenOfTypeFromRootId(rootNode: Entity, typ: String): Entity = {
    Entity.newBuilder.setUuid(rootNode.getUuid).addRelations(childrenRelatedWithType("owns", typ)).build
  }

  def getRelatedChildrenOfTypeFromRootId(rootNode: ReefUUID, relationship: String, typ: String): Entity = {
    Entity.newBuilder.setUuid(rootNode).addRelations(childrenRelatedWithType(relationship, typ)).build
  }

  private def childrenRelatedWithType(relationship: String, typ: String) = {
    Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship(relationship)
      .addEntities(Entity.newBuilder.addTypes(typ))
  }

  def getAllRelatedChildrenFromRootId(rootId: ReefUUID, relationship: String) = {
    val rel = Relationship.newBuilder.setDescendantOf(true).setRelationship(relationship)
    Entity.newBuilder.setUuid(rootId).addRelations(rel).build
  }

  def getDirectChildrenFromRootId(rootId: ReefUUID, relationship: String) = {
    val rel = Relationship.newBuilder.setDescendantOf(true).setRelationship(relationship).setDistance(1)
    Entity.newBuilder.setUuid(rootId).addRelations(rel).build
  }

  def getDirectChildrenFromRootId(rootId: ReefUUID, relationship: String, constrainingTypes: java.util.List[String]) = {
    val constraint = Entity.newBuilder.addAllTypes(constrainingTypes)
    val rel = Relationship.newBuilder.setDescendantOf(true).setRelationship(relationship).setDistance(1).addEntities(constraint)

    Entity.newBuilder.setUuid(rootId).addRelations(rel).build
  }

  private def childrenRelatedWithTypeRecursive(relationship: String, types: java.util.List[String], depth: Int): Relationship.Builder = {
    val child = if (depth == 0) {
      Entity.newBuilder.addAllTypes(types)
    } else {
      Entity.newBuilder.addAllTypes(types).addRelations(childrenRelatedWithTypeRecursive(relationship, types, depth - 1))
    }
    Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship(relationship)
      .setDistance(1)
      .addEntities(child)
  }

  def getChildrenAtDepth(rootId: ReefUUID, relationship: String, depth: Int, constrainingTypes: java.util.List[String]) = {
    Entity.newBuilder.setUuid(rootId).addRelations(childrenRelatedWithTypeRecursive(relationship, constrainingTypes, depth - 1)).build
  }

  def getChildrenAtDepth(rootType: String, relationship: String, depth: Int, constrainingTypes: java.util.List[String]) = {
    Entity.newBuilder.addTypes(rootType).addRelations(childrenRelatedWithTypeRecursive(relationship, constrainingTypes, depth - 1)).build
  }

  private case class Relation(relationship: String, types: List[String], descendent: Boolean, depth: Option[Int])

  private def makeRelation(r: EntityRelation) = {

    val depth = r.getDepth match {
      case -1 => None
      case x: Int if x <= 0 => throw new BadRequestException("Depth must be positive")
      case x: Int => Some(x)
    }

    Relation(r.getRelationship, Option(r.getTypes).map { _.toList }.getOrElse(Nil), r.getChild, depth)
  }

  private def makeRelations(relations: List[EntityRelation]) = {
    if (relations.isEmpty) throw new BadRequestException("Must include atleast one relation in request")
    relations.map { makeRelation(_) }
  }

  private def relatedByRelation(relations: List[Relation]): Relationship.Builder = {
    val relation = relations.head
    val child = if (relations.tail.isEmpty) {
      Entity.newBuilder.addAllTypes(relation.types)
    } else {
      Entity.newBuilder.addAllTypes(relation.types).addRelations(relatedByRelation(relations.tail))
    }
    val r = Relationship.newBuilder
      .setDescendantOf(relation.descendent)
      .setRelationship(relation.relationship)
      .addEntities(child)
    relation.depth.foreach { r.setDistance(_) }
    r
  }

  def getRelatedEntities(rootType: String, relations: List[EntityRelation]) = {
    Entity.newBuilder.addTypes(rootType).addRelations(relatedByRelation(makeRelations(relations))).build
  }
  def getRelatedEntities(rootUuid: ReefUUID, relations: List[EntityRelation]) = {
    Entity.newBuilder.setUuid(rootUuid).addRelations(relatedByRelation(makeRelations(relations))).build
  }

  def getAllPointsSortedByOwningEquipment(rootId: ReefUUID) = {
    Entity.newBuilder.setUuid(rootId).addRelations(
      Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
        Entity.newBuilder.addTypes("Equipment").addRelations(
          Relationship.newBuilder.setDescendantOf(true).setRelationship("owns").addEntities(
            Entity.newBuilder.addTypes("Point"))))).build
  }

  def getAllPointsAndRelatedFeedbackCommands() = {
    Entity.newBuilder.addTypes("Point").addRelations(getAllFeedBackCommands()).build
  }

  def getAllFeedBackCommands() = {
    Relationship.newBuilder.setDescendantOf(true).setRelationship("feedback").addEntities(Entity.newBuilder.addTypes("Command"))
  }

  def getPointsFeedbackCommands(uuid: ReefUUID) = {
    Entity.newBuilder.setUuid(uuid).addRelations(getAllFeedBackCommands()).build
  }

  def getAllFedBackPoints() = {
    Relationship.newBuilder.setDescendantOf(false).setRelationship("feedback").addEntities(Entity.newBuilder.addTypes("Point"))
  }

  def getCommandsFeedbackPoints(uuid: ReefUUID) = {
    Entity.newBuilder.setUuid(uuid).addRelations(getAllFedBackPoints()).build
  }

  def optionalChildrenSelector(parentName: String, relType: Option[String], subTypes: List[String], anyDepth: Boolean) = {
    val req = Entity.newBuilder.setName(parentName)

    val rel = Relationship.newBuilder
      .setDescendantOf(true)
      .setRelationship(relType getOrElse ("owns"))

    if (!anyDepth) rel.setDistance(1)
    subTypes.foreach(typ => rel.addEntities(Entity.newBuilder.addTypes(typ)))
    req.addRelations(rel).build
  }

  def extractChildren(entity: Entity) = {
    entity.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten.toList
  }

  def extractChildrenUuids(entity: Entity) = {
    entity.getRelationsList.toList.map { _.getEntitiesList.toList }.flatten.toList.map { _.getUuid }
  }
}
