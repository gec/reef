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
package org.totalgrid.reef.clientapi.sapi.client

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.clientapi.AddressableDestination

@RunWith(classOf[JUnitRunner])
class BasicRequestHeadersTest extends FunSuite with ShouldMatchers {
  test("Basic Merge") {
    val h1 = BasicRequestHeaders.empty

    val h2 = BasicRequestHeaders.empty.setDestination(new AddressableDestination("test"))

    h1.merge(h2).getDestination.map(_.getKey) should equal(Some("test"))
    h2.merge(h1).getDestination.map(_.getKey) should equal(Some("test"))
  }

  test("Merge override") {
    val h1 = BasicRequestHeaders.empty.setDestination(new AddressableDestination("test1"))

    val h2 = BasicRequestHeaders.empty.setDestination(new AddressableDestination("test2"))

    h1.merge(h2).getDestination.map(_.getKey) should equal(Some("test2"))
    h2.merge(h1).getDestination.map(_.getKey) should equal(Some("test1"))
  }
}