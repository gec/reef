/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.clientapi.sapi.client.rest.impl

import java.util.UUID
import scala.collection.mutable.Queue
import scala.collection.JavaConversions._

import com.google.protobuf.ByteString
import net.agileautomata.executor4s._

import org.totalgrid.reef.clientapi.types.TypeDescriptor
import org.totalgrid.reef.clientapi.sapi.client.rest._
import org.totalgrid.reef.clientapi.sapi.client._
import org.totalgrid.reef.clientapi.proto.Envelope.{ ServiceResponse, BatchServiceRequest, SelfIdentityingServiceRequest, Verb }
import org.totalgrid.reef.clientapi.proto.{ StatusCodes, Envelope }

class BatchServiceRestOperations[A <: RestOperations with ServiceRegistry with Executor](client: A) extends RestOperations {

  case class RequestWithFuture[A](request: SelfIdentityingServiceRequest, future: Future[Response[A]] with Settable[Response[A]], descriptor: TypeDescriptor[A])
  private val pendingRequests = Queue.empty[RequestWithFuture[_]]

  def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {

    val info = client.getServiceInfo(ClassLookup.get(payload))
    val uuid = UUID.randomUUID().toString

    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid)
    builder.setPayload(ByteString.copyFrom(info.descriptor.serialize(payload)))
    headers.foreach { _.toEnvelopeRequestHeaders.foreach(builder.addHeaders) }

    val cachedRequest = SelfIdentityingServiceRequest.newBuilder.setExchange(info.descriptor.id).setRequest(builder).build

    val future = client.future[Response[A]]

    pendingRequests.enqueue(RequestWithFuture(cachedRequest, future, info.descriptor))

    future
  }

  def subscribe[A](descriptor: TypeDescriptor[A]) = client.subscribe(descriptor)

  def flush() = {

    val requests = pendingRequests.toList
    pendingRequests.clear()

    val b = BatchServiceRequest.newBuilder
    requests.foreach { o => b.addRequests(o.request) }
    val request = b.build

    val batchFuture = client.request(Envelope.Verb.POST, request, None)

    batchFuture.listen {
      _ match {
        case SuccessResponse(status, batchResults) =>
          val resultAndFuture = batchResults.head.getRequestsList.toList.map { _.getResponse }.zip { requests }
          resultAndFuture.foreach {
            case (response, reqWithFuture) => applyResponse(response, reqWithFuture)
          }
          SuccessResponse(status, batchResults)
        case fail: FailureResponse =>
          requests.foreach { _.future.set(fail) }
          fail
      }
    }

    batchFuture
  }

  private def applyResponse[A](response: ServiceResponse, reqWithFuture: RequestWithFuture[A]) {
    val successOrFailure = if (StatusCodes.isSuccess(response.getStatus)) {
      val data: List[A] = response.getPayloadList.toList.map { bs => reqWithFuture.descriptor.deserialize(bs.toByteArray) }
      SuccessResponse(response.getStatus, data)
    } else {
      FailureResponse(response.getStatus, response.getErrorMessage)
    }
    reqWithFuture.future.set(successOrFailure)
  }
}