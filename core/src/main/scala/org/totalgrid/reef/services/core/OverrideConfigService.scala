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

import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.models.{ ApplicationSchema, OverrideConfig }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors

//implicits
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class OverrideConfigService(protected val modelTrans: ServiceTransactable[OverrideConfigServiceModel])
    extends SyncModeledServiceBase[MeasOverride, OverrideConfig, OverrideConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.measOverride
}

class OverrideConfigModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[MeasOverride, OverrideConfigServiceModel](pub, classOf[MeasOverride]) {

  def model = new OverrideConfigServiceModel(subHandler)
}

class OverrideConfigServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[MeasOverride, OverrideConfig]
    with EventedServiceModel[MeasOverride, OverrideConfig]
    with OverrideConfigConversion {
}

trait OverrideConfigConversion
    extends MessageModelConversion[MeasOverride, OverrideConfig]
    with UniqueAndSearchQueryable[MeasOverride, OverrideConfig] {

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
    !entry.proto.sameElements(existing.proto)
  }

  def convertToProto(sql: OverrideConfig): MeasOverride = {
    MeasOverride.parseFrom(sql.proto)
  }

  def createModelEntry(rawProto: MeasOverride): OverrideConfig = {
    val point = PointTiedModel.lookupPoint(rawProto.getPoint)
    val proto = rawProto.toBuilder.setPoint(PointTiedModel.populatedPointProto(point)).build
    new OverrideConfig(
      point.id,
      proto.toByteString.toByteArray)
  }
}