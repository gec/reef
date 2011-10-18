package org.totalgrid.reef.sapi.client.rest.impl

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
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.mockito.Mockito._

import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.MockFuture

import org.totalgrid.reef.api.japi.ReefServiceException
import org.totalgrid.reef.api.sapi.client._

import rest.Client
import org.totalgrid.reef.sapi.rest.impl.DefaultAnnotatedOperations
import org.totalgrid.reef.api.sapi.example.{SomeIntegerTypeDescriptor, SomeInteger}

@RunWith(classOf[JUnitRunner])
class DefaultAnnotatedOperationsTestSuite extends FunSuite with ShouldMatchers {

  test("Failure promise throws on await") {
    val client = mock(classOf[Client])
    val ops = new DefaultAnnotatedOperations(client)

    when(client.getHeaders) thenReturn BasicRequestHeaders.empty
    when(client.get(4)) thenReturn (MockFuture.defined[Response[Int]](FailureResponse()))

    val promise = ops.operation("failure")(_.get(4).map(r => r.one))
    intercept[ReefServiceException](promise.await)
    promise.extract.isFailure should equal(true)
  }

  test("Subscription is canceled on failed request") {
    val client = mock(classOf[Client])
    val subscription = mock(classOf[Subscription[SomeInteger]])
    val ops = new DefaultAnnotatedOperations(client)

    when(client.getHeaders) thenReturn BasicRequestHeaders.empty
    when(client.subscribe(SomeIntegerTypeDescriptor)) thenReturn Success(subscription)
    when(client.get(4)) thenReturn (MockFuture.defined[Response[Int]](FailureResponse()))

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }

    verify(subscription).cancel()
    promise.extract.isFailure should equal(true)
  }

  test("Failed subscription terminates sequence early") {
    val client = mock(classOf[Client])
    val ops = new DefaultAnnotatedOperations(client)

    when(client.subscribe(SomeIntegerTypeDescriptor)) thenReturn Failure("foobar")
    when(client.future[Result[SomeInteger]]) thenReturn MockFuture.undefined[Result[SomeInteger]]

    val promise = ops.subscription(SomeIntegerTypeDescriptor, "failure") { (sub, client) =>
      client.get(4).map(r => r.one)
    }

    promise.extract.isFailure should equal(true)
    verify(client).subscribe(SomeIntegerTypeDescriptor)
    verify(client).future[Result[SomeInteger]]
    verify(client).future[Result[SomeInteger]]
    verifyNoMoreInteractions(client)

  }

}