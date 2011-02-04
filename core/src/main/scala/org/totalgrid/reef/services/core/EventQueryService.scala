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
import org.totalgrid.reef.models.{ ApplicationSchema, EventStore }
import org.totalgrid.reef.messaging.ProtoServiceable
import org.totalgrid.reef.protoapi.{ ProtoServiceException, RequestEnv, ProtoServiceTypes }
import ProtoServiceTypes.Response

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoServiceEndpoint

import org.totalgrid.reef.proto.Envelope

import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }

import org.squeryl.PrimitiveTypeMode._
import OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

import org.totalgrid.reef.services.ServiceProviderHeaders._

import org.squeryl.dsl.ast.LogicalBoolean

object EventQueryService {

  def buildQuery(row: EventStore, select: EventSelect): LogicalBoolean = {

    var expressions: List[LogicalBoolean] = Nil

    select.getEventTypeCount match {
      case 0 =>
      case 1 =>
        select.getEventType(0) match {
          case "*" => expressions ::= (true === true) // special case for all events
          case id => expressions ::= (row.eventType === id)
        }
      case x => expressions ::= (row.eventType in select.getEventTypeList.toList)
    }

    // Each can have 1 or more lists of values to match against.
    //
    // TODO: Squeryl problem? if (select.getSeverityCount > 0) expressions ::= (row.severity in select.getSeverityList.toList)
    if (select.getSubsystemCount > 0) expressions ::= (row.subsystem in select.getSubsystemList.toList)
    if (select.getUserIdCount > 0) expressions ::= (row.userId in select.getUserIdList.toList)
    //TODO: check if _.getUID is empty string.
    //if (select.getEntityCount > 0) expressions ::= (row.entityId in EQ.findEntities(select.getEntityList.toList).map(_.id))
    if (select.getEntityCount > 0) expressions ::= (row.entityId in select.getEntityList.toList.map(EQ.idsFromProtoQuery(_)).flatten.distinct)

    if (select.hasTimeFrom) {
      // Combine timeFrom and timeTo in one expression. Hopefully, this explicit expression
      //  will produce an optimized query on the time index.
      if (select.hasTimeTo)
        expressions ::= (row.time gte select.getTimeFrom) and (row.time lte select.getTimeTo)
      else
        expressions ::= (row.time gte select.getTimeFrom)
    }

    // UidAfter is used to get updates without getting duplicates from previous queries.
    if (select.hasUidAfter)
      expressions ::= (row.id gt select.getUidAfter.toLong)

    //Console.printf( "expressions.size = %d\n", expressions.size)

    // Gather the expressions into a tree of AND clauses.
    // If we don't have any expressions, get all events.
    //    if (expressions.size == 0)
    //      (1 === 1)
    //    else
    //      expressions.reduceRight((a, b) => new BinaryOperatorNodeLogicalBoolean(a, b, "and"))

    expressions
  }

}

class EventQueryService(protected val modelTrans: ServiceTransactable[EventServiceModel])
    extends ProtoServiceable[EventList] with ProtoServiceEndpoint {
  import EventQueryService._

  def deserialize(bytes: Array[Byte]) = EventList.parseFrom(bytes)
  val servedProto: Class[_] = classOf[EventList]

  override def put(req: EventList, env: RequestEnv): Response[EventList] = noVerb("put")
  override def delete(req: EventList, env: RequestEnv): Response[EventList] = noVerb("delete")
  override def post(req: EventList, env: RequestEnv): Response[EventList] = noVerb("post")

  override def get(req: EventList, env: RequestEnv): Response[EventList] = {

    env.subQueue.foreach(queueName => throw new ProtoServiceException("Subscribe not allowed: " + queueName))

    if (!req.hasSelect)
      throw new ProtoServiceException("Must include select")

    modelTrans.transaction { (model: EventServiceModel) =>

      val select = req.getSelect
      var limit = 1000; // default all queries to max of 1000 events.

      if (select.hasLimit)
        limit = select.getLimit

      val entries =
        from(model.table)(row =>
          where(buildQuery(row, select))
            select (row)
            orderBy timeOrder(row.time, select)).page(0, limit) //.toList // page(page_offset, page_length)

      val respList = EventList.newBuilder.addAllEvents(entries.toList.map(model.convertToProto(_))).build
      new Response(Envelope.Status.OK, respList)
    }
  }

  def timeOrder(time: ExpressionNode, select: EventSelect) = {
    if (select.ascending getOrElse false)
      new OrderByArg(time).asc
    else
      new OrderByArg(time).desc
  }

}

