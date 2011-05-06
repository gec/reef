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

import org.totalgrid.reef.messaging.{ AMQPProtoFactory, ProtoClient }

import org.totalgrid.reef.proto.{ Descriptors, ReefServicesList }

import org.totalgrid.reef.proto.Measurements.MeasurementBatch

import scala.collection.JavaConversions._

import org.totalgrid.reef.models.{ ApplicationSchema, CommunicationEndpoint, Point }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.api.{ Envelope, RequestEnv, BadRequestException, IDestination, AddressableService, ReefServiceException }
import org.totalgrid.reef.api.ServiceTypes.{ Response, Request, Failure }
import org.totalgrid.reef.api.service.AsyncServiceBase

class MeasurementBatchService(amqp: AMQPProtoFactory)
    extends AsyncServiceBase[MeasurementBatch] {

  override val descriptor = Descriptors.measurementBatch

  override def putAsync(req: MeasurementBatch, env: RequestEnv)(callback: Response[MeasurementBatch] => Unit) = {

    val requests: List[Request[MeasurementBatch]] = transaction {
      // TODO: load all endpoints efficiently
      val names = req.getMeasList().toList.map(_.getName)
      val points = ApplicationSchema.points.where(p => p.name in names).toList

      if (!points.forall(_.endpoint.value.isDefined))
        throw new BadRequestException("Not all points have endpoints set.")

      val commEndpoints = points.groupBy(_.endpoint.value.get)

      commEndpoints.size match {
        //fails with exception if any batch can't be routed
        case 0 => throw new BadRequestException("No Logical Nodes on points: ")
        case 1 => Request(Envelope.Verb.PUT, req, destination = convertEndpointToDestination(commEndpoints.head._1)) :: Nil
        case _ => getRequests(req, commEndpoints)
      }

    }

    borrow { client =>
      client.requestAsyncScatterGather(requests) { results =>
        val failures = results.flatMap {
          _ match {
            case x: Failure => Some(x)
            case _ => None
          }
        }

        if (failures.size == 0) callback(Response(Envelope.Status.OK, List(MeasurementBatch.newBuilder(req).clearMeas.build())))
        else {
          val msg = failures.mkString(",")
          callback(Response(Envelope.Status.INTERNAL_ERROR, error = msg))
        }
      }
    }

  }

  private def borrow[A](fun: ProtoClient => A): A = {

    var client: Option[ProtoClient] = None

    try {
      val client = Some(amqp.getProtoClientSession(ReefServicesList, 5000))
      fun(client.get)
    } finally {
      try {
        client.foreach {
          _.close()
        }
      } catch {
        case ex: ReefServiceException => error(ex)
      }
    }
  }

  private def convertEndpointToDestination(ce: CommunicationEndpoint): IDestination = ce.frontEndAssignment.value.serviceRoutingKey match {
    case Some(key) => AddressableService(key)
    case None => throw new BadRequestException("No measurement stream assignment for endpoint: " + ce.name.value)
  }

  private def getRequests(req: MeasurementBatch, commEndpoints: Map[CommunicationEndpoint, List[Point]]): List[Request[MeasurementBatch]] = {
    val measList = req.getMeasList().toList

    // TODO: more efficient creation of measurement batches
    commEndpoints.foldLeft(Nil: List[Request[MeasurementBatch]]) {
      case (sum, (ce, points)) =>

        val batch = MeasurementBatch.newBuilder
        batch.setWallTime(req.getWallTime)
        points.foreach { p =>
          batch.addMeas(measList.find(_.getName == p.name).get)
        }
        Request(Envelope.Verb.PUT, batch.build, destination = convertEndpointToDestination(ce)) :: sum
    }

  }
}