/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.metrics

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.util.Timing

@RunWith(classOf[JUnitRunner])
class MetricsTests extends FunSuite with ShouldMatchers {

  class TestObject extends MetricsHooks {
    lazy val hook1 = valueHook("hook1")
    lazy val timehook1 = timingHook[Unit]("timingHook")

    def doSomething(t: Int) = {
      hook1(t)
    }

    def timeSomething(t: Int) = {
      timehook1 {
        Thread.sleep(t * 2)
      }
    }

    def timeNothing() = {
      timehook1 {}
    }
  }

  class LastValueHookSink extends MetricsHookSource {
    var hookName = ""
    var hookVal = -1
    def getSinkFunction(s: String, typ: MetricsHooks.HookType): (Int) => Unit = { (i: Int) =>
      {
        hookName = s
        hookVal = i
      }
    }
  }

  /// This test shows that the startup/teardown behavior is working without crashing
  test("UnHooked throws exception if forceHooks=true") {
    MetricsHooks.forceHooks = true
    val to = new TestObject
    intercept[UnHookedException] {
      to.doSomething(1)
    }
  }

  test("UnHooked silently eats value updates if forceHooks = false") {
    MetricsHooks.forceHooks = false
    val to = new TestObject
    to.doSomething(1)
  }

  test("Hooked values are collected") {
    MetricsHooks.forceHooks = false
    val to = new TestObject
    val source = new LastValueHookSink
    to.setHookSource(source)
    source.hookVal should equal(-1)
    source.hookName should equal("")
    to.doSomething(1)
    source.hookVal should equal(1)
    source.hookName should equal("hook1")
    to.doSomething(99)
    source.hookVal should equal(99)
  }

  test("Hooked timing values are collected") {

    val to = new TestObject
    var hookVal = -1
    var hookName = ""
    val source = new LastValueHookSink
    to.setHookSource(source)

    to.timeSomething(100)
    source.hookVal should be >= 100
    source.hookName should equal("timingHook")
    to.timeSomething(50)
    source.hookVal should be >= 50
  }

  test("UnHooked timing overhead is minimal") {

    val unhooked = new TestObject

    val hooked = new TestObject
    hooked.setHookSource(new SilentHookSource)

    var unhookedTime: Long = -1
    var hookedTime: Long = -1
    val iters = 1000000
    Timing.time({ (x: Long) => unhookedTime = x }) {
      for (a <- 1 to iters) unhooked.timeNothing()
    }
    Timing.time({ (x: Long) => hookedTime = x }) {
      for (a <- 1 to iters) hooked.timeNothing()
    }
    unhookedTime should not equal (-1)
    hookedTime should not equal (-1)
    //println("unhooked = " + unhookedTime + " hooked = " + hookedTime)
    //unhookedTime should be <= hookedTime
  }
}
