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
package org.totalgrid.reef.clientapi.sapi.client.rest.impl

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import net.agileautomata.commons.testing._
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.clientapi.sapi.client.rest.{ Connection, Client }
import org.totalgrid.reef.clientapi.sapi.client.rest.fixture._
import org.totalgrid.reef.clientapi.proto.Envelope

import org.totalgrid.reef.clientapi.sapi.client.Event

@RunWith(classOf[JUnitRunner])
class QpidServiceClientTest extends BasicClientTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryServiceClientTest extends BasicClientTest with MemoryBrokerTestFixture

// common base class with non-broker specific tests
trait BasicClientTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(fun: (Client, Connection) => Unit): Unit = broker { b =>
    val executor = Executors.newScheduledSingleThread()
    try {
      b.declareExchange(ExampleServiceList.info.subExchange) //normally a service would do this in bindService
      val conn = new DefaultConnection(b, executor, 5000)
      conn.addServiceInfo(ExampleServiceList.info)
      fun(conn.login("foo"), conn)
    } finally {
      b.disconnect()
      executor.terminate()
    }
  }

  test("Subscription sequence works if event occurs before start()") {
    fixture { (client, conn) =>
      val si = SomeInteger(42)
      val sub = client.subscribe(SomeIntegerTypeDescriptor).await.get // gets us an unbound queue, doesn't listen yet
      conn.bindQueueByClass(sub.id(), "#", classOf[SomeInteger]) //binds the queue to the correct exchange
      conn.publishEvent(Envelope.Event.ADDED, si, "foobar")
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.start(events.append)
      events shouldBecome Event(Envelope.Event.ADDED, si) within 5000
    }
  }

  test("Subscription sequence works if event occurs after start()") {
    fixture { (client, conn) =>
      val si = SomeInteger(42)
      val sub = client.subscribe(SomeIntegerTypeDescriptor).await.get // gets us an unbound queue, doesn't listen yet
      conn.bindQueueByClass(sub.id(), "#", classOf[SomeInteger]) //binds the queue to the correct exchange
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.start(events.append(_))
      conn.publishEvent(Envelope.Event.ADDED, si, "foobar")
      events shouldBecome Event(Envelope.Event.ADDED, si) within 5000
    }
  }

}