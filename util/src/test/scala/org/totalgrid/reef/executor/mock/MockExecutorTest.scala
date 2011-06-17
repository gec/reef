package org.totalgrid.reef.executor.mock

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
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt

@RunWith(classOf[JUnitRunner])
class ExecutorTestBase extends FunSuite with ShouldMatchers {

  test("exceptions thrown when queue is empty") {
    val exe = new MockExecutor
    intercept[Exception](exe.executeNext())
    intercept[Exception](exe.delayNext())
    intercept[Exception](exe.repeatNext())
  }

  test("executes are defered") {
    val exe = new MockExecutor
    var count = 0
    exe.execute(count += 1)
    count should equal(0)
    exe.executeNext()
    count should equal(1)
  }

  test("exceptions thrown when incorrect type is in front of queue") {
    val exe = new MockExecutor
    var count = 0
    exe.execute(count += 1)
    intercept[Exception](exe.delayNext())
    intercept[Exception](exe.repeatNext())
    count should equal(0)
    exe.executeNext()
    count should equal(1)
  }

  test("delays are defered") {
    val exe = new MockExecutor
    var count = 0
    exe.delay(100)(count += 1)
    count should equal(0)
    exe.delayNext() should equal(100)
    count should equal(1)
  }

  test("repeats cycle through queue") {
    val exe = new MockExecutor
    var count = 0
    exe.repeat(100)(count += 1)
    exe.repeat(50)(count += 1)
    count should equal(0)

    val iter = 1001

    iter.count { i =>
      if (i.isOdd) exe.repeatNext() should equal(100)
      else exe.repeatNext() should equal(50)
    }

    count should equal(iter)
  }

  test("Delay timer cancel removes from queue") {
    val exe = new MockExecutor
    var count = 0
    exe.delay(50)(count += 1).cancel()
    count should equal(0)
    intercept[Exception](exe.delayNext())
  }

  test("Delay timer now executes and removes from queue") {
    val exe = new MockExecutor
    var count = 0
    exe.delay(50)(count += 1).now()
    count should equal(1)
    intercept[Exception](exe.delayNext())
  }

  test("Repeat timer cancel removes from queue") {
    val exe = new MockExecutor
    var count = 0
    exe.repeat(50)(count += 1).cancel()
    count should equal(0)
    intercept[Exception](exe.repeatNext())
  }

  test("Repeat timer now executes and returns to queue") {
    val exe = new MockExecutor
    var count = 0
    exe.repeat(50)(count += 1).now()
    count should equal(1)
    exe.repeatNext() should equal(50)
    count should equal(2)
  }

}