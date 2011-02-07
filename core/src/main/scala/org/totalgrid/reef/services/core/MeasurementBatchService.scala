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

import org.totalgrid.reef.proto.Envelope

import org.totalgrid.reef.protoapi.{ RequestEnv, ProtoServiceException, ProtoServiceTypes }
import org.totalgrid.reef.messaging.{ AMQPProtoFactory, ProtoServiceable }
import ProtoServiceTypes.Response
import org.totalgrid.reef.services.ProtoServiceEndpoint

import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementBatch }

import scala.collection.JavaConversions._

import org.totalgrid.reef.models.{ ApplicationSchema, CommunicationEndpoint, Point }
import org.squeryl.PrimitiveTypeMode._

class MeasurementBatchService(amqp: AMQPProtoFactory) extends ProtoServiceable[MeasurementBatch] with ProtoServiceEndpoint {

  val servedProto = classOf[MeasurementBatch]

  override def deserialize(bytes: Array[Byte]) = MeasurementBatch.parseFrom(bytes)

  override def delete(req: MeasurementBatch, env: RequestEnv) = noVerb("delete")
  override def get(req: MeasurementBatch, env: RequestEnv) = noVerb("get")

  override def post(req: MeasurementBatch, env: RequestEnv) = put(req, env)
  override def put(req: MeasurementBatch, env: RequestEnv) = {
    transaction {
      val names = req.getMeasList().toList.map(_.getName)
      val points = ApplicationSchema.points.where(p => p.name in names).toList
      // TODO: load all endpoints efficiently

      if (!points.forall(_.endpoint.value.isDefined)) {
        throw new ProtoServiceException("Not all points have endpoints set.")
      }

      val commEndpoints = points.groupBy(_.endpoint.value.get)

      val destForBatchs = commEndpoints.size match {
        case 0 => throw new ProtoServiceException("No Logical Nodes on points: " + names)
        case 1 => (commEndpoints.head._1, req) :: Nil
        case _ => rebuildBatches(req, commEndpoints)
      }
      destForBatchs.foreach {
        case (ce, batch) =>
          ce.frontEndAssignment.value.serviceRoutingKey match {
            case Some(routingKey) =>
              val client = amqp.getProtoServiceClient("measurement_batch", routingKey, 1000, MeasurementBatch.parseFrom)
              client.putThrow(batch)
            // TODO: client.close
            case None =>
              throw new ProtoServiceException("Measurement Stream not ready.")
          }
      }
      val sentBatches = destForBatchs.map { case (ce, batch) => batch }
      new Response(Envelope.Status.OK, sentBatches)
    }
  }

  private def rebuildBatches(req: MeasurementBatch, commEndpoints: Map[CommunicationEndpoint, List[Point]]): List[(CommunicationEndpoint, MeasurementBatch)] = {
    var measList = req.getMeasList().toList

    var retList: List[(CommunicationEndpoint, MeasurementBatch)] = Nil

    // TODO: more efficient creation of measurement batches
    commEndpoints.foreach {
      case (ce, points) =>
        val batch = MeasurementBatch.newBuilder
        batch.setWallTime(req.getWallTime)
        points.foreach { p =>
          batch.addMeas(measList.find(_.getName == p.name).get)
        }
        retList = (ce, batch.build) :: retList
    }
    retList
  }
}