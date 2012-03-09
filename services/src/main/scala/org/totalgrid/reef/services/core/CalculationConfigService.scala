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

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.framework.SquerylModel._

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.service.proto.Model.{ Entity => EntityProto, Point => PointProto }
import org.totalgrid.reef.models._
import java.util.UUID

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.sapi.types.Optional._
import org.totalgrid.reef.models.UUIDConversions._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.exception.BadRequestException

class CalculationConfigService(protected val model: CalculationConfigServiceModel)
    extends SyncModeledServiceBase[Calculation, CalculationConfig, CalculationConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.calculation
}

class CalculationConfigServiceModel
    extends SquerylServiceModel[Long, Calculation, CalculationConfig]
    with EventedServiceModel[Calculation, CalculationConfig]
    with SimpleModelEntryCreation[Calculation, CalculationConfig]
    with CalculationConfigConversion
    with ServiceModelSystemEventPublisher {

  val entityModel = new EntityServiceModel
  val edgeModel = new EntityEdgeServiceModel

  override def createModelEntry(context: RequestContext, calculation: Calculation): CalculationConfig = {
    val outputPoint = getOutputPoint(context, calculation)

    val uuid: Option[UUID] = calculation.uuid
    if (!uuid.isEmpty) {
      // TODO: allow changing calc output point when we have better uniqueQueries
      // currently we can't 'find' entry with uuid and not current output point
      val existing = entityModel.findRecord(context, EntityProto.newBuilder.setUuid(uuid.get).build)
      if (!existing.isEmpty) {
        val existingCalc = existing.get.asType(ApplicationSchema.calculations, "Calculation")
        throw new BadRequestException("Can't change OutputPoint from: " + existingCalc.outputPoint.value.entityName)
      }
    }

    val entity = entityModel.findOrCreate(context, outputPoint.entityName + "Calc", List("Calculation"), uuid)

    prepareCalculationConfig(context, calculation, entity, outputPoint)
  }

  override def updateModelEntry(context: RequestContext, calculation: Calculation, existing: CalculationConfig) = {
    val outputPoint = getOutputPoint(context, calculation)
    prepareCalculationConfig(context, calculation, existing.entity.value, outputPoint)
  }

  private def getOutputPoint(context: RequestContext, calculation: Calculation): Point = {
    PointServiceConversion.findRecord(context, calculation.getOutputPoint)
      .getOrElse(throw new BadRequestException("Unknown OutputPoint: " + calculation.getOutputPoint))
  }

  private def pointProto(point: Point) = {
    PointProto.newBuilder.setName(point.entityName).setUuid(makeUuid(point)).setUnit(point.unit)
  }

  private def prepareCalculationConfig(context: RequestContext, calculation: Calculation, entity: Entity, outputPoint: Point): CalculationConfig = {

    val inputPoints = calculation.getCalcInputsList.toList.map { i => PointServiceConversion.findRecord(context, i.getPoint) }
    val unknownPoints = inputPoints.filter(_ == None)
    if (!unknownPoints.isEmpty) {
      val missingPoints = inputPoints.zip(calculation.getCalcInputsList.toList.map { _.getPoint.getName }).filter(_._1 == None)
      throw new BadRequestException("Calculation includes unknown InputPoints: " + missingPoints.map { _._2 }.mkString(","))
    }

    val rebuilder = calculation.toBuilder
    rebuilder.setOutputPoint(pointProto(outputPoint))
    rebuilder.setUuid(entity.id)

    rebuilder.clearCalcInputs()
    calculation.getCalcInputsList.toList.zip(inputPoints.flatten).foreach {
      case (input, point) =>
        rebuilder.addCalcInputs(input.toBuilder.setPoint(pointProto(point)))
    }

    val proto = rebuilder.build
    val over = new CalculationConfig(entity.id, outputPoint.id, proto.toByteString.toByteArray)

    over.entity.value = entity
    over.outputPoint.value = outputPoint
    over.proto.value = proto
    over.inputPoints.value = inputPoints.flatten

    over
  }

  override protected def postCreate(context: RequestContext, entry: CalculationConfig) {
    val ent = entry.entity.value

    entry.inputPoints.value.foreach { p =>
      edgeModel.addEdges(context, p.entity.value, List(ent), "calcs", false)
    }
    val outputPoint = entry.outputPoint.value
    edgeModel.addEdges(context, ent, List(outputPoint.entity.value), "calcs", false)
    edgeModel.addEdges(context, outputPoint.entity.value, List(ent), "source", false)

    entityModel.addTypes(context, outputPoint.entity.value, List("CalculatedPoint"))
  }

  override def preDelete(context: RequestContext, entry: CalculationConfig) {

    entityModel.removeTypes(context, entry.outputPoint.value.entity.value, List("CalculatedPoint"))

    entityModel.delete(context, entry.entity.value)
  }

  // we want to undo the previous connections if we are changing output or input points
  override protected def preUpdate(context: RequestContext, entry: CalculationConfig, previous: CalculationConfig) = {
    val calcEntity = entry.entity.value
    if (entry.outputPointId != previous.outputPointId) {
      // this should be unreachable until we fix searching
      val previousOutputPoint = previous.outputPoint.value.entity.value
      edgeModel.deleteEdges(context, calcEntity, List(previousOutputPoint), "calcs")
      edgeModel.deleteEdges(context, previousOutputPoint, List(calcEntity), "source")
      entityModel.removeTypes(context, previousOutputPoint, List("CalculatedPoint"))
    }
    val currentInputs = entry.inputPoints.value
    val previousInputs = previous.inputPoints.value
    previousInputs.diff(currentInputs).foreach { p =>
      edgeModel.deleteEdges(context, p.entity.value, List(calcEntity), "calcs")
    }
    entry
  }

  override protected def postUpdate(context: RequestContext, entry: CalculationConfig, previous: CalculationConfig) = {
    postCreate(context, entry)
  }
}

trait CalculationConfigConversion
    extends UniqueAndSearchQueryable[Calculation, CalculationConfig] {

  import org.squeryl.PrimitiveTypeMode._

  val table = ApplicationSchema.calculations

  def sortResults(list: List[Calculation]) = list.sortBy(_.getOutputPoint.getName)

  def getRoutingKey(req: Calculation) = ProtoRoutingKeys.generateRoutingKey(
    req.outputPoint.endpoint.uuid.value :: req.outputPoint.name :: Nil)

  def uniqueQuery(proto: Calculation, sql: CalculationConfig) = {
    List(proto.uuid.value.asParam(sql.entityId === UUID.fromString(_)),
      proto.outputPoint.map(pointProto => sql.outputPointId in PointServiceConversion.searchQueryForId(pointProto, { _.id })))
  }

  def searchQuery(proto: Calculation, sql: CalculationConfig) =
    List(proto.outputPoint.endpoint.map(logicalNode => sql.entityId in EntityQuery.findIdsOfChildren(logicalNode, "source", "Calculation")))

  def isModified(entry: CalculationConfig, existing: CalculationConfig): Boolean = {
    !entry.protoData.sameElements(existing.protoData)
  }

  def convertToProto(sql: CalculationConfig): Calculation = {
    val builder = sql.proto.value.toBuilder

    // the endpoint is not going to be set when we make the calculation
    builder.setOutputPoint(PointServiceConversion.convertToProto(sql.outputPoint.value))

    builder.build
  }
}