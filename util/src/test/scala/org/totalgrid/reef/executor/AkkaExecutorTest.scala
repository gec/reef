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

package org.totalgrid.reef.executor

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt
import parallel.Future
import java.util.concurrent.{ TimeUnit, CountDownLatch }

@RunWith(classOf[JUnitRunner])
class AkkaExecutorTest extends FunSuite with ShouldMatchers {

  class CountDown(count: Int) extends CountDownLatch(count) {
    def waitForZero(msec: Long = 500) = this.await(msec, TimeUnit.MILLISECONDS) should equal(true)
    def waitForNoZero(msec: Long = 500) = this.await(msec, TimeUnit.MILLISECONDS) should equal(false)
  }

  def fixture(fun: AkkaExecutor => Unit) = {
    val exe = new AkkaExecutor
    exe.start()
    try {
      fun(exe)
    } finally {
      exe.stop()
    }
  }

  test("stop and start calls are idempotent") {
    fixture { exe =>
      exe.start()
      exe.start()

      exe.stop()
      exe.stop()
    }
  }

  test("execution") {
    fixture { exe =>
      val cd = new CountDown(1)
      exe.execute(cd.countDown())
      cd.waitForZero()
    }
  }

  test("unrevoked delay") {
    fixture { exe =>
      val cd = new CountDown(1)
      exe.delay(1)(cd.countDown())
      cd.waitForZero()
    }
  }

  test("revoked delay") {
    fixture { exe =>
      val cd = new CountDown(1)
      exe.delay(100)(cd.countDown()).cancel()
      cd.waitForNoZero()
    }
  }

  test("delay done now") {
    fixture { exe =>
      val cd = new CountDown(1)
      exe.delay(5000)(cd.countDown()).now()
      cd.waitForZero()
    }
  }

  test("repeat called initially") {
    fixture { exe =>
      val cd = new CountDown(1)
      exe.repeat(5000)(cd.countDown())
      cd.waitForZero()
    }
  }

  test("repeat") {
    fixture { exe =>
      val cd = new CountDown(5)
      exe.repeat(1)(cd.countDown())
      cd.waitForZero()
    }
  }

  test("canceled repeat") {
    fixture { exe =>
      val cd = new CountDown(5)
      exe.repeat(1)(cd.countDown()).cancel()
      cd.waitForNoZero()
    }
  }

  test("Request marshalls exceptions") {
    fixture { exe =>
      5.times {
        val future: Future[Int] = exe.request(1 / 0)
        intercept[ArithmeticException](future())
      }
    }
  }

  test("Futures can be parallelized") {
    fixture { a =>
      fixture { b =>
        val f1 = a.request(3 * 3)
        val f2 = b.request(4 * 4)
        (f1() + f2()) should equal(25)
      }
    }
  }

  test("restart executor") {
    fixture { exe =>
      5.times {
        exe.start()
        exe.request(1)() should equal(1)
        exe.stop()
      }
    }
  }

  test("killing parent kills timers") {

    // race condition on the unlink, need to repeat to make error occur
    fixture { exe =>
      val cd = new CountDown(10)
      exe.repeat(1)(cd.countDown())
      exe.stop()
      cd.waitForNoZero(1000)
    }
  }

}