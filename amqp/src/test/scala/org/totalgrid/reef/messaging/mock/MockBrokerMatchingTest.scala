/**
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
package org.totalgrid.reef.messaging.mock

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MockBrokerMatchingTest extends FunSuite with ShouldMatchers {

  val m = MockBrokerInterface.matches(_, _)
  val r = "valA.valB.valC"

  test("Exact Match") { m(r, r) should equal(true) }
  test("Partial Match") { m(r, "*.valB.valC") should equal(true) }
  test("Hash WildCard") { m(r, "#") should equal(true) }
  test("Star Wildcards") { m(r, "*.*.*") should equal(true) }
  test("Mixed Wildcards") { m(r, "*.#") should equal(true) }
  test("No Exact Match") { m(r, "val1.val2.val3") should equal(false) }
  test("No Partial Match") { m(r, "val1.*.*") should equal(false) }
  test("Partial Key Match") { m(r, "val*.val*.val*") should equal(true) }
  test("Some Partial Key Match") { m(r, "val*.valB.valC") should equal(true) }
  test("Short Binding Key") { m(r, "*.*") should equal(false) }
  test("Shorter Binding Key") { m(r, "*") should equal(false) }
  test("Partial Hash Key") { m(r, "valA.#") should equal(true) }
  test("Partial Bad Hash Key") { m(r, "val1.#") should equal(false) }

}

