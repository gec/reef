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
import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import org.totalgrid.reef.clientapi.proto.Envelope

import org.totalgrid.reef.clientapi.AnyNodeDestination

import org.totalgrid.reef.clientapi.sapi.client.{ Promise, SuccessResponse, Response }
import net.agileautomata.executor4s._
import org.totalgrid.reef.clientapi.sapi.client.rest.fixture._

@RunWith(classOf[JUnitRunner])
class QpidClientToService extends ConnectionToServiceTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryClientToService extends ConnectionToServiceTest with MemoryBrokerTestFixture

// provides a specification for how the client should interact with brokers. testable on multiple brokers via minx
trait ConnectionToServiceTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture(attachService: Boolean)(fun: Client => Unit) = broker { b =>
    val executor = Executors.newScheduledSingleThread()
    var binding: Option[Cancelable] = None
    try {
      val conn = new DefaultConnection(b, executor, 100)
      conn.addServiceInfo(ExampleServiceList.info)
      binding = if (attachService) Some(conn.bindService(new SomeIntegerIncrementService(conn), executor, new AnyNodeDestination, true))
      else Some(conn.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), executor, new AnyNodeDestination, true))
      fun(conn.login("foo"))
    } finally {
      binding.foreach(_.cancel())
      executor.terminate()
    }
  }

  test("Service calls are successful") {
    fixture(true) { c =>
      val i = SomeInteger(1)
      c.put(i).await should equal(Response(Envelope.Status.OK, i.increment))
    }
  }

  //  test("Service calls can be listened for") {
  //    fixture(true) { c =>
  //      val i = SomeInteger(1)
  //      val future = c.put(i)
  //      var listenFired = false
  //      future.listen { result =>
  //        result should equal(Response(Envelope.Status.OK, i.increment))
  //        listenFired = true
  //      }
  //      // await should force future.listen calls to have fired
  //      future.await
  //      listenFired should equal(true)
  //    }
  //  }
  //
  //  test("Service calls can be listened for (promise)") {
  //    fixture(true) { c =>
  //      val i = SomeInteger(1)
  //      val promise = Promise.from(c.put(i).map { _.one })
  //      var listenFired = false
  //      promise.listen { prom =>
  //        prom.await should equal(i.increment)
  //        listenFired = true
  //      }
  //      // await should force promise listens to have fired
  //      promise.await
  //      listenFired should equal(true)
  //    }
  //  }

  test("Subscription calls work") { //subscriptions not currently working with embedded broker
    fixture(true) { c =>
      val events = new SynchronizedList[SomeInteger]
      val sub = c.subscribe(SomeIntegerTypeDescriptor).await.get
      c.put(SomeInteger(1), sub).await should equal(SuccessResponse(list = List(SomeInteger(2))))
      sub.start(e => events.append(e.value))
      events shouldBecome SomeInteger(2) within 5000
      sub.cancel()
    }
  }

  test("Events come in right order") { //subscriptions not currently working with embedded broker
    fixture(true) { c =>
      val events = new SynchronizedList[Int]
      val sub = c.subscribe(SomeIntegerTypeDescriptor).await.get
      c.bindQueueByClass(sub.id(), "#", classOf[SomeInteger])
      sub.start(e => events.append(e.value.num))

      val range = 0 to 1500

      range.foreach { i => c.publishEvent(Envelope.SubscriptionEventType.MODIFIED, SomeInteger(i), "key") }

      events shouldBecome range.toList within 5000
      sub.cancel()
    }
  }

  test("Failures timeout sucessfully") {
    fixture(false) { c =>
      val i = SomeInteger(1)
      c.put(i).await.status should equal(Envelope.Status.RESPONSE_TIMEOUT)
    }
  }
}