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
package org.totalgrid.reef.client.sapi.client.rest.impl

import java.util.UUID
import scala.collection.mutable.Queue
import scala.collection.JavaConversions._

import com.google.protobuf.ByteString
import net.agileautomata.executor4s._

import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.sapi.client.rest._
import org.totalgrid.reef.client.sapi.client._
import org.totalgrid.reef.client.proto.Envelope.{ ServiceResponse, BatchServiceRequest, SelfIdentityingServiceRequest, Verb }
import org.totalgrid.reef.client.proto.{ StatusCodes, Envelope }

class BatchServiceRestOperations(ops: RestOperations, hook: RequestSpyHook, registry: ServiceRegistry, exe: Executor) extends RestOperations {
  //class BatchServiceRestOperations[A <: RestOperations with RequestSpyHook with ServiceRegistry with Executor](client: A) extends RestOperations {

  case class RequestWithFuture[A](request: SelfIdentityingServiceRequest, future: Future[Response[A]] with Settable[Response[A]], descriptor: TypeDescriptor[A])
  private val pendingRequests = Queue.empty[RequestWithFuture[_]]

  def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {

    val info = registry.getServiceInfo(ClassLookup.get(payload))
    val uuid = UUID.randomUUID().toString

    val builder = Envelope.ServiceRequest.newBuilder.setVerb(verb).setId(uuid)
    builder.setPayload(ByteString.copyFrom(info.getDescriptor.serialize(payload)))
    headers.foreach { _.toEnvelopeRequestHeaders.foreach(builder.addHeaders) }

    val cachedRequest = SelfIdentityingServiceRequest.newBuilder.setExchange(info.getDescriptor.id).setRequest(builder).build

    val future = exe.future[Response[A]]

    pendingRequests.enqueue(RequestWithFuture(cachedRequest, future, info.getDescriptor))
    hook.notifyRequestSpys(verb, payload, future)

    future
  }

  def subscribe[A](descriptor: TypeDescriptor[A]) = ops.subscribe(descriptor)

  /**
   * send all of the pending requests in a single BatchServiceRequests
   */
  def flush() = {
    doBatchRequest(grabPendingRequests(), None)
  }

  /**
   * sends all of the pending requests in multiple BatchServiceRequests of no more than batchSize operations per request.
   * Future blocks until all of the operations have completed (which may take a long time)
   */
  def batchedFlush(batchSize: Int) = {

    def startNextBatch(future: SettableFuture[Response[Boolean]], pending: List[RequestWithFuture[_]], failure: Option[FailureResponse]) {

      if (pending.isEmpty || failure.isDefined) {
        future.set(failure.getOrElse(SuccessResponse(list = List(true))))
      } else {
        val (inProgress, remaining) = pending.splitAt(batchSize)

        doBatchRequest(inProgress, Some(startNextBatch(future, remaining, _)))
      }
    }

    val overallFuture = exe.future[Response[Boolean]]

    startNextBatch(overallFuture, grabPendingRequests(), None)

    Promise.from(overallFuture.map { _.one })
  }

  private def grabPendingRequests() = {
    val pending = pendingRequests.toList
    pendingRequests.clear()
    pending
  }

  private def doBatchRequest(inProgress: List[RequestWithFuture[_]], onResult: Option[(Option[FailureResponse]) => Unit]) = {

    val b = BatchServiceRequest.newBuilder
    inProgress.foreach { o => b.addRequests(o.request) }
    val batchServiceProto = b.build

    val batchFuture = ops.request(Envelope.Verb.POST, batchServiceProto, None)
    batchFuture.listen {
      _ match {
        case SuccessResponse(status, batchResults) =>
          val resultAndFuture = batchResults.head.getRequestsList.toList.map { _.getResponse }.zip { inProgress }
          resultAndFuture.foreach {
            case (response, reqWithFuture) => applyResponse(response, reqWithFuture)
          }
          onResult.foreach { _(None) }
          SuccessResponse(status, batchResults)
        case fail: FailureResponse =>
          inProgress.foreach { _.future.set(fail) }
          // make sure all of the sub future listen() calls have fired
          inProgress.foreach { _.future.await }
          onResult.foreach { _(Some(fail)) }
          fail
      }
    }
    Promise.from(batchFuture.map { _.one })
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