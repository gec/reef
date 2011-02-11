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

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class LazyVarTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  test("LazyVar Basics") {
    val lv = LazyVar("default")

    lv.value should equal("default")
    lv.value = "override"

    lv.value should equal("override")
  }

  test("LazyVar Lazyness Check") {
    var sideEffect = 0
    val lv = LazyVar({ sideEffect = 1; "default" })

    lv.value = "override"

    sideEffect should equal(0)

    lv.value should equal("override")
    sideEffect should equal(0)
  }

  test("LazyVar Evaluated Only Once") {
    var sideEffect = 0
    val lv = LazyVar({ sideEffect += 1; "default" })

    sideEffect should equal(0)

    lv.value should equal("default")
    sideEffect should equal(1)

    lv.value should equal("default")
    sideEffect should equal(1)
  }

  test("LazyVar Override Evaluated Only Once") {
    var sideEffect = 0
    val lv = LazyVar("default")

    lv.value = { sideEffect += 1; "override" }

    lv.value should equal("override")
    sideEffect should equal(1)

    lv.value should equal("override")
    sideEffect should equal(1)
  }

  test("LazyVar asOption") {
    val lv = LazyVar("default")

    lv.asOption should equal(None)

    lv.value should equal("default")
    lv.asOption should equal(Some("default"))
  }

  test("LazyVar asOption sees set with value") {
    val lv = LazyVar("default")

    lv.asOption should equal(None)

    lv.value = "override"
    lv.asOption should equal(Some("override"))
  }

  test("LazyVar implict") {
    val lv = LazyVar("default")

    implicit def convertToValue[A](lv: LazyVar[A]): A = lv.value
    val value: String = lv

    value should equal("default")
  }

}