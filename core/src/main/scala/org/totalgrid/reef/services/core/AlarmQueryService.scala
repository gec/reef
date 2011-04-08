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

import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.models.{ ApplicationSchema, EventStore, AlarmModel }
import org.totalgrid.reef.api.ServiceTypes.Response

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.api.{ Envelope, BadRequestException, RequestEnv }
import org.totalgrid.reef.api.service.AsyncToSyncServiceAdapter
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.squeryl.dsl.ast.LogicalBoolean

object AlarmQueryService {

  def buildQuery(row: AlarmModel, eventRow: EventStore, select: AlarmSelect): List[Option[LogicalBoolean]] = {
    List(
      select.getStateList.asParam(row.state in _.map(_.getNumber))) ::: select.eventSelect.map(EventQueryService.buildQuery(eventRow, _)).getOrElse(Nil)
  }

  def makeSubscriptionKeyParts(select: AlarmSelect): List[List[String]] = {

    val eventParts = EventQueryService.makeSubscriptionKeyParts(select.getEventSelect)
    val alarmParts = List(Nil: List[String]) ::: eventParts

    alarmParts
  }
}

class AlarmQueryService(subHandler: ServiceSubscriptionHandler) extends AsyncToSyncServiceAdapter[AlarmList] {

  def this(pubs: ServiceEventPublishers) = this(pubs.getEventSink(classOf[Alarm]))

  override val descriptor = Descriptors.alarmList

  override def put(req: AlarmList, env: RequestEnv): Response[AlarmList] = noPut
  override def delete(req: AlarmList, env: RequestEnv): Response[AlarmList] = noDelete
  override def post(req: AlarmList, env: RequestEnv): Response[AlarmList] = noPost

  override def get(req: AlarmList, env: RequestEnv): Response[AlarmList] = {
    import ApplicationSchema._
    import AlarmQueryService._

    if (!req.hasSelect) throw new BadRequestException("Must include select")

    val select = req.getSelect

    transaction {
      env.subQueue.foreach { queueName =>
        val keys = createSubscriptionPermutations(makeSubscriptionKeyParts(select))
        keys.foreach(keyParts => subHandler.bind(queueName, ProtoRoutingKeys.generateRoutingKey(keyParts)))
      }

      // default all queries to max of 1000 events.
      val limit = select.eventSelect.limit getOrElse 1000

      val results = from(alarms, events)((alarm, event) =>
        where(SquerylModel.combineExpressions(buildQuery(alarm, event, select).flatten) and
          alarm.eventUid === event.id)
          select ((alarm, event))
          orderBy timeOrder(event.time, select.eventSelect.ascending)).page(0, limit).toList // page(page_offset, page_length)

      val alarmProtos = results.map(x => AlarmConversion.convertToProto(x._1, x._2)) // AlalarmModel, EventStore
      val alarmList = AlarmList.newBuilder.addAllAlarms(alarmProtos).build

      Response(Envelope.Status.OK, alarmList :: Nil)
    }
  }

  def timeOrder(time: ExpressionNode, ascending: Option[Boolean]) = {
    if (ascending getOrElse false)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }

}

