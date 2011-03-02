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
package org.totalgrid.reef.api.scalaclient

import org.totalgrid.reef.api.scalaclient.mock._
import org.totalgrid.reef.api.ServiceTypes._
import org.totalgrid.reef.api.Envelope

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class FuturesTest extends FunSuite with ShouldMatchers {

  def fixture(verb: Envelope.Verb)(func: FutureOperations => (Object) => () => MultiResult[Object]) {
    val mock = new MockServiceClient
    val obj = new Object
    val future = func(mock)(obj)
    mock.callback(MultiSuccess(Nil))
    mock.next() match {
      case RequestRecord(verb2, obj2, _, _) =>
        verb2 should equal(verb)
        obj2 should equal(obj)
    }
    future() should equal(MultiSuccess(Nil))
  }

  test("FutureDelaysBlockingResponse") {
    import Envelope.Verb._
    fixture(GET) { mock => mock.getWithFuture[Object](_) }
    fixture(PUT) { mock => mock.putWithFuture[Object](_) }
    fixture(POST) { mock => mock.postWithFuture[Object](_) }
    fixture(DELETE) { mock => mock.deleteWithFuture[Object](_) }
  }

}