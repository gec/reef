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

import org.totalgrid.reef.models._

import org.totalgrid.reef.client.service.proto.Processing.{ TriggerSet => TriggerProto }

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.services.framework.SquerylModel._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import java.util.UUID
import org.totalgrid.reef.authz.VisibilityMap

class TriggerSetService(protected val model: TriggerSetServiceModel)
    extends SyncModeledServiceBase[TriggerProto, TriggerSet, TriggerSetServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.triggerSet
}

class TriggerSetServiceModel
  extends SquerylServiceModel[Long, TriggerProto, TriggerSet]
  with EventedServiceModel[TriggerProto, TriggerSet]
  with SimpleModelEntryCreation[TriggerProto, TriggerSet]
  with TriggerSetConversion {}

trait TriggerSetConversion
    extends UniqueAndSearchQueryable[TriggerProto, TriggerSet] {

  val table = ApplicationSchema.triggerSets

  def sortResults(list: List[TriggerProto]) = list.sortBy(_.getPoint.getName)

  def getRoutingKey(req: TriggerProto) = ProtoRoutingKeys.generateRoutingKey {
    req.point.endpoint.uuid.value :: req.point.name :: Nil
  }

  def relatedEntities(models: List[TriggerSet]) = {
    models.map { _.point.value.entityId }
  }

  private def resourceId = Descriptors.triggerSet.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: TriggerSet) = {
    sql.id in from(table, ApplicationSchema.points)((over, point) =>
      where(
        (over.pointId === point.id) and
          (point.entityId in entitySelector))
        select (over.id))
  }

  override def selector(map: VisibilityMap, sql: TriggerSet) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }

  override def uniqueQuery(context: RequestContext, proto: TriggerProto, sql: TriggerSet) = {
    List(proto.point.map(pointProto => sql.pointId in PointServiceConversion.searchQueryForId(context, pointProto, { _.id })))
  }

  override def searchQuery(context: RequestContext, proto: TriggerProto, sql: TriggerSet) = Nil

  def isModified(entry: TriggerSet, existing: TriggerSet): Boolean = {
    !entry.proto.sameElements(existing.proto)
  }

  def convertToProto(sql: TriggerSet): TriggerProto = {
    TriggerProto.parseFrom(sql.proto)
  }

  def createModelEntry(context: RequestContext, rawProto: TriggerProto): TriggerSet = {
    val point = PointTiedModel.lookupPoint(context, rawProto.getPoint)
    val proto = rawProto.toBuilder.setPoint(PointTiedModel.populatedPointProto(point)).build
    new TriggerSet(
      point.id,
      proto.toByteString.toByteArray)
  }
}