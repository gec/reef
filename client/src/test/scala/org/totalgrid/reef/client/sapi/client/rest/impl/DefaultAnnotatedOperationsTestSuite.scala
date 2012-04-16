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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.mockito.Mockito._

import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.{ InstantExecutor, MockFuture }

import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.client.sapi.client._

import org.totalgrid.reef.client.sapi.client.rest.fixture._
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.sapi.client.rest.{ ClientBindOperations, RestOperations }

@RunWith(classOf[JUnitRunner])
class DefaultAnnotatedOperationsTestSuite extends FunSuite with ShouldMatchers {

  test("Failure promise throws on await") {
    val client = mock(classOf[RestOperations], new MockitoStubbedOnly)
    val bindable = mock(classOf[ClientBindOperations], new MockitoStubbedOnly)
    val ops = new DefaultAnnotatedOperations(client, bindable, new InstantExecutor)

    doReturn(MockFuture.defined[Response[Int]](FailureResponse())).when(client).get(4)

    val promise = ops.operation("failure")(_.get(4).map(r => r.one))
    intercept[ReefServiceException](promise.await)
    promise.extract.isFailure should equal(true)
  }

  test("New Subscriptions are reported to manager") {
    val client = mock(classOf[RestOperations], new MockitoStubbedOnly)
    val bindable = mock(classOf[ClientBindOperations], new MockitoStubbedOnly)
    val ops = new DefaultAnnotatedOperations(client, bindable, new InstantExecutor)
    val subscription = mock(classOf[Subscription[SomeInteger]])

    doReturn(subscription).when(bindable).subscribe(SomeIntegerTypeDescriptor)
    doReturn(MockFuture.defined[Response[Int]](SuccessResponse(list = List(8)))).when(client).get(4)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }
    promise.await.getResult() should equal(8)
  }

  test("Subscription is canceled on failed request") {
    val client = mock(classOf[RestOperations], new MockitoStubbedOnly)
    val bindable = mock(classOf[ClientBindOperations], new MockitoStubbedOnly)
    val ops = new DefaultAnnotatedOperations(client, bindable, new InstantExecutor)
    val subscription = mock(classOf[Subscription[SomeInteger]])

    doReturn(subscription).when(bindable).subscribe(SomeIntegerTypeDescriptor)
    doReturn(MockFuture.defined[Response[Int]](FailureResponse())).when(client).get(4)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }
    verify(subscription).cancel()
    promise.extract.isFailure should equal(true)
  }

  test("Failed subscription terminates sequence early") {
    val client = mock(classOf[RestOperations], new MockitoStubbedOnly)
    val bindable = mock(classOf[ClientBindOperations], new MockitoStubbedOnly)
    val ops = new DefaultAnnotatedOperations(client, bindable, new InstantExecutor)

    doThrow(new RuntimeException("Intentional Error")).when(bindable).subscribe(SomeIntegerTypeDescriptor)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }

    promise.extract.isFailure should equal(true)
  }

}