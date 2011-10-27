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

import org.totalgrid.reef.client.sapi.Descriptors

import org.totalgrid.reef.proto.Measurements.MeasurementBatch

import scala.collection.JavaConversions._

import org.totalgrid.reef.models.{ CommunicationEndpoint, Point }
import org.totalgrid.reef.api.japi.{ Envelope, BadRequestException }
import org.totalgrid.reef.services.framework.{ AuthorizesCreate, RequestContextSource, ServiceEntryPoint }

import net.agileautomata.executor4s.Futures
import org.totalgrid.reef.api.japi.client.AddressableDestination
import org.totalgrid.reef.api.sapi.client._

class MeasurementBatchService
    extends ServiceEntryPoint[MeasurementBatch] with AuthorizesCreate {

  override val descriptor = Descriptors.measurementBatch

  override def putAsync(contextSource: RequestContextSource, req: MeasurementBatch)(callback: Response[MeasurementBatch] => Unit) = {

    val future = contextSource.transaction { context =>
      authorizeCreate(context, req)

      // TODO: load all endpoints efficiently
      val names = req.getMeasList().toList.map(_.getName)
      val points = Point.findByNames(names).toList

      if (!points.forall(_.endpoint.value.isDefined))
        throw new BadRequestException("Not all points have endpoints set.")

      val commEndpoints = points.groupBy(_.endpoint.value.get)

      val requests = commEndpoints.size match {
        //fails with exception if any batch can't be routed
        case 0 => throw new BadRequestException("No Logical Nodes on points: ")
        case 1 =>
          val headers = BasicRequestHeaders.empty.setDestination(convertEndpointToDestination(commEndpoints.head._1))
          Request(Envelope.Verb.PUT, req, headers) :: Nil
        case _ => getRequests(req, commEndpoints)
      }
      val futures = requests.map(req => context.client.request(req.verb, req.payload, Some(req.env)))
      Futures.gather(context.client, futures)
    }

    future.listen { results =>
      val failures = results.filterNot(_.success)
      val response: Response[MeasurementBatch] =
        if (failures.size == 0) SuccessResponse(Envelope.Status.OK, List(MeasurementBatch.newBuilder(req).clearMeas.build))
        else FailureResponse(Envelope.Status.INTERNAL_ERROR, failures.mkString(","))
      callback(response)
    }

  }

  private def convertEndpointToDestination(ce: CommunicationEndpoint) = ce.frontEndAssignment.value.serviceRoutingKey match {
    case Some(key) => new AddressableDestination(key)
    case None => throw new BadRequestException("No measurement stream assignment for endpoint: " + ce.entityName)
  }

  private def getRequests(req: MeasurementBatch, commEndpoints: Map[CommunicationEndpoint, List[Point]]): List[Request[MeasurementBatch]] = {
    val measList = req.getMeasList().toList

    // TODO: more efficient creation of measurement batches
    commEndpoints.foldLeft(Nil: List[Request[MeasurementBatch]]) {
      case (sum, (ce, points)) =>

        val batch = MeasurementBatch.newBuilder
        batch.setWallTime(req.getWallTime)
        points.foreach { p =>
          batch.addMeas(measList.find(_.getName == p.entityName).get)
        }
        val headers = BasicRequestHeaders.empty.setDestination(convertEndpointToDestination(ce))
        Request(Envelope.Verb.PUT, batch.build, headers) :: sum
    }

  }
}