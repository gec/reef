/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.models.{ ApplicationSchema, Point, Entity }
import org.totalgrid.reef.proto.Model.{ Point => PointProto }
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.Descriptors

import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.messaging.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class PointService(protected val modelTrans: ServiceTransactable[PointServiceModel])
    extends BasicProtoService[PointProto, Point, PointServiceModel] {

  override val descriptor = Descriptors.point
}

class PointServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[PointProto, PointServiceModel](pub, classOf[PointProto]) {

  def model = new PointServiceModel(subHandler)
}

class PointServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[PointProto, Point]
    with EventedServiceModel[PointProto, Point]
    with PointServiceConversion {

  /**
   * we override this function so we can publish events with the "abnormalUpdated" part of routing
   * key filled out from the transient field on the sql object
   */
  override def getEventProtoAndKey(entry: Point) = {
    val proto = convertToProto(entry)
    val key = getRoutingKey(proto, entry)
    (proto, key :: Nil)
  }

  def createAndSetOwningNode(points: List[String], dataSource: Entity): Unit = {
    if (points.size == 0) return
    //TODO: combine the createAndSet for points and commands
    val allreadyExistingPoints = Entity.asType(ApplicationSchema.points, EQ.findEntitiesByName(points).toList, Some("Point"))

    val changePointOwner = allreadyExistingPoints.filter { c =>
      c.sourceEdge.value.map(_.parentId != dataSource.id) getOrElse (true)
    }
    changePointOwner.foreach(p => {
      p.sourceEdge.value.foreach(EQ.deleteEdge(_))
      EQ.addEdge(dataSource, p.entity.value, "source")
      update(p, p)
    })

    val newPoints = points.diff(allreadyExistingPoints.map(_.name).toList)
    newPoints.foreach(pname => {
      create(makePointEntry(pname, false, Some(dataSource)))
    })

  }
}

trait PointServiceConversion extends MessageModelConversion[PointProto, Point] with UniqueAndSearchQueryable[PointProto, Point] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._

  val table = ApplicationSchema.points

  /**
   * this is the subscription routingKey
   */
  def getRoutingKey(req: PointProto) = ProtoRoutingKeys.generateRoutingKey {
    req.uid ::
      req.name ::
      req.entity.uid ::
      req.abnormal :: // this subscribes the users to all points that have their abnormal field changed
      Nil
  }

  /**
   * this is the service event notifaction routingKey
   */
  def getRoutingKey(req: PointProto, entry: Point) = ProtoRoutingKeys.generateRoutingKey {
    req.uid ::
      req.name ::
      req.entity.uid ::
      Some(entry.abnormalUpdated) :: // we actually publish the key to intrested parties on change, not on current state
      Nil
  }

  def uniqueQuery(proto: PointProto, sql: Point) = {
    List(
      proto.uid.asParam(sql.id === _.toLong),
      proto.name.asParam(sql.name === _),
      //proto.entity.map(entity => sql.entityId in EntitySearches.searchQueryForId(entity, { _.id })),
      proto.entity.map(ent => sql.entityId in EQ.typeIdsFromProtoQuery(ent, "Point")),
      proto.logicalNode.map(logicalNode => sql.entityId in EQ.findIdsOfChildren(logicalNode, "source", "Point")))
  }

  def searchQuery(proto: PointProto, sql: Point) = List(proto.abnormal.asParam(sql.abnormal === _))

  def isModified(entry: Point, existing: Point): Boolean = {
    entry.abnormal != existing.abnormal
  }

  def convertToProto(sql: Point): PointProto = {
    val b = PointProto.newBuilder()

    b.setUid(sql.id.toString)
    b.setName(sql.name)
    sql.entity.asOption.foreach(e => b.setEntity(EQ.entityToProto(e)))
    sql.logicalNode.asOption.foreach(_.foreach(ln => b.setLogicalNode(EntityProto.newBuilder.setUid(ln.id.toString).setName(ln.name))))
    b.setAbnormal(sql.abnormal)
    b.build
  }

  def createModelEntry(proto: PointProto): Point = {
    makePointEntry(proto.name.get, false, None)
  }

  def makePointEntry(pname: String, abnormal: Boolean, dataSource: Option[Entity]) = {
    val ent = EQ.findOrCreateEntity(pname, "Point")
    val p = new Point(pname, ent.id, false)
    dataSource.foreach(ln => { EQ.addEdge(ln, ent, "source"); p.logicalNode.value = Some(ln) })
    p.entity.value = ent
    p
  }
}

object PointServiceConversion extends PointServiceConversion

/**
 * temporary trait shared by all of the models that are closly tied to a single point, should be replaced
 * with refactored searching behaviors.
 */
object PointTiedModel {
  def lookupPoint(proto: PointProto): Point = {
    ApplicationSchema.points.where(p => p.name === proto.getName).single
  }

  /**
   * "Fully" fills out the point proto, makes it routable and more consumable on the client side
   */
  def populatedPointProto(point: Point): PointProto.Builder = {
    val pb = PointProto.newBuilder
    pb.setName(point.name).setUid(point.id.toString)
    pb.setEntity(EQ.entityToProto(point.entity.value))
    point.logicalNode.value.foreach(p => pb.setLogicalNode(EQ.entityToProto(p)))
    pb
  }
}

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.proto.Measurements.{ Measurement, Quality }
import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.services.ProtoServiceCoordinator

import org.totalgrid.reef.util.Conversion.convertIterableToMapified

/**
 * Watches the measurement stream looking for transitions in the abnormal value of points
 * TODO: move to its own file
 */
class PointAbnormalsThunker(trans: ServiceTransactable[PointServiceModel], summary: SummaryPoints) extends ProtoServiceCoordinator with Logging {

  val pointMap = scala.collection.mutable.Map.empty[String, Point]

  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Reactable) {
    // TODO: assign abnormal thunkers to communication streams
    reactor.execute {
      transaction {
        ApplicationSchema.points.where(p => true === true).toList.foreach { p: Point => pointMap.put(p.name, p) }
      }
      val startedAbnormal = pointMap.values.foldLeft(0) { case (sum, p) => if (p.abnormal) sum + 1 else sum }
      summary.setSummary("summary.abnormals", startedAbnormal)
    }
    amqp.subscribe("measurement", "#", Measurement.parseFrom(_), { msg: Measurement => reactor.execute { handleMeasurement(msg) } })
  }

  def handleMeasurement(m: Measurement): Unit = {

    val point = pointMap.get(m.getName) match {
      case Some(p) => p
      case None =>
        val p = transaction {
          ApplicationSchema.points.where(p => p.name === m.getName).headOption
        }
        if (p.isEmpty) {
          error { "Got measurement for unknown point: " + m.getName }
          return
        } else {
          if (p.get.abnormal) summary.incrementSummary("summary.abnormals", 1)
          pointMap.put(m.getName, p.get)
          p.get
        }
    }

    val currentlyAbnormal = m.getQuality.getValidity != Quality.Validity.GOOD

    if (currentlyAbnormal != point.abnormal) {
      trans.transaction { model =>
        debug { "updated point: " + m.getName + " to abnormal= " + currentlyAbnormal }
        val updated = point.copy(abnormal = currentlyAbnormal)
        updated.abnormalUpdated = true
        model.update(updated, point)
        point.abnormal = currentlyAbnormal
        summary.incrementSummary("summary.abnormals", if (point.abnormal) 1 else -1)
      }
    }
  }
}

