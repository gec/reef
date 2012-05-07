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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s.testing.MockFuture
import org.totalgrid.reef.client.javaimpl.fixture._
import org.totalgrid.reef.client.types.ServiceTypeInformation
import org.totalgrid.reef.client.proto.Envelope.{ BatchServiceRequest, Verb }

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.operations.{ RestOperations, Response }
import org.totalgrid.reef.client.sapi.client.rest.impl.ClassLookup
import org.totalgrid.reef.client.javaimpl.ResponseWrapper
import org.totalgrid.reef.client.operations.impl.FuturePromise.OpenEitherPromise
import org.totalgrid.reef.client.exception._
import org.totalgrid.reef.client.Promise
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders

@RunWith(classOf[JUnitRunner])
class BatchServiceOperationsTest extends FunSuite with ShouldMatchers {

  private def duplicatePayload(onRequest: () => Unit, request: BatchServiceRequest): Response[BatchServiceRequest] = {
    val batchResponse = BatchServiceRequest.newBuilder
    onRequest()
    request.getRequestsList.toList.map { req =>
      val request = req.getRequest
      val response = Envelope.ServiceResponse.newBuilder.setId(request.getId).setStatus(Envelope.Status.OK)
      response.addPayload(request.getPayload)
      response.addPayload(request.getPayload)
      batchResponse.addRequests(req.toBuilder.setResponse(response))
    }
    ResponseWrapper.success(Envelope.Status.OK, List(batchResponse.build))
  }

  private def conditionalSuccess(errorMessages: List[Option[String]])(request: BatchServiceRequest): Response[BatchServiceRequest] = {
    val batchResponse = BatchServiceRequest.newBuilder

    request.getRequestsList.toList.zip(errorMessages).map {
      case (req, errorMsg) =>
        val request = req.getRequest
        val response = Envelope.ServiceResponse.newBuilder.setId(request.getId)
        if (errorMsg.isEmpty) response.setStatus(Envelope.Status.OK).addPayload(request.getPayload)
        else response.setStatus(Envelope.Status.BAD_REQUEST).setErrorMessage(errorMsg.get)
        batchResponse.addRequests(req.toBuilder.setResponse(response))
    }
    new ResponseWrapper(Envelope.Status.BAD_REQUEST, List(batchResponse.build), "Batch failed because: " + errorMessages.flatten.head, false)
  }

  private def badAuthFailure(request: BatchServiceRequest): Response[BatchServiceRequest] = {
    ResponseWrapper.failure(Envelope.Status.UNAUTHORIZED, "not authorized")
  }

  case class RealRequestCounter(var requests: Int = 0) {
    def increment() = requests += 1
  }

  class MockBatch(responseFun: BatchServiceRequest => Response[BatchServiceRequest]) extends BatchRestOperationsImpl {
    protected def getServiceInfo[A](klass: Class[A]) = {
      klass should equal(classOf[SomeInteger])
      ExampleServiceList.info.asInstanceOf[ServiceTypeInformation[A, A]]
    }

    protected def futureSource[A] = new OpenEitherPromise[A](new MockFuture[Either[ReefServiceException, A]](None))

    protected def ops = new DerivedRestOperations with RestOperations {
      protected def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {
        ClassLookup(payload) should equal(Some(classOf[BatchServiceRequest]))

        val batchRequest = payload.asInstanceOf[BatchServiceRequest]
        val response = responseFun(batchRequest).asInstanceOf[Response[A]]

        TestPromises.fixed(response)
      }
    }

    protected def notifyListeners[A](verb: Verb, payload: A, promise: Promise[Response[A]]) {}
  }

  test("Single Request works") {
    val requestCounter = new RealRequestCounter()
    val ops = new MockBatch(duplicatePayload(requestCounter.increment _, _))

    val future = ops.request(Envelope.Verb.PUT, SomeInteger(100))

    future.isComplete should equal(false)

    ops.flush().await

    future.isComplete should equal(true)
    future.await.getList.toList should equal(List(SomeInteger(100), SomeInteger(100)))

    requestCounter.requests should equal(1)
  }

  test("Multiple Requests works") {
    val requestCounter = new RealRequestCounter()
    val ops = new MockBatch(duplicatePayload(requestCounter.increment _, _))

    val futures = (0 to 100).map { i => ops.request(Envelope.Verb.PUT, SomeInteger(i)) }

    futures.map { _.isComplete }.distinct should equal(List(false))

    ops.flush()

    futures.map { _.isComplete }.distinct should equal(List(true))
    futures.zipWithIndex.foreach {
      case (value, index) =>
        value.await.getList.toList should equal(List(SomeInteger(index), SomeInteger(index)))
    }
    requestCounter.requests should equal(1)
  }

  test("Handles General Batch Level Failure") {
    val ops = new MockBatch(badAuthFailure _)

    val future = ops.request(Envelope.Verb.PUT, SomeInteger(100))

    future.isComplete should equal(false)

    val batchFuture = ops.flush()

    intercept[UnauthorizedException] {
      batchFuture.await
    }.getMessage should include("not authorized")

    future.isComplete should equal(true)

    intercept[UnauthorizedException] {
      future.await
    }.getMessage should include("not authorized")
  }

  test("Handles Partial Failures") {
    val ops = new MockBatch(conditionalSuccess(List(None, Some("partial failure"))))

    val successFuture = ops.request(Envelope.Verb.PUT, SomeInteger(100))
    val failureFuture = ops.request(Envelope.Verb.PUT, SomeInteger(200))

    val batchResult = ops.flush()

    intercept[BadRequestException] {
      batchResult.await
    }.getMessage should include("partial failure")

    successFuture.isComplete should equal(true)
    failureFuture.isComplete should equal(true)

    intercept[BadRequestException] {
      successFuture.await
    }.getMessage should include("partial failure")

    intercept[BadRequestException] {
      failureFuture.await
    }.getMessage should include("partial failure")
  }

  test("BatchedFlush will send in chunks") {
    val requestCounter = new RealRequestCounter()
    val ops = new MockBatch(duplicatePayload(requestCounter.increment _, _))

    (1 to 13).map { i => ops.request(Envelope.Verb.PUT, SomeInteger(i)) }

    val batchResult = ops.batchedFlush(4).await

    requestCounter.requests should equal(3 + 1)
  }

}