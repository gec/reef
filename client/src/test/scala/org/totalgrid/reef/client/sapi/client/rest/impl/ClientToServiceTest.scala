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
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.{ AddressableDestination, SubscriptionCreationListener, SubscriptionBinding, AnyNodeDestination }
import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Promise, SuccessResponse, Response }
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.sapi.client.rest.fixture._

@RunWith(classOf[JUnitRunner])
class QpidClientToService extends ClientToServiceTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryClientToService extends ClientToServiceTest with MemoryBrokerTestFixture

// provides a specification for how the client should interact with brokers. testable on multiple brokers via minx
trait ClientToServiceTest extends BrokerTestFixture with FunSuite with ShouldMatchers {

  def fixture[A](attachService: Boolean)(fun: Client => A) = broker { b =>
    val executor = Executors.newResizingThreadPool(5.minutes)
    var binding: Option[SubscriptionBinding] = None
    try {
      val conn = new DefaultConnection(b, executor, if (attachService) 1000 else 100)
      conn.addServiceInfo(ExampleServiceList.info)
      binding = if (attachService) Some(conn.bindService(new SomeIntegerIncrementService(conn), executor, new AnyNodeDestination, true))
      else Some(conn.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), executor, new AnyNodeDestination, true))
      fun(conn.login("foo"))
    } finally {
      binding.foreach(_.cancel())
      executor.terminate()
    }
  }

  def testSuccess(c: Client) {
    val i = SomeInteger(1)
    c.put(i).await should equal(Response(Envelope.Status.OK, i.increment))
  }

  test("Service calls are successful") {
    fixture(true) { c =>
      testSuccess(c)
    }
  }

  test("Service calls can be listened for") {
    fixture(true) { c =>
      val i = SomeInteger(1)
      val future = c.put(i)
      val events = new SynchronizedList[SomeInteger]
      future.listen { result =>
        result should equal(Response(Envelope.Status.OK, i.increment))
        events.append(result.list.head)
      }
      events shouldBecome (i.increment) within (100)
    }
  }

  def testPromiseAwait(c: Client) {
    val i = SomeInteger(1)
    val promise = Promise.from(c.put(i).map { _.one })
    val events = new SynchronizedList[SomeInteger]
    promise.listen { prom =>
      prom.await should equal(i.increment)
      events.append(prom.await)
    }
    events shouldBecome (i.increment) within (100)
  }

  test("Service calls can be listened for (promise)") {
    fixture(true) { c =>
      testPromiseAwait(c)
    }
  }

  test("Service calls promises can be listened for (inside strand)") {
    fixture(true) { c =>
      c.attempt {
        testPromiseAwait(c)
      }.await
    }
  }

  test("Subscription calls work") {
    fixture(true) { c =>
      val events = new SynchronizedList[SomeInteger]
      val bindings = new SynchronizedList[SubscriptionBinding]
      c.addSubscriptionCreationListener(new SubscriptionCreationListener {
        def onSubscriptionCreated(binding: SubscriptionBinding) { bindings.append(binding) }
      })
      val sub = c.subscribe(SomeIntegerTypeDescriptor)
      val headers = BasicRequestHeaders.empty.setSubscribeQueue(sub.getId)
      c.put(SomeInteger(1), headers).await should equal(SuccessResponse(list = List(SomeInteger(2))))
      sub.start(e => events.append(e.value))
      events shouldBecome SomeInteger(2) within 5000
      sub.cancel()

      bindings.get.size should equal(1)
    }
  }

  test("Service bindings work") {
    fixture(true) { c =>
      val bindings = new SynchronizedList[SubscriptionBinding]
      c.addSubscriptionCreationListener(new SubscriptionCreationListener {
        def onSubscriptionCreated(binding: SubscriptionBinding) { bindings.append(binding) }
      })
      val sub = c.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), c, new AnyNodeDestination, true)
      sub.cancel()

      bindings.get.size should equal(1)
    }
  }

  test("Events come in right order") {
    fixture(true) { c =>
      val events = new SynchronizedList[Int]
      val sub = c.subscribe(SomeIntegerTypeDescriptor)
      c.bindQueueByClass(sub.getId(), "#", classOf[SomeInteger])
      sub.start(e => events.append(e.value.num))

      val range = 0 to 1500

      range.foreach { i => c.publishEvent(Envelope.SubscriptionEventType.MODIFIED, SomeInteger(i), "key") }

      events shouldBecome range.toList within 5000
      sub.cancel()
    }
  }

  def testTimeout(c: Client) {
    val i = SomeInteger(1)
    c.put(i).await.status should equal(Envelope.Status.RESPONSE_TIMEOUT)
  }

  test("Failures timeout sucessfully") {
    fixture(false) { c =>
      testTimeout(c)
    }
  }

  def testFlatmapSuccess(c: Client) {
    val i = SomeInteger(1)
    // this test simulates an API call where we do 2 steps
    val f1: Future[Response[SomeInteger]] = c.put(i)
    val f2 = f1.flatMap {
      _.one match {
        case Success(int) => c.put(int)
        case fail: Failure => f1.asInstanceOf[Future[Response[SomeInteger]]]
      }
    }
    f2.flatMap {
      _.one match {
        case Success(int) => f1.replicate[Response[Double]](Response(Envelope.Status.OK, int.num * 88.88))
        case fail: Failure => f1.asInstanceOf[Future[Response[Double]]]
      }
    }.await should equal(Response(Envelope.Status.OK, 88.88 * 3))
  }

  test("Flatmapped service calls are successful") {
    fixture(true) { c =>
      testFlatmapSuccess(c)
    }
  }

  // below show that these operations will succeed when run inside the strand (like on a subscription
  // callback)

  test("Service calls are successful (inside strand)") {
    fixture(true) { c =>
      c.attempt {
        testSuccess(c)
      }.await
    }
  }

  test("Failures timeout sucessfully (inside strand)") {
    fixture(false) { c =>
      c.attempt {
        testTimeout(c)
      }.await
    }
  }

  test("Flatmap services calls are successfull (inside strand)") {
    // currently deadlocks because flatMap is implemented with a listen call that gets marshalled
    // to this same strand and therefore can never be run (since we are inside that thread already)
    fixture(true) { c =>
      c.attempt {
        testFlatmapSuccess(c)
      }.await
    }
  }

  test("Services can be late bound using queue names") {
    fixture(false) { c =>

      val address = new AddressableDestination("magic-key")

      val sub = c.lateBindService(new SomeIntegerIncrementService(c), c)

      c.bindServiceQueue(sub.getId, address.getKey, classOf[SomeInteger])
      val request = c.put(SomeInteger(1), BasicRequestHeaders.empty.setDestination(address))
      request.await should equal(SuccessResponse(list = List(SomeInteger(2))))
    }
  }
}