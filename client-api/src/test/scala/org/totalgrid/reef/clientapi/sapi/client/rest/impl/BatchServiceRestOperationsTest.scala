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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito._
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.clientapi.proto.Envelope
import org.mockito.Mockito
import net.agileautomata.executor4s.testing.MockFuture
import org.totalgrid.reef.clientapi.sapi.client.rest.fixture._
import org.totalgrid.reef.clientapi.types.TypeDescriptor
import org.totalgrid.reef.clientapi.types.ServiceTypeInformation
import org.totalgrid.reef.clientapi.sapi.client.rest.{ ServiceRegistry, Client, RestOperations }
import org.totalgrid.reef.clientapi.proto.Envelope.{ BatchServiceRequest, Verb }

import scala.collection.JavaConversions._
import net.agileautomata.executor4s.{ Failure, TimeInterval, Executor }
import org.totalgrid.reef.clientapi.sapi.client._
import org.totalgrid.reef.clientapi.rpc.{ ServicesList, RpcProviderInfo }

@RunWith(classOf[JUnitRunner])
class BatchServiceRestOperationsTest extends FunSuite with ShouldMatchers {

  class MockRestOperations(responseFun: BatchServiceRequest => Response[BatchServiceRequest]) extends RestOperations with ServiceRegistry with Executor with RequestSpyHook {
    def request[A](verb: Verb, payload: A, headers: Option[BasicRequestHeaders]) = {

      ClassLookup(payload) should equal(Some(classOf[BatchServiceRequest]))

      val batchRequest = payload.asInstanceOf[BatchServiceRequest]

      val response = responseFun(batchRequest).asInstanceOf[Response[A]]

      MockFuture.defined(response)
    }

    def getServiceInfo[A](klass: Class[A]) = {
      klass should equal(classOf[SomeInteger])
      ExampleServiceList.info.asInstanceOf[ServiceTypeInformation[A, A]]
    }

    override def future[A] = new MockFuture(None)

    def subscribe[A](descriptor: TypeDescriptor[A]) = throw new Exception
    def addServiceInfo[A](info: ServiceTypeInformation[A, _]) = throw new Exception
    def addServicesList(servicesList: ServicesList) = throw new Exception
    def addRpcProvider(info: RpcProviderInfo) = throw new Exception
    def attempt[A](fun: => A) = throw new Exception
    def execute(fun: => Unit) = throw new Exception
    def schedule(interval: TimeInterval)(fun: => Unit) = throw new Exception
    def scheduleWithFixedOffset(initial: TimeInterval, offset: TimeInterval)(fun: => Unit) = throw new Exception
  }

  private def duplicatePayload(request: BatchServiceRequest) = {
    val batchResponse = BatchServiceRequest.newBuilder

    request.getRequestsList.toList.map { req =>
      val request = req.getRequest
      val response = Envelope.ServiceResponse.newBuilder.setId(request.getId).setStatus(Envelope.Status.OK)
      response.addPayload(request.getPayload)
      response.addPayload(request.getPayload)
      batchResponse.addRequests(req.toBuilder.setResponse(response))
    }
    SuccessResponse(list = List(batchResponse.build))
  }

  private def conditionalSuccess(errorMessages: List[Option[String]])(request: BatchServiceRequest) = {
    val batchResponse = BatchServiceRequest.newBuilder

    request.getRequestsList.toList.zip(errorMessages).map {
      case (req, errorMsg) =>
        val request = req.getRequest
        val response = Envelope.ServiceResponse.newBuilder.setId(request.getId)
        if (errorMsg.isEmpty) response.setStatus(Envelope.Status.OK).addPayload(request.getPayload)
        else response.setStatus(Envelope.Status.BAD_REQUEST).setErrorMessage(errorMsg.get)
        batchResponse.addRequests(req.toBuilder.setResponse(response))
    }
    Response(Envelope.Status.BAD_REQUEST, List(batchResponse.build), "Batch failed because: " + errorMessages.flatten.head)
  }

  private def badAuthFailure(request: BatchServiceRequest) = {
    FailureResponse(Envelope.Status.UNAUTHORIZED, "not authorized")
  }

  test("Single Request works") {
    val client = new MockRestOperations(duplicatePayload _)
    val ops = new BatchServiceRestOperations(client)

    val future = ops.request(Envelope.Verb.PUT, SomeInteger(100), None)

    future.isComplete should equal(false)

    ops.flush().await

    future.isComplete should equal(true)
    future.await.list should equal(List(SomeInteger(100), SomeInteger(100)))
  }

  test("Multiple Requests works") {
    val client = new MockRestOperations(duplicatePayload _)
    val ops = new BatchServiceRestOperations(client)

    val futures = (0 to 100).map { i =>
      ops.request(Envelope.Verb.PUT, SomeInteger(i), None)
    }

    futures.map { _.isComplete }.distinct should equal(List(false))

    ops.flush()

    futures.map { _.isComplete }.distinct should equal(List(true))
    futures.zipWithIndex.foreach {
      case (value, index) =>
        value.await.list should equal(List(SomeInteger(index), SomeInteger(index)))
    }

  }

  test("Handles General Batch Level Failure") {
    val client = new MockRestOperations(badAuthFailure _)
    val ops = new BatchServiceRestOperations(client)

    val future = ops.request(Envelope.Verb.PUT, SomeInteger(100), None)

    future.isComplete should equal(false)

    val batchFuture = ops.flush()

    batchFuture.await.success should equal(false)
    batchFuture.await.toString should include("not authorized")

    future.isComplete should equal(true)
    future.await.success should equal(false)
    future.await.toString should include("not authorized")
  }

  test("Handles Partial Failures") {
    val client = new MockRestOperations(conditionalSuccess(List(None, Some("partial failure"))))
    val ops = new BatchServiceRestOperations(client)

    val successFuture = ops.request(Envelope.Verb.PUT, SomeInteger(100), None)
    val failureFuture = ops.request(Envelope.Verb.PUT, SomeInteger(200), None)

    val batchResult = ops.flush().await

    successFuture.await.success should equal(false)
    successFuture.await.toString should include("partial failure")
    failureFuture.await.success should equal(false)
    failureFuture.await.toString should include("partial failure")

    batchResult.success should equal(false)
    batchResult.toString should include("partial failure")
  }

}