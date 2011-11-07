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

import scala.collection.JavaConversions._

import org.totalgrid.reef.clientapi.sapi.types.BuiltInDescriptors
import org.totalgrid.reef.clientapi.sapi.client.impl.SynchronizedPromise
import org.totalgrid.reef.clientapi.sapi.client._
import org.totalgrid.reef.clientapi.sapi.service.{ ServiceResponseCallback, ServiceHelpers }
import org.totalgrid.reef.clientapi.exceptions.BadRequestException

import org.totalgrid.reef.clientapi.proto.Envelope.{ ServiceResponse, SelfIdentityingServiceRequest, ServiceRequest, BatchServiceRequest }
import org.totalgrid.reef.clientapi.proto.{ StatusCodes, Envelope }

import org.totalgrid.reef.services.framework._

class BatchServiceRequestService(services: List[ServiceEntryPoint[_ <: AnyRef]])
    extends ServiceEntryPoint[BatchServiceRequest] with AuthorizesCreate {

  private val serviceMap: Map[String, ServiceEntryPoint[_ <: AnyRef]] = services.map { x => x.descriptor.id() -> x }.toMap

  override val descriptor = BuiltInDescriptors.batchServiceRequest

  override def postAsync(contextSource: RequestContextSource, req: BatchServiceRequest)(callback: Response[BatchServiceRequest] => Unit) {
    val responses = contextSource.transaction { context =>
      authorizeCreate(context, req)
      val source = new RequestContextSource { def transaction[A](f: (RequestContext) => A) = f(context) }

      val requests = req.getRequestsList.toList
      requests.map { request =>
        serviceMap.get(request.getExchange) match {
          case Some(service) =>
            val klass = service.descriptor.getKlass
            val rsp = handleRequest(source, service, request.getRequest, klass)
            if (!StatusCodes.isSuccess(rsp.getStatus)) throw StatusCodes.toException(rsp.getStatus, rsp.getErrorMessage)
            rsp
          case None => throw new BadRequestException("No known service for exchange: " + request.getExchange)
        }
      }
    }

    callback(buildResponse(responses))
  }

  private def buildResponse(responses: List[Envelope.ServiceResponse]) = {
    val b = BatchServiceRequest.newBuilder

    responses.foreach { r =>
      b.addRequests(SelfIdentityingServiceRequest.newBuilder.setResponse(r))
    }

    Response(Envelope.Status.OK, b.build)
  }

  private def handleRequest[A <: AnyRef](
    contextSource: RequestContextSource,
    rawService: ServiceEntryPoint[_ <: AnyRef],
    request: ServiceRequest,
    klass: Class[A]): ServiceResponse = {

    val service = rawService.asInstanceOf[ServiceEntryPoint[A]]

    val value = service.descriptor.deserialize(request.getPayload.toByteArray)

    val promise = new SynchronizedPromise[ServiceResponse]

    def onResponse(rsp: Response[A]) = {
      promise.set(ServiceHelpers.getResponse(request.getId, rsp, service.descriptor))
    }
    val callback = new ServiceResponseCallback {
      def onResponse(rsp: ServiceResponse) = promise.set(rsp)
    }

    ServiceHelpers.catchErrors(request, callback) {
      service.respondAsync(request.getVerb, contextSource, value)(onResponse)
    }

    promise.await
  }

}