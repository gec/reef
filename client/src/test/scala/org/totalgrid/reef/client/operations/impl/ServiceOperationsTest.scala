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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.mockito.Mockito._

import net.agileautomata.executor4s.testing.InstantExecutor

import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.operations.BindOperations

import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client.operations.scl.ScalaResponse
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.Subscription
import org.totalgrid.reef.client.javaimpl.fixture.{ SomeInteger, SomeIntegerTypeDescriptor }
import org.totalgrid.reef.client.exception._

@RunWith(classOf[JUnitRunner])
class ServiceOperationsTest extends FunSuite with ShouldMatchers {

  private def setupOps() = {
    val rest = mock(classOf[OptionallyBatchedRestOperations], new MockitoStubbedOnly)
    val bindable = mock(classOf[BindOperations], new MockitoStubbedOnly)
    def noBatch(): BatchRestOperations = throw new Exception("No batching")
    val ops = new DefaultServiceOperations(rest, bindable, noBatch _, new InstantExecutor)

    (rest, bindable, ops)
  }

  test("Failure promise throws on await") {

    val (rest, bindable, ops) = setupOps()

    doReturn(TestPromises.fixedError(new BadRequestException(""))).when(rest).get(4)

    val promise = ops.operation("failure")(_.get(4).map(r => r.one))
    intercept[ReefServiceException](promise.await)
  }

  test("New Subscriptions are reported to manager") {
    val (rest, bindable, ops) = setupOps()
    val subscription = mock(classOf[Subscription[SomeInteger]])

    doReturn(subscription).when(bindable).subscribe(SomeIntegerTypeDescriptor)
    doReturn(TestPromises.fixed(ScalaResponse.success(Status.OK, List(8)))).when(rest).get(4)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }
    promise.await.getResult() should equal(8)
  }

  test("Subscription is canceled on failed request") {
    val (rest, bindable, ops) = setupOps()
    val subscription = mock(classOf[Subscription[SomeInteger]])

    doReturn(subscription).when(bindable).subscribe(SomeIntegerTypeDescriptor)
    doReturn(TestPromises.fixedError(new BadRequestException(""))).when(rest).get(4)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }
    verify(subscription).cancel()
    intercept[BadRequestException] { promise.await }
  }

  test("Failed subscription terminates sequence early") {
    val (rest, bindable, ops) = setupOps()

    doThrow(new RuntimeException("Intentional Error")).when(bindable).subscribe(SomeIntegerTypeDescriptor)

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }

    intercept[InternalClientError] { promise.await }
  }

}