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

import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, Relationship }
import org.totalgrid.reef.models.{ ApplicationSchema }
import org.totalgrid.reef.messaging.{ ProtoServiceable, ProtoServiceException, RequestEnv }
import org.totalgrid.reef.messaging.ProtoServiceTypes._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoServiceEndpoint
import org.totalgrid.reef.services.{ ServiceEventPublishers, ServiceSubscriptionHandler }

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.messaging.ProtoServiceException

import OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import scala.collection.JavaConversions._

class EntityService extends ProtoServiceable[EntityProto] with ProtoServiceEndpoint {

  def deserialize(bytes: Array[Byte]) = EntityProto.parseFrom(bytes)
  val servedProto: Class[_] = classOf[EntityProto]

  override def put(req: EntityProto, env: RequestEnv): Response[EntityProto] = {

    transaction {
      var ent = EQ.findOrCreateEntity(req.getName, req.getTypesList.head) // TODO: need better message if getTypesList is empty: "BAD_REQUEST with message: java.util.NoSuchElementException"
      req.getTypesList.tail.foreach(t => if (ent.types.value.find(_ == t).isEmpty) ent = EQ.addTypeToEntity(ent, t))
      new Response(Envelope.Status.OK, EQ.entityToProto(ent).build)
    }
  }
  override def delete(req: EntityProto, env: RequestEnv): Response[EntityProto] = noVerb("delete")
  override def post(req: EntityProto, env: RequestEnv): Response[EntityProto] = noVerb("post")

  override def get(req: EntityProto, env: RequestEnv): Response[EntityProto] = {
    transaction {
      info("Query: " + req)
      val result = EQ.fullQuery(req);
      info("Result: " + result)
      new Response(Envelope.Status.OK, result)
    }
  }
}

import org.totalgrid.reef.proto.Model.{ EntityEdge => EntityEdgeProto }
import org.totalgrid.reef.models.{ EntityEdge }

class EntityEdgeService extends ProtoServiceable[EntityEdgeProto] with ProtoServiceEndpoint {

  def deserialize(bytes: Array[Byte]) = EntityEdgeProto.parseFrom(bytes)
  val servedProto: Class[_] = classOf[EntityEdgeProto]

  def convertToProto(entry: EntityEdge): EntityEdgeProto = {
    val b = EntityEdgeProto.newBuilder()
    b.setParent(EntityProto.newBuilder.setUid(entry.parentId.toString))
    b.setChild(EntityProto.newBuilder.setUid(entry.childId.toString))
    b.setRelationship(entry.relationship)
    b.build
  }

  override def put(req: EntityEdgeProto, env: RequestEnv): Response[EntityEdgeProto] = {

    transaction {
      val parentEntity = EQ.findEntity(req.getParent).getOrElse(throw new Exception("cannot find parent"))
      val childEntity = EQ.findEntity(req.getChild).getOrElse(throw new Exception("cannot find child"))
      val existingEdge = EQ.findEdge(parentEntity, childEntity, req.getRelationship)

      val (edge, status) = existingEdge match {
        case Some(edge) => (edge, Envelope.Status.NOT_MODIFIED)
        case None => (EQ.addEdge(parentEntity, childEntity, req.getRelationship), Envelope.Status.CREATED)
      }
      val proto = convertToProto(edge)
      new Response(status, proto)
    }
  }
  override def delete(req: EntityEdgeProto, env: RequestEnv): Response[EntityEdgeProto] = noVerb("delete")
  override def post(req: EntityEdgeProto, env: RequestEnv): Response[EntityEdgeProto] = noVerb("post")

  override def get(req: EntityEdgeProto, env: RequestEnv): Response[EntityEdgeProto] = {
    transaction {
      // TODO: add edge searching
      val edges = EQ.edges.where(t => true === true).toList
      new Response(Envelope.Status.OK, edges.map { convertToProto(_) })
    }
  }
}
