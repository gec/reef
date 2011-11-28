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
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.services.framework.ProtoSerializer._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.services.core.util.UUIDConversions._

import org.totalgrid.reef.clientapi.exceptions.BadRequestException

import org.totalgrid.reef.proto.Model.{ PointType, Point => PointProto, Entity => EntityProto }
import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.services.coordinators.CommunicationEndpointOfflineBehaviors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.clientapi.sapi.types.Optional._

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

    EntityQueryManager.deleteEntity(entry.entity.value)
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
    req.uuid.value ::
      req.name ::
      req.entity.uuid.value ::
      req.abnormal :: // this subscribes the users to all points that have their abnormal field changed
      Nil
  }

  /**
   * this is the service event notifaction routingKey
   */
  def getRoutingKey(req: PointProto, entry: Point) = ProtoRoutingKeys.generateRoutingKey {
    req.uuid.value ::
      req.name ::
      req.entity.uuid.value ::
      Some(entry.abnormalUpdated) :: // we actually publish the key to intrested parties on change, not on current state
      Nil
  }

  def uniqueQuery(proto: PointProto, sql: Point) = {

    val eSearch = EntitySearch(proto.uuid.value, proto.name, proto.name.map(x => List("Point")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })),
      proto.entity.map(ent => sql.entityId in EntityQueryManager.typeIdsFromProtoQuery(ent, "Point")))
  }

  def searchQuery(proto: PointProto, sql: Point) = List(proto.abnormal.asParam(sql.abnormal === _),
    proto.endpoint.map(logicalNode => sql.entityId in EntityQueryManager.findIdsOfChildren(logicalNode, "source", "Point")))

  def isModified(entry: Point, existing: Point): Boolean = {
    entry.abnormal != existing.abnormal
  }

  def convertToProto(sql: Point): PointProto = {
    val b = PointProto.newBuilder()

    b.setUuid(makeUuid(sql))
    b.setName(sql.entityName)
    sql.entity.asOption.foreach(e => b.setEntity(EntityQueryManager.entityToProto(e)))

    sql.logicalNode.value // autoload logicalNode
    sql.logicalNode.asOption.foreach { _.foreach { ln => b.setEndpoint(EntityQueryManager.minimalEntityToProto(ln).build) } }
    b.setAbnormal(sql.abnormal)
    b.setType(PointType.valueOf(sql.pointType))
    b.setUnit(sql.unit)
    b.build
  }

  def createModelEntry(proto: PointProto): Point = {
    Point.newInstance(proto.name.get, false, None, proto.getType, proto.getUnit, proto.uuid)
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
    pb.setEntity(EntityQueryManager.entityToProto(point.entity.value))
    point.logicalNode.value.foreach(p => pb.setEndpoint(EntityQueryManager.entityToProto(p)))
    pb
  }
}
