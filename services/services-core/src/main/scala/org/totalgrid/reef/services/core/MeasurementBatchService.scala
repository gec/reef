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

import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.service.proto.Measurements.MeasurementBatch

import scala.collection.JavaConversions._

import org.totalgrid.reef.models.{ CommunicationEndpoint, Point }
import org.totalgrid.reef.services.framework.{ RequestContextSource, ServiceEntryPoint }

import org.totalgrid.reef.client.{ RequestHeaders, AddressableDestination }

import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client.exception.{ ReefServiceException, BadRequestException }
import org.totalgrid.reef.client.proto.Envelope

//import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.client.operations.scl.{ ScalaResponse, PromiseCollators }
import org.totalgrid.reef.client.operations.Response

class MeasurementBatchService
    extends ServiceEntryPoint[MeasurementBatch] {

  private case class Request[+A](verb: Envelope.Verb, payload: A, env: RequestHeaders = BasicRequestHeaders.empty)

  override val descriptor = Descriptors.measurementBatch

  override def putAsync(contextSource: RequestContextSource, req: MeasurementBatch)(callback: Response[MeasurementBatch] => Unit) {

    val future = contextSource.transaction { context =>

      // TODO: load all endpoints efficiently
      val names = req.getMeasList().toList.map(_.getName).distinct
      val points = Point.findByNames(names).toList

      if (names.size != points.size) {
        val missingPoints = names.diff(points.map { _.entityName })
        throw new BadRequestException("Trying to publish on unknown points: " + missingPoints.mkString(","))
      }

      val pointsWithoutEndpoints = points.filter(_.endpoint.value.isEmpty)

      if (!pointsWithoutEndpoints.isEmpty) {
        throw new BadRequestException("No endpoint set for points: " + pointsWithoutEndpoints.map { _.entityName })
      }

      val commEndpoints = points.groupBy(_.endpoint.value.get)

      context.auth.authorize(context, componentId, "create", points.map { _.entityId })

      val headers = BasicRequestHeaders.empty
      val commonHeaders = context.getHeaders.timeout.map { headers.setTimeout(_) }.getOrElse(headers)

      val requests = commEndpoints.size match {
        //fails with exception if any batch can't be routed
        case 0 => throw new BadRequestException("No Logical Nodes on points: ")
        case 1 =>
          val addressedHeaders = commonHeaders.setDestination(convertEndpointToDestination(commEndpoints.head._1))
          Request(Envelope.Verb.PUT, req, addressedHeaders) :: Nil
        case _ => getRequests(req, commonHeaders, commEndpoints)
      }

      val exe = context.client.getInternal.getExecutor
      val ops = context.client.getServiceOperations

      val promises = requests.map { req =>
        ops.operation("Couldn't scatter measurement updates in MeasurementBatchService") { rest =>
          rest.request(req.verb, req.payload, req.env)
        }
      }

      PromiseCollators.collate(exe, promises)
    }

    future.listenFor { results =>
      val response = try {
        results.await()
        ScalaResponse.success(Envelope.Status.OK, MeasurementBatch.newBuilder(req).clearMeas.build)
      } catch {
        case rse: ReefServiceException => ScalaResponse.failure[MeasurementBatch](rse.getStatus, rse.getMessage)
      }
      callback(response)
    }
  }

  private def convertEndpointToDestination(ce: CommunicationEndpoint) = ce.frontEndAssignment.value.serviceRoutingKey match {
    case Some(key) => new AddressableDestination(key)
    case None => throw new BadRequestException("No measurement stream assignment for endpoint: " + ce.entityName)
  }

  private def getRequests(req: MeasurementBatch, commonHeaders: RequestHeaders, commEndpoints: Map[CommunicationEndpoint, List[Point]]): List[Request[MeasurementBatch]] = {
    val measList = req.getMeasList().toList

    // TODO: more efficient creation of measurement batches
    commEndpoints.foldLeft(Nil: List[Request[MeasurementBatch]]) {
      case (sum, (ce, points)) =>

        val batch = MeasurementBatch.newBuilder
        batch.setWallTime(req.getWallTime)
        points.foreach { p =>
          batch.addMeas(measList.find(_.getName == p.entityName).get)
        }
        val headers = commonHeaders.setDestination(convertEndpointToDestination(ce))
        Request(Envelope.Verb.PUT, batch.build, headers) :: sum
    }

  }
}