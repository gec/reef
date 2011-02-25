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

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.models.EventStore
import org.totalgrid.reef.messaging.ServiceEndpoint; import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.ServiceTypes.Response

import org.totalgrid.reef.services.framework._

import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.api._

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import _root_.scala.collection.JavaConversions._

import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.squeryl.dsl.ast.LogicalBoolean

object EventQueryService {

  def buildQuery(row: EventStore, select: EventSelect): List[Option[LogicalBoolean]] = {

    // Each can have 1 or more lists of values to match against.
    // TODO: Squeryl problem? if (select.getSeverityCount > 0) expressions ::= (row.severity in select.getSeverityList.toList)
    // TODO: check if _.getUID is empty string.
    //if (select.getEntityCount > 0) expressions ::= (row.entityId in EQ.findEntities(select.getEntityList.toList).map(_.id))

    List(
      select.getEventTypeList.asParam { row.eventType in _ },
      select.getSubsystemList.asParam { row.subsystem in _ },
      select.getUserIdList.asParam { row.userId in _ },
      select.getEntityList.asParam { row.entityId in _.map(EQ.idsFromProtoQuery(_)).flatten.distinct },
      select.timeFrom.asParam(row.time gte _),
      select.timeTo.asParam(row.time lte _),
      select.uidAfter.asParam(row.id gt _.toLong))
  }

}

class EventQueryService(protected val modelTrans: ServiceTransactable[EventServiceModel])
    extends ServiceEndpoint[EventList] {
  import EventQueryService._

  override val descriptor = Descriptors.eventList

  override def put(req: EventList, env: RequestEnv): Response[EventList] = noVerb("put")
  override def delete(req: EventList, env: RequestEnv): Response[EventList] = noVerb("delete")
  override def post(req: EventList, env: RequestEnv): Response[EventList] = noVerb("post")

  override def get(req: EventList, env: RequestEnv): Response[EventList] = {

    env.subQueue.foreach(queueName => throw new BadRequestException("Subscribe not allowed: " + queueName))

    if (!req.hasSelect)
      throw new BadRequestException("Must include select")

    modelTrans.transaction { (model: EventServiceModel) =>

      val select = req.getSelect
      val limit = select.limit.getOrElse(1000) // default all queries to max of 1000 events.

      val entries =
        from(model.table)(row =>
          where(SquerylModel.combineExpressions(buildQuery(row, select).flatten))
            select (row)
            orderBy timeOrder(row.time, select.ascending)).page(0, limit)

      val respList = EventList.newBuilder.addAllEvents(entries.toList.map(model.convertToProto(_))).build
      new Response(Envelope.Status.OK, respList)
    }
  }

  def timeOrder(time: ExpressionNode, ascending: Option[Boolean]) = {
    if (ascending getOrElse false)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }

}

