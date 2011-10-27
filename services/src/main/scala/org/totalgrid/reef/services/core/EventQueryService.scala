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

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.services.framework._

import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode, LogicalBoolean }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.models.{ ApplicationSchema, EventStore }
import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimpleRead
import org.totalgrid.reef.clientapi.exceptions.BadRequestException

// implicit proto properties
import SquerylModel._ // implict asParam

import scala.collection.JavaConversions._

object EventQueryService {

  def buildQuery(row: EventStore, select: EventSelect): List[Option[LogicalBoolean]] = {

    // Each can have 1 or more lists of values to match against.

    var severityOptions = routingOption(select.getSeverityList) { _.map { _.intValue } }
    if (select.hasSeverityOrHigher) {
      val higherOptions = new Range(1, select.getSeverityOrHigher + 1, 1).toList
      severityOptions = (higherOptions ::: severityOptions).distinct
    }

    List(
      select.getEventTypeList.asParam { row.eventType in _ },
      select.getSubsystemList.asParam { row.subsystem in _ },
      severityOptions.asParam { row.severity in _ },
      select.getUserIdList.asParam { row.userId in _ },
      select.getEntityList.asParam { row.entityId in _.map(EntityQueryManager.idsFromProtoQuery(_)).flatten.distinct },
      select.timeFrom.asParam(row.time gte _),
      select.timeTo.asParam(row.time lte _))
    //select.uuid.uuidAfter.asParam(row.id gt _.toLong))
  }

  def makeSubscriptionKeyParts(select: EventSelect): List[List[String]] = {
    if (select.hasTimeTo) throw new BadRequestException("Illegal subscribe query: timeTo field set, all subscriptions must be live.")

    var severityOptions = routingOption(select.getSeverityList) { _.map { _.toString } }
    if (select.hasSeverityOrHigher) {
      val higherOptions = new Range(1, select.getSeverityOrHigher + 1, 1).map { _.toString }.toList
      severityOptions = (higherOptions ::: severityOptions).distinct
    }

    val routingKeyParts =
      routingOption(select.getEventTypeList) { s => s } ::
        severityOptions ::
        routingOption(select.getSubsystemList) { s => s } ::
        routingOption(select.getUserIdList) { s => s } ::
        routingOption(select.getEntityList) { _.map(EntityQueryManager.idsFromProtoQuery(_)).flatten.distinct.map { _.toString } } :: Nil

    val multipleTypesSubscription = routingKeyParts.filter(_.size > 1)
    if (multipleTypesSubscription.size > 1) {
      throw new BadRequestException("Illegal subscribe query: cannot subscribe using multiple entries in multiple lists, multiple entries allowed in at most 1 list")
    }
    routingKeyParts

  }
}

class EventQueryService
    extends ServiceEntryPoint[EventList]
    with SimpleRead {

  import EventQueryService._

  final override val descriptor = Descriptors.eventList

  override def getSubscribeKeys(req: EventList) = {
    createSubscriptionPermutations(makeSubscriptionKeyParts(req.getSelect)).map { ProtoRoutingKeys.generateRoutingKey(_) }
  }

  override def subscribe(context: RequestContext, req: ServiceType) = {
    context.getHeaders.subQueue.foreach { subQueue =>
      val keys = getSubscribeKeys(req)
      // have to pass an event object so the binding is done to the correct queue
      keys.foreach(context.subHandler.bindQueueByClass(subQueue, _, classOf[Event]))
    }
  }

  override def doGet(context: RequestContext, req: EventList): EventList = {

    if (!req.hasSelect) throw new BadRequestException("Must include select")
    val select = req.getSelect

    val limit = select.limit.getOrElse(1000) // default all queries to max of 1000 events.

    val entries =
      from(ApplicationSchema.events)(row =>
        where(SquerylModel.combineExpressions(buildQuery(row, select).flatten))
          select (row)
          orderBy timeOrder(row.time, select.ascending)).page(0, limit)

    val respList = EventList.newBuilder.addAllEvents(entries.toList.map(EventConversion.convertToProto(_))).build
    respList
  }

  def timeOrder(time: ExpressionNode, ascending: Option[Boolean]) = {
    if (ascending getOrElse false)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }

}

