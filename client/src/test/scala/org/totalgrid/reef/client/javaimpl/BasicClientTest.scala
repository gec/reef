package org.totalgrid.reef.client.javaimpl

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

import net.agileautomata.commons.testing._
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client._
import factory.ReefConnectionFactory
import org.totalgrid.reef.client.operations.scl.ScalaSubscription._
import org.totalgrid.reef.client.operations.scl.Event
import org.totalgrid.reef.client.javaimpl.fixture._
import net.agileautomata.executor4s._
import sapi.client.rest.impl.DefaultConnection
import SimpleRestAccess._

@RunWith(classOf[JUnitRunner])
class QpidServiceClientTest extends BasicClientTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryServiceClientTest extends BasicClientTest with MemoryBrokerTestFixture

// common base class with non-broker specific tests
trait BasicClientTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(fun: (Client, Connection) => Unit) {

    val (brokerFac, kill) = getFactory
    val exe = Executors.newResizingThreadPool(5.minutes)

    try {
      val brokerConn = brokerFac.connect
      brokerConn.declareExchange(ExampleServiceList.info.getEventExchange)
      val conn = new ConnectionWrapper(new DefaultConnection(brokerConn, exe, 5000), exe)
      conn.addServicesList(ExampleServiceList)
      fun(conn.createClient("foo"), conn)
    } finally {
      exe.terminate()
      kill()
    }
  }

  test("Subscription sequence works if event occurs before start()") {
    fixture { (client, conn) =>
      val eventPub = conn.getServiceRegistration.getEventPublisher

      val si = SomeInteger(42)
      val sub = client.subscribe(SomeIntegerTypeDescriptor) // gets us an unbound queue, doesn't listen yet
      eventPub.bindQueueByClass(sub.getId, "#", classOf[SomeInteger])
      eventPub.publishEvent(Envelope.SubscriptionEventType.ADDED, si, "foobar")
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.onEvent(events.append(_))
      events shouldBecome Event(Envelope.SubscriptionEventType.ADDED, si) within 5000
    }
  }

  test("Subscription sequence works if event occurs after start()") {
    fixture { (client, conn) =>
      val eventPub = conn.getServiceRegistration.getEventPublisher

      val si = SomeInteger(42)
      val sub = client.subscribe(SomeIntegerTypeDescriptor) // gets us an unbound queue, doesn't listen yet
      eventPub.bindQueueByClass(sub.getId, "#", classOf[SomeInteger])
      val events = new SynchronizedList[Event[SomeInteger]]
      sub.onEvent(events.append(_))
      eventPub.publishEvent(Envelope.SubscriptionEventType.ADDED, si, "foobar")
      events shouldBecome Event(Envelope.SubscriptionEventType.ADDED, si) within 5000
    }
  }

}