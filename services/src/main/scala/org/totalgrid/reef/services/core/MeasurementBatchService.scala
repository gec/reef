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

import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.proto.Measurements.MeasurementBatch

import scala.collection.JavaConversions._

import org.totalgrid.reef.sapi._
import org.totalgrid.reef.sapi.client._
import org.totalgrid.reef.models.{ CommunicationEndpoint, Point }
import org.totalgrid.reef.japi.{ Envelope, BadRequestException }
import org.totalgrid.reef.services.framework.{ AuthorizesCreate, RequestContextSource, ServiceEntryPoint }

class MeasurementBatchService(pool: SessionPool)
    extends ServiceEntryPoint[MeasurementBatch] with AuthorizesCreate {

  override val descriptor = Descriptors.measurementBatch

  override def putAsync(contextSource: RequestContextSource, req: MeasurementBatch)(callback: Response[MeasurementBatch] => Unit) = {

    val requests = contextSource.transaction { context =>
      authorizeCreate(context, req)

      // TODO: load all endpoints efficiently
      val names = req.getMeasList().toList.map(_.getName)
      val points = Point.findByNames(names).toList

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

    val promises = pool.borrow { client =>
      requests.map { req =>
        client.request(req.verb, req.payload, req.env, req.destination)
      }
    }

    ScatterGather.collect(promises) { results =>
      val failures = results.filterNot(_.success)
      val response = if (failures.size == 0) Success(Envelope.Status.OK, List(MeasurementBatch.newBuilder(req).clearMeas.build))
      else {
        val msg = failures.mkString(",")
        Failure(Envelope.Status.INTERNAL_ERROR, msg)
      }
      callback(response)
    }

  }

  private def convertEndpointToDestination(ce: CommunicationEndpoint): Destination = ce.frontEndAssignment.value.serviceRoutingKey match {
    case Some(key) => AddressableDestination(key)
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
        Request(Envelope.Verb.PUT, batch.build, destination = convertEndpointToDestination(ce)) :: sum
    }

  }
}