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

import org.totalgrid.reef.client.operations.scl.ScalaPromise._
import org.totalgrid.reef.client.exception._
import org.totalgrid.reef.client.operations.scl.PromiseCollators
import net.agileautomata.executor4s.testing.{ InstantExecutor, MockFuture }

@RunWith(classOf[JUnitRunner])
class FuturePromiseTest extends FunSuite with ShouldMatchers {

  def future() = new MockFuture[Either[ReefServiceException, java.lang.Integer]](None)

  def doubleFun(int: java.lang.Integer): java.lang.Integer = if (int < 0) throw new ExpectationException("Can't double") else int + int

  test("Simple Transform") {
    val promise = FuturePromise.open(future())

    promise.isComplete should equal(false)
    val doubled = promise.map(doubleFun)

    doubled.isComplete should equal(false)

    promise.setSuccess(10)

    doubled.isComplete should equal(true)
    promise.isComplete should equal(true)

    promise.await should equal(10)
    doubled.await should equal(20)
  }

  test("Simple Transform Failure") {
    val promise = FuturePromise.open(future())

    val doubled = promise.map(doubleFun)
    val doubledAndAnnotated = doubled.mapError { rse => rse.addExtraInformation("annotated"); rse }

    promise.setSuccess(-10)

    promise.await should equal(-10)
    intercept[ExpectationException] {
      doubled.await
    }.getMessage should not include ("annotated")

    intercept[ExpectationException] {
      doubledAndAnnotated.await
    }.getMessage should include("annotated")
  }

  test("Request Failure") {
    val promise = FuturePromise.open(future())

    val doubled = promise.map(doubleFun)
    val doubledAndAnnotated = doubled.mapError { rse => rse.addExtraInformation("annotated"); rse }

    promise.setFailure(new BadRequestException("bad stuff"))

    intercept[BadRequestException] {
      doubled.await
    }.getMessage should include("bad stuff")

    val msg = intercept[BadRequestException] {
      doubledAndAnnotated.await
    }.getMessage
    msg should include("annotated")
    msg should include("bad stuff")
  }

  class IntListener {
    var error: Throwable = null
    var result: Int = -1

    def listen(v: Either[Throwable, java.lang.Integer]) {
      v match {
        case Right(value) => result = value
        case Left(ex) => error = ex
      }
    }
    def complete = error != null || result != -1
  }

  test("Listen success") {
    val promise = FuturePromise.open(future())

    val doubled = promise.map(doubleFun)
    val doubledAndAnnotated = doubled.mapError { rse => rse.addExtraInformation("annotated"); rse }

    val doubledListener = new IntListener
    doubledAndAnnotated.listenEither(doubledListener.listen _)

    doubledListener.complete should equal(false)

    promise.setSuccess(5)

    doubledListener.complete should equal(true)

    val promiseListener = new IntListener
    promise.listenEither(promiseListener.listen _)
    promiseListener.complete should equal(true)

    promiseListener.result should equal(5)
    doubledListener.result should equal(10)
  }

  test("Listen failure") {
    val promise = FuturePromise.open(future())

    val doubled = promise.map(doubleFun)
    val doubledAndAnnotated = doubled.mapError { rse => rse.addExtraInformation("annotated"); rse }

    val doubledListener = new IntListener
    doubledAndAnnotated.listenEither(doubledListener.listen _)

    promise.setFailure(new BadRequestException("bad stuff"))

    doubledListener.complete should equal(true)

    val promiseListener = new IntListener
    promise.listenEither(promiseListener.listen _)
    promiseListener.complete should equal(true)

    promiseListener.error.getMessage should include("bad stuff")
    doubledListener.error.getMessage should include("annotated")
  }

  test("Exception in errorTransform Failure") {
    val promise = FuturePromise.open(future())

    val annotated = promise.mapError(rse => throw new ExpectationException("Bleh"))

    promise.setFailure(new BadRequestException("bad stuff"))

    val msg = intercept[InternalClientError] {
      annotated.await
    }.getMessage
    msg should include("bad stuff")
    msg should include("Bleh")
  }

  test("OpenPromise onAwait is called") {

    var throwException = true
    def checkFlag = if (throwException) throw new ExpectationException("intentional error")
    val future = new MockFuture[Either[ReefServiceException, java.lang.Integer]](Some(Right(3)))
    val promise = FuturePromise.openWithAwaitNotifier(future, Some(checkFlag _))

    intercept[ExpectationException] {
      promise.await
    }

    throwException = false
    promise.await
  }

  test("Successfull promise collation") {
    val promise1 = FuturePromise.open(future())
    val promise2 = FuturePromise.open(future())

    val collated = PromiseCollators.collate(new InstantExecutor(), List(promise1, promise2))
    collated.isComplete should equal(false)

    promise1.setSuccess(5)
    collated.isComplete should equal(false)

    promise2.setSuccess(10)
    collated.isComplete should equal(true)

    collated.await() should equal(List(5, 10))
  }

  test("Partial failure promise collation") {
    val promise1 = FuturePromise.open(future())
    val promise2 = FuturePromise.open(future())

    val collated = PromiseCollators.collate(new InstantExecutor(), List(promise1, promise2))
    collated.isComplete should equal(false)

    promise1.setSuccess(5)
    collated.isComplete should equal(false)

    val ex = new ExpectationException("intentional")
    promise2.setFailure(ex)
    collated.isComplete should equal(true)

    intercept[ExpectationException] {
      collated.await()
    } should equal(ex)
  }

  // TODO: fix collated promise to use call all childrens onAwaits
  ignore("Collated Promises use onAwait callback") {
    def checkFlag = throw new ExpectationException("intentional error")
    val future = new MockFuture[Either[ReefServiceException, java.lang.Integer]](Some(Right(3)))
    val promise = FuturePromise.openWithAwaitNotifier(future, Some(checkFlag _))

    val collated = PromiseCollators.collate(new InstantExecutor(), List(promise))

    intercept[ExpectationException] {
      collated.await
    }
  }
}
