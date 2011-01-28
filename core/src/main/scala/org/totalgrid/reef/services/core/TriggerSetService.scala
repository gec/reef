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

import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.models._

import org.totalgrid.reef.proto.Model.{ Point => PointProto }
import org.totalgrid.reef.proto.Processing.{ TriggerSet => TriggerProto }

import org.totalgrid.reef.services.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.{ ProtoRoutingKeys }
import org.totalgrid.reef.services.framework.SquerylModel._
import org.totalgrid.reef.services.core.OptionalProtos._
import org.squeryl.PrimitiveTypeMode._

class TriggerSetService(protected val modelTrans: ServiceTransactable[TriggerSetServiceModel])
    extends BasicProtoService[TriggerProto, TriggerSet, TriggerSetServiceModel] /*(modelTrans)*/ {

  def deserialize(bytes: Array[Byte]) = TriggerProto.parseFrom(bytes)
  val servedProto: Class[_] = classOf[TriggerProto]
}

class TriggerSetServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[TriggerProto, TriggerSetServiceModel](pub, classOf[TriggerProto]) {

  def model = new TriggerSetServiceModel(subHandler)
}

class TriggerSetServiceModel(protected val subHandler: ServiceSubscriptionHandler)
  extends SquerylServiceModel[TriggerProto, TriggerSet]
  with EventedServiceModel[TriggerProto, TriggerSet]
  with TriggerSetConversion {}

trait TriggerSetConversion
    extends MessageModelConversion[TriggerProto, TriggerSet]
    with UniqueAndSearchQueryable[TriggerProto, TriggerSet] {

  val table = ApplicationSchema.triggerSets

  def getRoutingKey(req: TriggerProto) = ProtoRoutingKeys.generateRoutingKey {
    req.point.logicalNode.uid :: req.point.name :: Nil
  }

  def uniqueQuery(proto: TriggerProto, sql: TriggerSet) = {
    List(proto.point.map(pointProto => sql.pointId in PointServiceConversion.searchQueryForId(pointProto, { _.id })))
  }

  def searchQuery(proto: TriggerProto, sql: TriggerSet) = Nil

  def isModified(entry: TriggerSet, existing: TriggerSet): Boolean = {
    !entry.proto.sameElements(existing.proto)
  }

  def convertToProto(sql: TriggerSet): TriggerProto = {
    TriggerProto.parseFrom(sql.proto)
  }

  def createModelEntry(rawProto: TriggerProto): TriggerSet = {
    val point = PointTiedModel.lookupPoint(rawProto.getPoint)
    val proto = rawProto.toBuilder.setPoint(PointTiedModel.populatedPointProto(point)).build
    new TriggerSet(
      point.id,
      proto.toByteString.toByteArray)
  }
}