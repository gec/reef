package org.totalgrid.reef.messaging.synchronous

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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.japi.Envelope.ServiceResponse
import org.totalgrid.reef.japi.Envelope

import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.commons.testing._
import org.totalgrid.reef.broker.newapi.BrokerMessage

@RunWith(classOf[JUnitRunner])
class ResponseCorrelatorTest extends FunSuite with ShouldMatchers {

  def getResponse(uuid: String) = ServiceResponse.newBuilder().setErrorMessage("").setId(uuid).setStatus(Envelope.Status.INTERNAL_ERROR).build()

  test("Calls back on timeout") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator
    var list: List[Option[ServiceResponse]] = Nil
    rc.register(mock, 1.milliseconds, list ::= _) { uuid => }
    list should equal(Nil)
    mock.tick(1.milliseconds)
    list should equal(List(None))
  }

  test("Marshall responses to executor") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator
    var list: List[Option[ServiceResponse]] = Nil
    val uuid = rc.register(mock, 200.milliseconds, list ::= _)(uuid => uuid)
    val response = getResponse(uuid)
    rc.onMessage(BrokerMessage(response.toByteArray, None))
    list should equal(List(Some(response)))
  }

  test("Multiple callbacks have no effect") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator
    var list: List[Option[ServiceResponse]] = Nil
    val uuid = rc.register(mock, 200.milliseconds, list ::= _)(uuid => uuid)
    val response = getResponse(uuid)
    4.times(rc.onMessage(BrokerMessage(response.toByteArray, None)))
    list should equal(List(Some(response)))
  }

}
