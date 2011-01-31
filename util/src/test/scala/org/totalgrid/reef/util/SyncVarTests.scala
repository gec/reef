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
package org.totalgrid.reef.util

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.actors.Actor.actor
import org.totalgrid.reef.util.Conversion._

@RunWith(classOf[JUnitRunner])
class SyncVarTests extends FunSuite with ShouldMatchers {

  test("TestInitialValue") {
    val sv = new SyncVar(0)
    sv.waitFor(_ == 0, 0)
    sv.current should equal(0)
  }

  test("TestException") {
    val sv = new SyncVar(0)
    intercept[Exception] {
      sv.waitFor(_ == 1, 0)
    }
    sv.current should equal(0)
  }

  test("AsynchronousUpdates") {
    val sv = new SyncVar(0)
    val num = 1000

    actor {
      num.count(i => sv.update(i))
    }

    num.count { i =>
      sv.waitFor(_ == i)
    }

    intercept[Exception] { sv.waitFor(_ == num + 1, 10) }

    sv.current should equal(num)
  }

  test("AtomicUpdates") {
    val sv = new SyncVar(0)
    val num = 100 //100 actors w/ 100 increments

    num.times {
      actor {
        num.times(sv.atomic(_ + 1))
      }
    }

    sv.waitFor(_ == num * num)
  }

  test("ReadsToLastValue") {
    val sv = new SyncVar(0)
    val num = 1000

    actor {
      num.count(i => sv.update(i))
    }

    sv.waitFor(_ == num, 5000)

    sv.current should equal(num)
  }

  test("WaitForUnreached") {
    val sv = new SyncVar(0)

    intercept[Exception] {
      sv.waitFor(_ == 10, 1)
    }

    sv.waitFor(_ == 10, 1, false) should equal(false)
  }
}
