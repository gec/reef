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

import org.totalgrid.reef.models.{ ApplicationSchema, Point, Entity }
import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.japi.BadRequestException
import org.totalgrid.reef.proto.Model.{ PointType, Point => PointProto, Entity => EntityProto }
import org.totalgrid.reef.sapi.{ RequestEnv, AllMessages }
import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.services.coordinators.CommunicationEndpointOfflineBehaviors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class PointService(protected val model: PointServiceModel)
    extends SyncModeledServiceBase[PointProto, Point, PointServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.point

  override def preCreate(context: RequestContext, proto: PointProto) = {
    if (!proto.hasName || !proto.hasUnit || !proto.hasType) {
      throw new BadRequestException("Must specify name, type and unit when creating point")
    }
    proto
  }

  override def preUpdate(context: RequestContext, request: PointProto, existing: Point) = {
    preCreate(context, request)
  }

}

class PointServiceModel(triggerModel: TriggerSetServiceModel,
  overrideModel: OverrideConfigServiceModel,
  val measurementStore: MeasurementStore)
    extends SquerylServiceModel[PointProto, Point]
    with EventedServiceModel[PointProto, Point]
    with SimpleModelEntryCreation[PointProto, Point]
    with PointServiceConversion
    with CommunicationEndpointOfflineBehaviors {

  /**
   * we override this function so we can publish events with the "abnormalUpdated" part of routing
   * key filled out from the transient field on the sql object
   */
  override def getEventProtoAndKey(entry: Point) = {
    val proto = convertToProto(entry)
    val key = getRoutingKey(proto, entry)
    (proto, key :: Nil)
  }

  def createAndSetOwningNode(context: RequestContext, points: List[String], dataSource: Entity): Unit = {
    if (points.size == 0) return
    //TODO: combine the createAndSet for points and commands
    val alreadyExistingPoints = Entity.asType(ApplicationSchema.points, EQ.findEntitiesByName(points).toList, Some("Point"))
    val newPoints = points.diff(alreadyExistingPoints.map(_.entityName).toList)
    if (!newPoints.isEmpty) throw new BadRequestException("Trying to set endpoint for unknown points: " + newPoints)

    val changePointOwner = alreadyExistingPoints.filter { c =>
      c.sourceEdge.value.map(_.parentId != dataSource.id) getOrElse (true)
    }
    changePointOwner.foreach(p => {
      p.sourceEdge.value.foreach(EQ.deleteEdge(_))
      EQ.addEdge(dataSource, p.entity.value, "source")
      update(context, p, p)
    })
  }

  override def postCreate(context: RequestContext, entry: Point) {
    markPointsOffline(entry :: Nil)
  }

  override def preDelete(context: RequestContext, entry: Point) {
    entry.logicalNode.value match {
      case Some(parent) =>
        throw new BadRequestException("Cannot delete point: " + entry.entityName + " while it is still assigned to logicalNode " + parent.name)
      case None => // no endpoint so we are free to delete point
    }
  }

  override def postDelete(context: RequestContext, entry: Point) {

    entry.triggers.value.foreach { t => triggerModel.delete(context, t) }
    entry.overrides.value.foreach { o => overrideModel.delete(context, o) }

    measurementStore.remove(entry.entityName :: Nil)

    EQ.deleteEntity(entry.entity.value)
  }
}

trait PointServiceConversion extends UniqueAndSearchQueryable[PointProto, Point] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._

  val table = ApplicationSchema.points

  /**
   * this is the subscription routingKey
   */
  def getRoutingKey(req: PointProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.uuid ::
      req.name ::
      req.entity.uuid.uuid ::
      req.abnormal :: // this subscribes the users to all points that have their abnormal field changed
      Nil
  }

  /**
   * this is the service event notifaction routingKey
   */
  def getRoutingKey(req: PointProto, entry: Point) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.uuid ::
      req.name ::
      req.entity.uuid.uuid ::
      Some(entry.abnormalUpdated) :: // we actually publish the key to intrested parties on change, not on current state
      Nil
  }

  def uniqueQuery(proto: PointProto, sql: Point) = {

    val eSearch = EntitySearch(proto.uuid.uuid, proto.name, proto.name.map(x => List("Point")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })),
      proto.entity.map(ent => sql.entityId in EQ.typeIdsFromProtoQuery(ent, "Point")),
      proto.logicalNode.map(logicalNode => sql.entityId in EQ.findIdsOfChildren(logicalNode, "source", "Point")))
  }

  def searchQuery(proto: PointProto, sql: Point) = List(proto.abnormal.asParam(sql.abnormal === _))

  def isModified(entry: Point, existing: Point): Boolean = {
    entry.abnormal != existing.abnormal
  }

  def convertToProto(sql: Point): PointProto = {
    val b = PointProto.newBuilder()

    b.setUuid(makeUuid(sql))
    b.setName(sql.entityName)
    sql.entity.asOption.foreach(e => b.setEntity(EQ.entityToProto(e)))

    sql.logicalNode.value // autoload logicalNode
    sql.logicalNode.asOption.foreach { _.foreach { ln => b.setLogicalNode(EQ.minimalEntityToProto(ln).build) } }
    b.setAbnormal(sql.abnormal)
    b.setType(PointType.valueOf(sql.pointType))
    b.setUnit(sql.unit)
    b.build
  }

  def createModelEntry(proto: PointProto): Point = {
    Point.newInstance(proto.name.get, false, None, proto.getType.getNumber, proto.getUnit)
  }

}

object PointServiceConversion extends PointServiceConversion

/**
 * temporary trait shared by all of the models that are closly tied to a single point, should be replaced
 * with refactored searching behaviors.
 */
object PointTiedModel {
  def lookupPoint(proto: PointProto): Point = {
    Point.findByName(proto.getName).single
  }

  /**
   * "Fully" fills out the point proto, makes it routable and more consumable on the client side
   */
  def populatedPointProto(point: Point): PointProto.Builder = {
    val pb = PointProto.newBuilder
    pb.setName(point.entityName).setUuid(makeUuid(point))
    pb.setEntity(EQ.entityToProto(point.entity.value))
    point.logicalNode.value.foreach(p => pb.setLogicalNode(EQ.entityToProto(p)))
    pb
  }
}

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.proto.Measurements.{ Measurement, Quality }
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.services.ProtoServiceCoordinator

import org.totalgrid.reef.util.Conversion.convertIterableToMapified

/**
 * Watches the measurement stream looking for transitions in the abnormal value of points
 * TODO: move to its own file
 */
class PointAbnormalsThunker(model: PointServiceModel, summary: SummaryPoints, contextSource: RequestContextSource) extends ProtoServiceCoordinator with Logging {

  val pointMap = scala.collection.mutable.Map.empty[String, Point]

  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Executor) {
    // TODO: assign abnormal thunkers to communication streams
    reactor.execute {
      transaction {
        ApplicationSchema.points.where(p => true === true).toList.foreach { p: Point => pointMap.put(p.entityName, p) }
      }
      val startedAbnormal = pointMap.values.foldLeft(0) { case (sum, p) => if (p.abnormal) sum + 1 else sum }
      summary.setSummary("summary.abnormals", startedAbnormal)
    }
    amqp.subscribe("measurement", AllMessages, Measurement.parseFrom(_), { msg: Measurement => reactor.execute { handleMeasurement(msg) } })
  }

  def handleMeasurement(m: Measurement): Unit = {

    val point = pointMap.get(m.getName) match {
      case Some(p) => p
      case None =>
        val p = transaction {
          Point.findByName(m.getName).headOption
        }
        if (p.isEmpty) {
          logger.error { "Got measurement for unknown point: " + m.getName }
          return
        } else {
          if (p.get.abnormal) summary.incrementSummary("summary.abnormals", 1)
          pointMap.put(m.getName, p.get)
          p.get
        }
    }

    val currentlyAbnormal = m.getQuality.getValidity != Quality.Validity.GOOD

    if (currentlyAbnormal != point.abnormal) {
      contextSource.transaction { context =>
        logger.debug("updated point: " + m.getName + " to abnormal= " + currentlyAbnormal)
        val updated = point.copy(abnormal = currentlyAbnormal)
        updated.abnormalUpdated = true
        model.update(context, updated, point)
        point.abnormal = currentlyAbnormal
        summary.incrementSummary("summary.abnormals", if (point.abnormal) 1 else -1)
      }
    }
  }
}

