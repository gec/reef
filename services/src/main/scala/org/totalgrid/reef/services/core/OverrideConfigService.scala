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

import org.totalgrid.reef.api.proto.Processing._
import org.totalgrid.reef.models.{ ApplicationSchema, OverrideConfig }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.api.sapi.impl.Descriptors
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.event.{ EventType, SystemEventSink }
import org.totalgrid.reef.api.japi.BadRequestException

//implicits
import org.totalgrid.reef.services.framework.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.api.sapi.impl.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.api.sapi.types.Optional._

class OverrideConfigService(protected val model: OverrideConfigServiceModel)
    extends SyncModeledServiceBase[MeasOverride, OverrideConfig, OverrideConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.measOverride
}

class OverrideConfigServiceModel
    extends SquerylServiceModel[MeasOverride, OverrideConfig]
    with EventedServiceModel[MeasOverride, OverrideConfig]
    with SimpleModelEntryCreation[MeasOverride, OverrideConfig]
    with OverrideConfigConversion
    with ServiceModelSystemEventPublisher {

  override protected def preCreate(context: RequestContext, entry: OverrideConfig): OverrideConfig = {
    if (entry.isOperatorBlockRequest)
      entry
    else
      throw new BadRequestException("Cannot override a point that is in service. First block the point, then override it.")
  }

  override protected def postCreate(context: RequestContext, entry: OverrideConfig) {
    val code = if (entry.proto.value.meas.isDefined) EventType.Scada.SetOverride else EventType.Scada.SetNotInService
    postSystemEvent(context, code, entity = Some(entry.point.value.entity.value))
  }

  override protected def postUpdate(context: RequestContext, entry: OverrideConfig, previous: OverrideConfig) {
    val code = if (entry.proto.value.meas.isDefined) EventType.Scada.SetOverride else EventType.Scada.SetNotInService
    postSystemEvent(context, code, entity = Some(entry.point.value.entity.value))
  }

  override protected def postDelete(context: RequestContext, entry: OverrideConfig) {
    val code = if (entry.proto.value.meas.isDefined) EventType.Scada.RemoveOverride else EventType.Scada.RemoveNotInService
    postSystemEvent(context, code, entity = Some(entry.point.value.entity.value))
  }
}

trait OverrideConfigConversion
    extends UniqueAndSearchQueryable[MeasOverride, OverrideConfig] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._

  val table = ApplicationSchema.overrides

  def getRoutingKey(req: MeasOverride) = ProtoRoutingKeys.generateRoutingKey(
    req.point.logicalNode.uuid.uuid :: req.point.name :: Nil)

  def uniqueQuery(proto: MeasOverride, sql: OverrideConfig) = {
    List(
      proto.point.map(pointProto => sql.pointId in PointServiceConversion.searchQueryForId(pointProto, { _.id })))
  }

  def searchQuery(proto: MeasOverride, sql: OverrideConfig) = Nil

  def isModified(entry: OverrideConfig, existing: OverrideConfig): Boolean = {
    !entry.protoData.sameElements(existing.protoData)
  }

  def convertToProto(sql: OverrideConfig): MeasOverride = {
    sql.proto.value
  }

  def createModelEntry(rawProto: MeasOverride): OverrideConfig = {
    val point = PointTiedModel.lookupPoint(rawProto.getPoint)
    val proto = rawProto.toBuilder.setPoint(PointTiedModel.populatedPointProto(point)).build
    val over = new OverrideConfig(
      point.id,
      proto.toByteString.toByteArray)
    over.point.value = point
    over.proto.value = proto
    over
  }
}