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
package org.totalgrid.reef.client.sapi.client.rest.impl

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import net.agileautomata.commons.testing._
import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.MockExecutor
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.proto.Envelope.ServiceResponse

import org.totalgrid.reef.broker.BrokerMessage
import org.totalgrid.reef.client.sapi.client.{ ResponseTimeout, FailureResponse }

@RunWith(classOf[JUnitRunner])
class ResponseCorrelatorTest extends FunSuite with ShouldMatchers {

  def getResponse(uuid: String) = ServiceResponse.newBuilder().setErrorMessage("").setId(uuid).setStatus(Envelope.Status.INTERNAL_ERROR).build()

  test("Calls back on timeout") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator(mock)
    var list: List[Either[FailureResponse, ServiceResponse]] = Nil
    rc.register(1.milliseconds, list ::= _)
    list should equal(Nil)
    mock.tick(1.milliseconds)
    list should equal(List(Left(ResponseTimeout)))
  }

  test("Marshall responses to executor") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator(mock)
    var list: List[Either[FailureResponse, ServiceResponse]] = Nil
    val uuid = rc.register(200.milliseconds, list ::= _)
    val response = getResponse(uuid)
    rc.onMessage(BrokerMessage(response.toByteArray, None))
    mock.runUntilIdle()
    list should equal(List(Right(response)))
  }

  test("Multiple callbacks have no effect") {
    val mock = new MockExecutor
    val rc = new ResponseCorrelator(mock)
    var list: List[Either[FailureResponse, ServiceResponse]] = Nil
    val uuid = rc.register(200.milliseconds, list ::= _)
    val response = getResponse(uuid)
    4.times(rc.onMessage(BrokerMessage(response.toByteArray, None)))
    mock.runUntilIdle()
    list should equal(List(Right(response)))
  }

}
