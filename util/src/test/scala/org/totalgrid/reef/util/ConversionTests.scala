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
package org.totalgrid.reef.util

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import Conversion._
@RunWith(classOf[JUnitRunner])
class ConversionTests extends FunSuite with ShouldMatchers {
  test("convertStringToType") {
    convertStringToType("99") should equal(99)
    convertStringToType("2147483648") should equal(2147483648L)
    convertStringToType("true") should equal(true)
    convertStringToType("100.5") should equal(100.5)
    convertStringToType("magic") should equal("magic")
  }

  test("testConversionType") {
    convertStringToType("99") match {
      case x: Int => x should equal(99)
    }

  }
}