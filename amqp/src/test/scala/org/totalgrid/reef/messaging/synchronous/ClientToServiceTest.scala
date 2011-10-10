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

import org.totalgrid.reef.sapi.example._
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.client.{ SingleSuccess, Response }
import net.agileautomata.commons.testing._
import net.agileautomata.executor4s._
import org.totalgrid.reef.sapi.{ AnyNodeDestination, ServiceList }
import org.totalgrid.reef.sapi.newclient.Client

@RunWith(classOf[JUnitRunner])
class QpidClientToService extends ConnectionToServiceTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryClientToService extends ConnectionToServiceTest with MemoryBrokerTestFixture

// provides a specification for how the client should interact with brokers. testable on multiple brokers via minx
trait ConnectionToServiceTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(fun: Client => Unit) = broker { b =>
    val executor = Executors.newScheduledSingleThread()
    var binding: Option[Cancelable] = None
    try {
      val conn = new BasicConnection(ServiceList(SomeIntegerTypeDescriptor), b, executor, 5000)
      binding = Some(conn.bindService(new SomeIntegerIncrementService(conn), executor, AnyNodeDestination, true))
      fun(conn.login("foo"))
    } finally {
      binding.foreach(_.cancel())
      executor.terminate()
    }
  }

  test("Service calls are successful") {
    fixture { c =>
      val i = SomeInteger(1)
      c.put(i).await should equal(Response(Envelope.Status.OK, i.increment))
    }
  }

  test("Subscription calls work") { //subscriptions not currently working with embedded broker
    fixture { c =>
      val events = new SynchronizedList[SomeInteger]
      val sub = c.prepareSubscription(SomeIntegerTypeDescriptor)
      c.put(SomeInteger(1), sub).await should equal(SingleSuccess(single = SomeInteger(2)))
      sub.start(e => events.append(e.value))
      events shouldBecome SomeInteger(2) within 5000
      sub.cancel()
    }
  }

}