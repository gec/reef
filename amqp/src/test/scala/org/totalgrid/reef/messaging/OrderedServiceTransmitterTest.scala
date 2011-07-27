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
package org.totalgrid.reef.messaging

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.sapi.{ Destination, RequestEnv }
import org.totalgrid.reef.japi.{ Envelope, ServiceIOException }
import org.totalgrid.reef.sapi.client._
import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt
import org.totalgrid.reef.promise.{ SynchronizedPromise, Promise }
import scala.actors.Actor._

@RunWith(classOf[JUnitRunner])
class OrderedServiceTransmitterTest extends FunSuite with ShouldMatchers {

  class QueuedResponseClientSession extends ClientSession {

    var numRequests = 0

    private var open = true
    private var autoRespond = true

    final override def isOpen = open
    final override def close() = open = false

    final def setAutoRespond(b: Boolean) = autoRespond = b

    private val queue = new scala.collection.mutable.Queue[Envelope.Status]
    private val pending = new scala.collection.mutable.Queue[() => Unit]

    def queueResponse(num: Int, status: Envelope.Status) = num.times(queue.enqueue(status))
    def queueSuccess(num: Int = 1) = queueResponse(num, Envelope.Status.OK)
    def queueFailure(num: Int = 1) = queueResponse(num, Envelope.Status.BUS_UNAVAILABLE)

    def addSubscription[A](klass: Class[_]): Subscription[A] = throw new ServiceIOException("Unimplemented")

    final override def request[A](verb: Verb, payload: A, env: RequestEnv, destination: Destination): Promise[Response[A]] = queue.synchronized {
      numRequests += 1
      val rsp = if (queue.size > 0) Response(queue.dequeue(), payload) else Failure()
      val promise = new SynchronizedPromise[Response[A]]
      if (autoRespond) {
        actor { promise.onResponse(rsp) }
      } else {
        pending.enqueue(() => { promise.onResponse(rsp) })
      }
      promise
    }

    def respondOne(): Int = queue.synchronized {
      if (!pending.isEmpty) {
        pending.dequeue()()
      }
      pending.size
    }

  }

  def fixture(test: (QueuedResponseClientSession, OrderedServiceTransmitter) => Unit) = {
    val session = new QueuedResponseClientSession
    val pool = new org.totalgrid.reef.messaging.mock.MockSessionPool(session)
    val ost = new OrderedServiceTransmitter(pool)
    test(session, ost)
  }

  test("Success on first attempt") {
    fixture { (session, ost) =>
      session.queueSuccess(1)
      ost.publish(99).await should equal(true)
      session.numRequests should equal(1)
    }
  }

  test("Retry until failure") {
    fixture { (session, ost) =>

      ost.publish(99).await should equal(false)
      session.numRequests should equal(1)

      session.numRequests = 0
      ost.publish(99, maxRetries = 3).await should equal(false)
      session.numRequests should equal(4)
    }
  }

  test("Retry until success") {
    fixture { (session, ost) =>

      session.queueFailure(5)
      session.queueSuccess(1)
      ost.publish(99, maxRetries = 5).await should equal(true)
      session.numRequests should equal(6)
    }
  }

  test("Threading stress test") {
    val count = 10000
    fixture { (session, ost) =>
      session.queueSuccess(count)
      val results = (1 to count).map(i => ost.publish(99)).map(f => f.await)
      results.find(_ == false) should equal(None)
      session.numRequests should equal(count)
    }
  }

  test("Flush is same as await all") {
    val count = 10000
    fixture { (session, ost) =>
      session.queueSuccess(count)
      val promises = (1 to count).map(i => ost.publish(99))
      ost.flush()
      promises.find(_.isComplete == false) should equal(None)
      val results = promises.map { _.await }
      results.find(_ == false) should equal(None)
      session.numRequests should equal(count)
    }
  }

  test("Instant failure if shutdown") {
    fixture { (session, ost) =>
      session.queueSuccess(1)
      ost.shutdown
      val promise = ost.publish(99)
      promise.isComplete should equal(true)
      promise.await should equal(false)

      session.numRequests should equal(0)
    }
  }

  test("Only one message in flight at time") {
    fixture { (session, ost) =>
      session.queueSuccess(2)
      // disable the instant repsponses, they are queued until we call respondOne
      session.setAutoRespond(false)

      // try to send 2 messages, first one will be sent, second will wait for first to complete
      val promise1 = ost.publish(99)
      val promise2 = ost.publish(98)

      // only one request has been sent, no responses have been sent
      session.numRequests should equal(1)
      promise1.isComplete should equal(false)
      promise2.isComplete should equal(false)

      // respond to first message, first request is satisfied, second request should be cleared to send
      session.respondOne()
      promise1.await should equal(true)
      promise2.isComplete should equal(false)
      session.numRequests should equal(2)

      // respond to second message
      session.respondOne()
      promise2.await should equal(true)
    }
  }
}
