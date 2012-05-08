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
package org.totalgrid.reef.client.operations.impl

import java.util.UUID
import com.google.protobuf.ByteString
import org.totalgrid.reef.client.types.{ TypeDescriptor, ServiceTypeInformation }
import collection.mutable.Queue
import org.totalgrid.reef.client.operations.scl.ScalaPromise._
import org.totalgrid.reef.client.operations.scl.ScalaResponse._
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }
import org.totalgrid.reef.client.javaimpl.ResponseWrapper
import org.totalgrid.reef.client.proto.Envelope.{ ServiceResponse, BatchServiceRequest, SelfIdentityingServiceRequest, Verb }
import org.totalgrid.reef.client.exception.{ InternalClientError, ReefServiceException }
import org.totalgrid.reef.client.{ RequestHeaders, Promise }
import org.totalgrid.reef.client.operations.{ Response, RestOperations }
import org.totalgrid.reef.client.sapi.client.rest.impl.{ DefaultClient, ClassLookup }

trait BatchRestOperations extends RestOperations with OptionallyBatchedRestOperations {
  def batched: Option[BatchRestOperations] = Some(this)
  def flush(): Promise[BatchServiceRequest]
  def batchedFlush(batchSize: Int): Promise[java.lang.Boolean]
}

class DefaultBatchRestOperations(protected val ops: RestOperations, client: DefaultClient) extends BatchRestOperationsImpl {
  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _] = client.getServiceInfo(klass)
  protected def futureSource[A] = FuturePromise.open[A](client)
  protected def notifyListeners[A](verb: Envelope.Verb, payload: A, promise: Promise[Response[A]]) {
    client.notifyListeners(verb, payload, promise)
  }

}

trait BatchRestOperationsImpl extends BatchRestOperations with DerivedRestOperations {
  protected def getServiceInfo[A](klass: Class[A]): ServiceTypeInformation[A, _]
  protected def futureSource[A]: OpenPromise[A]
  protected def ops: RestOperations
  protected def notifyListeners[A](verb: Envelope.Verb, payload: A, promise: Promise[Response[A]])

  case class QueuedRequest[A](request: SelfIdentityingServiceRequest, descriptor: TypeDescriptor[A], promise: OpenPromise[Response[A]])
  private val requestQueue = Queue.empty[QueuedRequest[_]]

  protected def request[A](verb: Verb, payload: A, headers: Option[RequestHeaders]): Promise[Response[A]] = {

    val descriptor: TypeDescriptor[A] = getServiceInfo(ClassLookup.get(payload)).getDescriptor
    val uuid = UUID.randomUUID().toString

    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid)
    builder.setPayload(ByteString.copyFrom(descriptor.serialize(payload)))
    headers.foreach { h => builder.addAllHeaders(h.toEnvelopeRequestHeaders) }

    val request = SelfIdentityingServiceRequest.newBuilder.setExchange(descriptor.id).setRequest(builder).build

    val promise: OpenPromise[Response[A]] = futureSource[Response[A]]

    requestQueue.enqueue(QueuedRequest[A](request, descriptor, promise))

    notifyListeners(verb, payload, promise)
    promise
  }

  def flush(): Promise[BatchServiceRequest] = {
    sendBatch(popRequests(), None)
  }

  def batchedFlush(batchSize: Int): Promise[java.lang.Boolean] = {

    def nextBatch(prevFailed: Option[ReefServiceException], pending: List[QueuedRequest[_]], promise: OpenPromise[java.lang.Boolean]) {
      prevFailed match {
        case Some(rse) => promise.setFailure(rse)
        case None => pending match {
          case Nil => promise.setSuccess(true)
          case remains =>
            val (now, later) = if (batchSize > 0) {
              remains.splitAt(batchSize)
            } else {
              (remains, Nil)
            }
            sendBatch(now, Some(nextBatch(_, later, promise)))
        }
      }
    }

    val promise = futureSource[java.lang.Boolean]

    nextBatch(None, popRequests(), promise)

    promise
  }

  private def sendBatch(requests: List[QueuedRequest[_]], chain: Option[(Option[ReefServiceException]) => Unit]): Promise[BatchServiceRequest] = {

    def applyResponseToPromise[A](response: ServiceResponse, desc: TypeDescriptor[A], promise: OpenPromise[Response[A]]) {
      StatusCodes.isSuccess(response.getStatus) match {
        case true =>
          val data = response.getPayloadList.toList.map(bs => desc.deserialize(bs.toByteArray))
          promise.setSuccess(ResponseWrapper.success(response.getStatus, data))
        case false =>
          promise.setFailure(new ReefServiceException(response.getErrorMessage, response.getStatus))
      }
    }

    val batch = {
      val b = BatchServiceRequest.newBuilder
      requests.foreach(r => b.addRequests(r.request))
      b.build
    }

    val batchPromise: Promise[Response[BatchServiceRequest]] = ops.request(Envelope.Verb.POST, batch)

    batchPromise.listenEither {
      case Right(resp) => {
        resp.isSuccess match {
          case true => {
            val responses = resp.getList.get(0).getRequestsList.toList.map(_.getResponse)
            responses.zip(requests).foreach {
              case (servResp, QueuedRequest(_, desc, promise)) => applyResponseToPromise(servResp, desc, promise)
            }
            chain.foreach(_(None))
          }
          case false =>
            val rse = resp.getException
            requests.foreach(_.promise.setFailure(rse))
            chain.foreach(_(Some(rse)))
        }
      }
      case Left(ex) => {
        val rse = ex match {
          case rse: ReefServiceException => rse
          case other => new InternalClientError("Problem with batch request", ex)
        }
        requests.foreach(_.promise.setFailure(rse))
        chain.foreach(_(Some(rse)))
      }
    }

    batchPromise.map(_.one)
  }

  private def popRequests(): List[QueuedRequest[_]] = {
    val list = requestQueue.toList
    requestQueue.clear()
    list
  }
}

