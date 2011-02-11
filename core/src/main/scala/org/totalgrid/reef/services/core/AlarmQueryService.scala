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
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.models.{ ApplicationSchema, EventStore, AlarmModel }
import org.totalgrid.reef.protoapi.ServiceTypes.Response

import org.totalgrid.reef.messaging.{ ServiceEndpoint, Descriptors }
import org.totalgrid.reef.protoapi.{ ServiceException, RequestEnv }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.proto.Envelope

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }

import org.totalgrid.reef.messaging.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.squeryl.dsl.ast.LogicalBoolean

object AlarmQueryService {

  def buildQuery(row: AlarmModel, select: AlarmSelect): LogicalBoolean = {

    var expressions: List[LogicalBoolean] = Nil

    if (select.getStateCount > 0) expressions ::= (row.state in select.getStateList.toList.map(_.getNumber))

    expressions
  }

  def optionalEventQuery(event: EventStore, select: Option[EventSelect]): LogicalBoolean = {
    select.map(EventQueryService.buildQuery(event, _)) getOrElse (true === true)
  }

}

class AlarmQueryService
    extends ServiceEndpoint[AlarmList] {

  override val descriptor = Descriptors.alarmList

  override def put(req: AlarmList, env: RequestEnv): Response[AlarmList] = noVerb("put")
  override def delete(req: AlarmList, env: RequestEnv): Response[AlarmList] = noVerb("delete")
  override def post(req: AlarmList, env: RequestEnv): Response[AlarmList] = noVerb("post")

  override def get(req: AlarmList, env: RequestEnv): Response[AlarmList] = {
    import ApplicationSchema._
    import AlarmQueryService._

    env.subQueue.foreach(queueName => throw new ServiceException("Subscribe not allowed: " + queueName))

    if (!req.hasSelect)
      throw new ServiceException("Must include select")

    transaction {

      val select = req.getSelect
      val eSelect = select.getEventSelect

      // default all queries to max of 1000 events.
      val limit = eSelect.limit getOrElse 1000

      val results = from(alarms, events)((alarm, event) =>
        where(buildQuery(alarm, select) and
          optionalEventQuery(event, Some(eSelect)) and
          alarm.eventUid === event.id)
          select ((alarm, event))
          orderBy timeOrder(event.time, eSelect)).page(0, limit).toList // page(page_offset, page_length)

      val alarmProtos = results.map(x => AlarmConversion.convertToProto(x._1, x._2)) // AlalarmModel, EventStore
      val alarmList = AlarmList.newBuilder.addAllAlarms(alarmProtos).build

      new Response(Envelope.Status.OK, alarmList)
    }
  }

  def timeOrder(time: ExpressionNode, eSelect: EventSelect) = {
    if (eSelect.ascending getOrElse false)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }

}

