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

import org.totalgrid.reef.sapi.ServiceList
import org.totalgrid.reef.sapi.example._
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.broker.api._
import org.totalgrid.reef.sapi.client.Event

import net.agileautomata.commons.testing._
import net.agileautomata.executor4s.Executors

@RunWith(classOf[JUnitRunner])
class QpidServiceClientTest extends ServiceClientTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryServiceClientTest extends ServiceClientTest with MemoryBrokerTestFixture

// common base class with non-broker specific tests
trait ServiceClientTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(fun: (ServiceClient, BrokerConnection) => Unit): Unit = broker { b =>
    val list = ServiceList(SomeIntegerTypeDescriptor)
    val executor = Executors.newScheduledSingleThread()
    try {
      b.declareExchange(list.getServiceInfo(classOf[SomeInteger]).subExchange) //normally a service would do this in bindService
      val client = new ServiceClient(list, b, executor, 5000)
      fun(client, b)
    } finally {
      executor.terminate()
    }
  }

  test("Subscription sequence works if event occurs before start()") {
    fixture { (client, conn) =>
      val si = SomeInteger(42)
      val sub = client.prepareSubscription(SomeIntegerTypeDescriptor) // gets us an unbound queue, doesn't listen yet
      client.bindQueueByClass(sub.id(), "#", classOf[SomeInteger]) //binds the queue to the correct exchange
      client.publishEvent(Envelope.Event.ADDED, si, "foobar")
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.start(events.append)
      events shouldBecome Event(Envelope.Event.ADDED, si) within 5000
    }
  }

  test("Subscription sequence works if event occurs after start()") {
    fixture { (client, conn) =>
      val si = SomeInteger(42)
      val sub = client.prepareSubscription(SomeIntegerTypeDescriptor) // gets us an unbound queue, doesn't listen yet
      client.bindQueueByClass(sub.id(), "#", classOf[SomeInteger]) //binds the queue to the correct exchange
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.start(events.append(_))
      client.publishEvent(Envelope.Event.ADDED, si, "foobar")
      events shouldBecome Event(Envelope.Event.ADDED, si) within 5000
    }
  }

}