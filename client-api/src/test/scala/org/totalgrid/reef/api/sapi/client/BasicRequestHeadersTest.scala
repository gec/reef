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
package org.totalgrid.reef.api.sapi.client

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.api.japi.client.AddressableDestination

@RunWith(classOf[JUnitRunner])
class BasicRequestHeadersTest extends FunSuite with ShouldMatchers {
  test("Basic Merge") {
    val h1 = BasicRequestHeaders.empty

    val h2 = BasicRequestHeaders.empty.setDestination(AddressableDestination("test"))

    h1.merge(h2).getDestination should equal(Some(AddressableDestination("test")))
    h2.merge(h1).getDestination should equal(Some(AddressableDestination("test")))
  }

  test("Merge override") {
    val h1 = BasicRequestHeaders.empty.setDestination(AddressableDestination("test1"))

    val h2 = BasicRequestHeaders.empty.setDestination(AddressableDestination("test2"))

    h1.merge(h2).getDestination should equal(Some(AddressableDestination("test2")))
    h2.merge(h1).getDestination should equal(Some(AddressableDestination("test1")))
  }
}