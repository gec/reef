package org.totalgrid.reef.messaging

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
import org.totalgrid.reef.executor.mock.MockExecutor

import org.totalgrid.reef.broker.BrokerConnection
import org.mockito.Mockito

@RunWith(classOf[JUnitRunner])
class BrokerConnectionManagerTest extends FunSuite with ShouldMatchers {

  def fixture(initial: Int, max: Long)(test: (BrokerConnection, MockExecutor, BrokerConnectionManager) => Unit): Unit = {
    val broker = Mockito.mock(classOf[BrokerConnection])
    val exe = new MockExecutor
    val manager = new BrokerConnectionManager(exe, broker, initial, max)
    Mockito.verify(broker).addListener(manager) // mananger adds itself as a listener on construction
    test(broker, exe, manager)
  }

  def fixture(test: (BrokerConnection, MockExecutor, BrokerConnectionManager) => Unit): Unit = fixture(1000, 60000)(test)

  test("No actions until started") {
    fixture { (broker, exe, manager) =>
      exe.numActionsPending should equal(0)
      Mockito.verifyNoMoreInteractions(broker)
    }
  }

  test("Starts causes connection execution") {
    fixture { (broker, exe, manager) =>
      manager.start()
      Mockito.when(broker.connect()).thenReturn(true)
      exe.executeNext(1, 0)
    }
  }

}