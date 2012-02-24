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
import org.totalgrid.reef.models.{ EntityQuery, ApplicationSchema, CalculationConfig }
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

  def createModelEntry(context: RequestContext, calculation: Calculation): CalculationConfig = {
    val outputPoint = PointServiceConversion.findRecord(context, calculation.getOutputPoint)
      .getOrElse(throw new BadRequestException("Unknown output point: " + calculation.getOutputPoint))
    val pointEntity = outputPoint.entity.value
    if (pointEntity.types.value.find(_ == "CalculatedPoint").isEmpty) {
      entityModel.findOrCreate(context, pointEntity.name, "CalculatedPoint" :: pointEntity.types.value, Some(pointEntity.id))
    }

    val inputPoints = calculation.getCalcInputsList.toList.map { i => PointServiceConversion.findRecord(context, i.getPoint) }
    val unknownPoints = inputPoints.filter(_ == None)
    if (!unknownPoints.isEmpty) {
      throw new BadRequestException("Calculation includes unknown InputPoints: " + unknownPoints.mkString(","))
    }

    val uuid: Option[UUID] = calculation.uuid
    val ent = entityModel.findOrCreate(context, outputPoint.entityName + "Calc", List("Calculation"), uuid)
    val proto = calculation.toBuilder.setOutputPoint(PointTiedModel.populatedPointProto(outputPoint)).setUuid(ent.id).build
    val over = new CalculationConfig(ent.id, outputPoint.id, proto.toByteString.toByteArray)

    over.outputPoint.value = outputPoint
    over.proto.value = proto

    inputPoints.flatten.foreach { p =>
      edgeModel.addEdge(context, p.entity.value, ent, "calcs")
    }
    edgeModel.addEdge(context, ent, outputPoint.entity.value, "calcs")
    edgeModel.addEdge(context, outputPoint.entity.value, ent, "source")

    over
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
    List(
      proto.uuid.value.asParam(sql.entityId === UUID.fromString(_)),
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