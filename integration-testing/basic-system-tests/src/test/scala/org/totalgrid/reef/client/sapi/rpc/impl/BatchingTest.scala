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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.totalgrid.reef.client.exception.ServiceIOException

@RunWith(classOf[JUnitRunner])
class BatchingTest extends ServiceClientSuite {

  test("Batching with simple get waits") {

    // this batching mode change doesn't affect the services we are spawning
    async.batching.start()
    try {
      val promise = async.getPoints()

      intercept[ServiceIOException] {
        // can't call await
        promise.await()
      }.getMessage should include("flush")

      promise.isComplete should equal(false)

      async.batching.flush().await should equal(1)

      promise.isComplete should equal(true)
    } finally {
      async.batching.exit()
    }
  }

  test("Using Batching with sync api fails fast") {
    // this batching mode change doesn't affect the services we are spawning
    session.getBatching.start()
    try {
      intercept[ServiceIOException] {
        client.getPoints()
      }.getMessage should include("flush")

    } finally {
      session.getBatching.exit()
    }
  }

  test("Batching with get by names waits") {

    val allResp = async.getPoints().await

    // this batching mode change doesn't affect the services we are spawning
    async.batching.start()
    try {
      val promise = async.getPointsByUuids(allResp.map { _.getUuid })

      // TODO: batchGets await doesn't have exception
      //      intercept[ServiceIOException] {
      //        // can't call await
      //        promise.await()
      //      }.getMessage should include("flush")

      promise.isComplete should equal(false)

      async.batching.flush().await should equal(allResp.size)

      // TODO: this isComplete wont be set immediately after flush since its waiting for a
      // listen to propegate the value through collated promise
      //promise.isComplete should equal(true)
    } finally {
      async.batching.exit()
    }
  }

}