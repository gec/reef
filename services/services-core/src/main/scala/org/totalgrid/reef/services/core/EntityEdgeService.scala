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

import java.util.UUID
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models._

import org.totalgrid.reef.models.SquerylConversions._
import SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.UUIDConversions._
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Entity => EntityProto, EntityEdge => EntityEdgeProto }

class EntityEdgeService(protected val model: EntityEdgeServiceModel)
    extends SyncModeledServiceBase[EntityEdgeProto, EntityEdge, EntityEdgeServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.entityEdge
}

class EntityEdgeServiceModel
    extends SquerylServiceModel[Long, EntityEdgeProto, EntityEdge]
    with EventedServiceModel[EntityEdgeProto, EntityEdge] {

  val table = ApplicationSchema.edges

  def findRecord(context: RequestContext, req: EntityEdgeProto): Option[EntityEdge] = {
    findRecords(context, req) match {
      case List(head, _) => None
      case List(head) => Some(head)
      case Nil => None
    }
  }

  def findRecords(context: RequestContext, req: EntityEdgeProto): List[EntityEdge] = {
    import ApplicationSchema._

    if (req.hasUuid && req.getUuid.getValue == "*") {
      edges.where(t => true === true).toList
    } else if (req.hasUuid) {
      edges.where(t => t.id === req.getUuid.getValue.toInt).toList
    } else {

      def entExpr(ent: Entity, proto: EntityProto) = {
        proto.uuid.value.asParam(v => ent.id === UUID.fromString(v)) ::
          proto.name.asParam(v => ent.name === v) :: Nil
      }

      def edgeExpr(edge: EntityEdge, proto: EntityEdgeProto) = {
        proto.parent.map(entProto => edge.parentId in from(entities)(ent => where(entExpr(ent, entProto).flatten) select (ent.id))) ::
          proto.child.map(entProto => edge.childId in from(entities)(ent => where(entExpr(ent, entProto).flatten) select (ent.id))) ::
          proto.relationship.asParam(rel => edge.relationship === rel) ::
          proto.distance.map(dist => edge.distance === dist) :: Nil
      }

      from(edges)(edge =>
        where(edgeExpr(edge, req).flatten)
          select (edge)).toList
    }
  }

  def createFromProto(context: RequestContext, req: EntityEdgeProto): EntityEdge = {
    if (!(req.hasParent && req.hasChild)) {
      throw new BadRequestException("Must specify both parent and child")
    }
    if (req.hasDistance && req.getDistance != 1) {
      throw new BadRequestException("Cannot create in-direct (distance != 1) edges")
    }
    val parentEntity = EntityQuery.findEntity(req.getParent).getOrElse(throw new BadRequestException("cannot find parent: " + req.getParent))
    val childEntity = EntityQuery.findEntity(req.getChild).getOrElse(throw new BadRequestException("cannot find child: " + req.getChild))

    if (parentEntity.id == childEntity.id) {
      throw new BadRequestException("Parent and child cannot be same entity")
    }

    val relationship = req.relationship.getOrElse(throw new BadRequestException("must include relationship"))

    addEdge(context, parentEntity, childEntity, relationship)
  }

  def addEdges(context: RequestContext, parent: Entity, children: List[Entity], relation: String, exclusive: Boolean) {
    val childIds = children.map { _.id }
    if (exclusive) {
      val oldEdges = table.where(e => e.distance === 1 and (e.childId in childIds) and e.relationship === relation and (e.parentId <> parent.id)).toList
      oldEdges.foreach(old => delete(context, old))
    }
    val existingEdges = table.where(e => e.distance === 1 and (e.childId in childIds) and e.relationship === relation and e.parentId === parent.id).toList
    val childrenNeedingEdges = children.filterNot { child => existingEdges.find(_.childId == child.id).isDefined }
    childrenNeedingEdges.foreach { child => addEdge(context, parent, child, relation) }
  }

  def deleteEdges(context: RequestContext, parent: Entity, children: List[Entity], relation: String) {
    val childIds = children.map { _.id }
    val existingEdges = table.where(e => e.distance === 1 and (e.childId in childIds) and e.relationship === relation and e.parentId === parent.id).toList
    existingEdges.foreach { e =>
      delete(context, e)
    }
  }

  def addEdge(context: RequestContext, parent: Entity, child: Entity, relation: String) = {
    val originalEdge = create(context, new EntityEdge(parent.id, child.id, relation, 1))
    EntityQuery.getParentsWithDistance(parent.id, relation).foreach { case (ent, dist) => addDerivedEdge(context, ent, child, relation, dist + 1, originalEdge) }
    EntityQuery.getChildrenWithDistance(child.id, relation).foreach { case (ent, dist) => addDerivedEdge(context, parent, ent, relation, dist + 1, originalEdge) }
    originalEdge
  }

  private def addDerivedEdge(context: RequestContext, parent: Entity, child: Entity, relation: String, depth: Int, sourceEdge: EntityEdge) = {
    val derivedEdge = create(context, new EntityEdge(parent.id, child.id, relation, depth))
    ApplicationSchema.derivedEdges.insert(new EntityDerivedEdge(sourceEdge.id, derivedEdge.id))
    derivedEdge
  }

  override def updateFromProto(context: RequestContext, proto: EntityEdgeProto, existing: EntityEdge): (EntityEdge, Boolean) = {
    (existing, false)
  }

  def sortResults(list: List[EntityEdgeProto]): List[EntityEdgeProto] = {
    list
  }

  def getRoutingKey(req: EntityEdgeProto): String = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.value :: req.parent.uuid.value :: req.child.uuid.value :: req.relationship :: Nil
  }

  def relatedEntities(entries: List[EntityEdge]) = {
    entries.map { e => e.parentId :: e.childId :: Nil }.flatten
  }

  def convertToProto(entry: EntityEdge): EntityEdgeProto = {
    import org.totalgrid.reef.services.framework.SquerylModel._

    EntityEdgeProto.newBuilder()
      .setUuid(ReefUUID.newBuilder.setValue(entry.id.toString))
      .setParent(EntityProto.newBuilder.setUuid(makeUuid(entry.parentId)))
      .setChild(EntityProto.newBuilder.setUuid(makeUuid(entry.childId)))
      .setRelationship(entry.relationship)
      .setDistance(entry.distance)
      .build
  }

  def isModified(entry: EntityEdge, previous: EntityEdge): Boolean = {
    false
  }

  override protected def postDelete(context: RequestContext, previous: EntityEdge) {

    val derivedIds = ApplicationSchema.derivedEdges.where(_.edgeId === previous.id).map(_.parentEdgeId)

    ApplicationSchema.derivedEdges.deleteWhere(_.edgeId === previous.id)

    val otherEdges = ApplicationSchema.edges.where(_.id in derivedIds)

    otherEdges.foreach(e => delete(context, e))
  }
}