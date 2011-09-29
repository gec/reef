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

import net.agileautomata.commons.testing._
import net.agileautomata.executor4s.Executors

import org.jmock.lib.concurrent.DeterministicScheduler

import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class ResponseCorrelatorTest extends FunSuite with ShouldMatchers {

  def getResponse(uuid: String) = ServiceResponse.newBuilder().setErrorMessage("").setId(uuid).setStatus(Envelope.Status.INTERNAL_ERROR).build()

  test("Calls back on timeout") {
    val mock = new DeterministicScheduler
    val rc = new ResponseCorrelator(Executors.newCustomExecutor(mock))
    var list: List[Option[ServiceResponse]] = Nil
    def add(rsp: Option[ServiceResponse]) = list ::= rsp
    rc.register(add, 1)
    list should equal(Nil)
    mock.tick(1, TimeUnit.MILLISECONDS)
    list should equal(List(None))
  }

  test("Marshall responses to executor") {
    val mock = new DeterministicScheduler
    val rc = new ResponseCorrelator(Executors.newCustomExecutor(mock))
    var list: List[Option[ServiceResponse]] = Nil
    def add(rsp: Option[ServiceResponse]) = list ::= rsp
    val uuid = rc.register(add, 200)
    val response = getResponse(uuid)
    rc.receive(response.toByteArray, None)
    list should equal(Nil)
    mock.runNextPendingCommand()
    list should equal(List(Some(response)))
  }

  test("Multiple callbacks have no effect") {
    val mock = new DeterministicScheduler
    val rc = new ResponseCorrelator(Executors.newCustomExecutor(mock))
    var list: List[Option[ServiceResponse]] = Nil
    def add(rsp: Option[ServiceResponse]) = list ::= rsp
    val uuid = rc.register(add, 200)
    val response = getResponse(uuid)
    4.times(rc.receive(response.toByteArray, None))
    mock.runUntilIdle()
    list should equal(List(Some(response)))
  }

}
