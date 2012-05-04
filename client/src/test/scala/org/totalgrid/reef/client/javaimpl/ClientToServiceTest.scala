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
import org.totalgrid.reef.client.proto.Envelope
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.javaimpl.fixture._
import org.totalgrid.reef.client.factory.ReefConnectionFactory
import org.totalgrid.reef.client._
import operations.Response

import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client.operations.scl.ScalaSubscription._
import sapi.client.BasicRequestHeaders
import scala.collection.JavaConversions._
import types.TypeDescriptor

@RunWith(classOf[JUnitRunner])
class QpidClientToService extends ClientToServiceTest with QpidBrokerTestFixture

@RunWith(classOf[JUnitRunner])
class MemoryClientToService extends ClientToServiceTest with MemoryBrokerTestFixture

object SimpleRestAccess {
  class ClientWrap(c: Client) {
    def put[A](v: A): Promise[Response[A]] = c.getServiceOperations.operation("Failure doing put") { rest =>
      rest.put(v)
    }
    def put[A](v: A, hdrs: RequestHeaders): Promise[Response[A]] = c.getServiceOperations.operation("Failure doing put") { rest =>
      rest.put(v, hdrs)
    }
    def subscribe[A](desc: TypeDescriptor[A]): Subscription[A] = {
      c.getServiceOperations.getBindOperations.subscribe(desc)
    }

    def attempt[A](f: => A) = c.getInternal.getExecutor.attempt(f)
  }
  implicit def implClient(c: Client): ClientWrap = new ClientWrap(c)
}

// provides a specification for how the client should interact with brokers. testable on multiple brokers via minx
trait ClientToServiceTest extends BrokerTestFixture with FunSuite with ShouldMatchers {
  import SimpleRestAccess._

  def fixture[A](attachService: Boolean)(fun: (Client, Connection) => A) = {
    val (brokerFac, kill) = getFactory
    val exe = Executors.newResizingThreadPool(5.minutes)
    val fac = new ReefConnectionFactory(brokerFac, exe, ExampleServiceList)
    var binding: Option[SubscriptionBinding] = None
    try {
      val conn = fac.connect()
      binding = if (attachService) {
        val srv = new SomeIntegerIncrementService(conn.getServiceRegistration.getEventPublisher)
        Some(conn.getServiceRegistration.bindService(srv, srv.descriptor, new AnyNodeDestination, true))
      } else {
        val srv = new BlackHoleService(SomeIntegerTypeDescriptor)
        Some(conn.getServiceRegistration.bindService(srv, srv.descriptor, new AnyNodeDestination, true))
      }
      fun(conn.createClient("foo"), conn)
    } finally {
      binding.foreach(_.cancel())
      fac.terminate()
      exe.terminate()
      kill()
    }

  }

  def testSuccess(c: Client) {
    val i = SomeInteger(1)
    checkResp(c.put(i).await, Envelope.Status.OK, i.increment)
  }

  test("Service calls are successful") {
    fixture(true) { (c, _) =>
      testSuccess(c)
    }
  }

  def checkResp[A](resp: Response[A], status: Envelope.Status, v: A) {
    resp.isSuccess should equal(true)
    resp.getStatus should equal(status)
    resp.getList.get(0) should equal(v)
  }

  def checkResp[A](resp: Response[A], list: List[A]) {
    resp.isSuccess should equal(true)
    val respList = resp.getList.toList
    respList should equal(list)
  }

  test("Service calls can be listened for") {
    fixture(true) { (c, _) =>
      val i = SomeInteger(1)
      val future = c.put(i)
      val events = new SynchronizedList[SomeInteger]
      future.listenFor { prom =>
        val resp = prom.await()
        checkResp(resp, Envelope.Status.OK, i.increment)
        events.append(resp.getList.head)
      }
      events shouldBecome (i.increment) within (100)
    }
  }

  def testPromiseAwait(c: Client) {
    val i = SomeInteger(1)
    val promise = c.put(i).map(_.one)
    val events = new SynchronizedList[SomeInteger]
    promise.listenFor { prom =>
      prom.await should equal(i.increment)
      events.append(prom.await)
    }
    events shouldBecome (i.increment) within (100)
  }

  test("Service calls can be listened for (promise)") {
    fixture(true) { (c, _) => testPromiseAwait(c) }
  }

  test("Service calls promises can be listened for (inside strand)") {
    fixture(true) { (c, _) =>
      c.attempt {
        testPromiseAwait(c)
      }
    }
  }

  test("Subscription calls work") {
    fixture(true) { (c, _) =>
      val events = new SynchronizedList[SomeInteger]
      val bindings = new SynchronizedList[SubscriptionBinding]
      c.addSubscriptionCreationListener(new SubscriptionCreationListener {
        def onSubscriptionCreated(binding: SubscriptionBinding) {
          bindings.append(binding)
        }
      })
      val sub = c.subscribe(SomeIntegerTypeDescriptor)
      val headers = BasicRequestHeaders.empty.setSubscribeQueue(sub.getId)

      checkResp(c.put(SomeInteger(1), headers).await, List(SomeInteger(2)))

      sub.onEvent(e => events.append(e.value))
      events shouldBecome SomeInteger(2) within 5000
      sub.cancel()

      bindings.get.size should equal(1)
    }
  }

  /*test("Service bindings work") {
    fixture(true) { (c, conn) =>
      val bindings = new SynchronizedList[SubscriptionBinding]
      c.addSubscriptionCreationListener(new SubscriptionCreationListener {
        def onSubscriptionCreated(binding: SubscriptionBinding) {
          bindings.append(binding)
        }
      })
      val sub = conn.getServiceRegistration.bindService(new BlackHoleService(SomeIntegerTypeDescriptor), SomeIntegerTypeDescriptor, new AnyNodeDestination, true)
      sub.cancel()

      bindings.get.size should equal(1)
    }
  }*/

  test("Events come in right order") {
    fixture(true) { (c, conn) =>
      val reg = conn.getServiceRegistration
      val eventPub = conn.getServiceRegistration.getEventPublisher

      val events = new SynchronizedList[Int]
      val sub = c.subscribe(SomeIntegerTypeDescriptor)
      reg.bindServiceQueue(sub.getId(), "#", classOf[SomeInteger])
      sub.onEvent(e => events.append(e.value.num))

      val range = 0 to 1500

      range.foreach {
        i => eventPub.publishEvent(Envelope.SubscriptionEventType.MODIFIED, SomeInteger(i), "key")
      }

      events shouldBecome range.toList within 5000
      sub.cancel()
    }
  }

  def testTimeout(c: Client) {
    val i = SomeInteger(1)
    c.put(i).await.getStatus should equal(Envelope.Status.RESPONSE_TIMEOUT)
  }

  test("Failures timeout sucessfully") {
    fixture(false) { (c, _) =>
      testTimeout(c)
    }
  }
  /*
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
    fixture(true) {
      c =>
        testFlatmapSuccess(c)
    }
  }*/

  // below show that these operations will succeed when run inside the strand (like on a subscription
  // callback)

  test("Service calls are successful (inside strand)") {
    fixture(true) { (c, _) =>
      c.attempt {
        testSuccess(c)
      }.await
    }
  }

  test("Failures timeout sucessfully (inside strand)") {
    fixture(false) { (c, _) =>
      c.attempt {
        testTimeout(c)
      }.await
    }
  }

  /*test("Flatmap services calls are successfull (inside strand)") {
    // currently deadlocks because flatMap is implemented with a listen call that gets marshalled
    // to this same strand and therefore can never be run (since we are inside that thread already)
    fixture(true) { (c, _) =>
      c.attempt {
        testFlatmapSuccess(c)
      }.await
    }
  } */

  test("Services can be late bound using queue names") {
    fixture(false) { (c, conn) =>
      val reg = conn.getServiceRegistration
      val binding = c.getServiceOperations.getBindOperations
      val pub = conn.getServiceRegistration.getEventPublisher

      val address = new AddressableDestination("magic-key")

      val sub = binding.lateBindService(new SomeIntegerIncrementService(pub), SomeIntegerTypeDescriptor)

      reg.bindServiceQueue(sub.getId, address.getKey, classOf[SomeInteger])
      val request = c.put(SomeInteger(1), BasicRequestHeaders.empty.setDestination(address))
      checkResp(request.await, List(SomeInteger(2)))
    }
  }
}