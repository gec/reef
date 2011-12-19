package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Model.{Entity => EntityProto, EntityEdge => EntityEdgeProto}
import java.util.UUID
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models._


class EntityEdgeModelService(protected val model: EntityEdgeServiceModel)
  extends SyncModeledServiceBase[EntityEdgeProto, EntityEdge, EntityEdgeServiceModel]
  with DefaultSyncBehaviors {

  override val descriptor = Descriptors.entityEdge
}

class EntityEdgeServiceModel
  extends SquerylServiceModel[Long, EntityEdgeProto, EntityEdge]
  with EventedServiceModel[EntityEdgeProto, EntityEdge] {

  val table = ApplicationSchema.edges

  def findRecord(context: RequestContext, req: EntityEdgeProto): Option[EntityEdge] = {
    EntityQuery.findEdges(req) match {
      case List(head, _) => None
      case List(head) => Some(head)
      case Nil => None
    }
  }

  def findRecords(context: RequestContext, req: EntityEdgeProto): List[EntityEdge] = {
    EntityQuery.findEdges(req)
  }

  def createFromProto(context: RequestContext, req: EntityEdgeProto): EntityEdge = {
    val parentEntity = EntityQuery.findEntity(req.getParent).getOrElse(throw new BadRequestException("cannot find parent: " + req.getParent))
    val childEntity = EntityQuery.findEntity(req.getChild).getOrElse(throw new BadRequestException("cannot find child: " + req.getChild))
    val relationship = req.relationship.getOrElse(throw new BadRequestException("must include relationship"))

    addEdge(context, parentEntity, childEntity, relationship)
  }

  private def addEdge(context: RequestContext, parent: Entity, child: Entity, relation: String) = {
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
    null
  }

  def getRoutingKey(req: EntityEdgeProto): String = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.value :: req.parent.uuid.value :: req.child.uuid.value :: req.relationship :: req :: Nil
  }

  def convertToProto(entry: EntityEdge): EntityEdgeProto = {
    null
  }

  def isModified(entry: EntityEdge, previous: EntityEdge): Boolean = {
    true
  }

  override protected def postDelete(context: RequestContext, previous: EntityEdge) {
    null
  }
}