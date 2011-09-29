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

import org.totalgrid.reef.util.Conversion.convertIntToDecoratedInt
import scala.actors.Actor._
import org.totalgrid.reef.japi.Envelope.Verb
import org.totalgrid.reef.sapi.{ AnyNodeDestination, Routable, RequestEnv }
import org.totalgrid.reef.promise.{ FixedPromise, Promise }
import org.totalgrid.reef.sapi.client._
import org.totalgrid.reef.japi.{ ServiceIOException, ReefServiceException, Envelope }

@RunWith(classOf[JUnitRunner])
class BasicSessionPoolTest extends FunSuite with ShouldMatchers {

  class EchoingClientSession extends ClientSession {

    var numRequests = 0
    private var open = true

    final override def isOpen = open
    final override def close() = open = false

    def addSubscription[A](klass: Class[_]): Subscription[A] = throw new ServiceIOException("Unimplemented")

    final override def request[A](verb: Verb, payload: A, env: RequestEnv = getDefaultHeaders, destination: Routable = AnyNodeDestination): Promise[Response[A]] = {
      numRequests += 1
      new FixedPromise(SingleSuccess(single = payload))
    }

  }

  class MockSessionSource(throws: Boolean = false) extends SessionSource {

    val sessions = scala.collection.mutable.Set.empty[EchoingClientSession]

    def newSession(): ClientSession = {
      if (throws) throw new ServiceIOException("Test exception")
      val s = new EchoingClientSession
      sessions += s
      s
    }
  }

  def fixture(test: (MockSessionSource, SessionPool) => Unit): Unit = fixture(false)(test)

  def fixture(throws: Boolean)(test: (MockSessionSource, SessionPool) => Unit): Unit = {
    val mock = new MockSessionSource(throws)
    val pool = new BasicSessionPool(mock)
    test(mock, pool)
  }

  test("Starts with zero size") {
    fixture { (source, pool) =>
      pool.size should equal(0)
    }
  }

  test("Pool reuses sessions") {
    fixture { (source, pool) =>
      3.times(pool.borrow(s => s))
      source.sessions.size should equal(1)
      pool.size should equal(1)
    }
  }

  test("Recursive borrowing acquires new sessions") {

    def borrow(pool: SessionPool, num: Int): Unit =
      if (num > 0) pool.borrow(s => borrow(pool, num - 1))

    val num = 100

    fixture { (source, pool) =>
      borrow(pool, num)
      pool.size should equal(num)
      source.sessions.size should equal(num)
    }
  }

  test("Closed sessions are removed") {
    fixture { (source, pool) =>
      pool.borrow(_.close())
      source.sessions.size should equal(1)
      pool.size should equal(0)
    }
  }

  test("Exception while borrowing yields a special session that errors") {
    fixture(true) { (source, pool) =>
      pool.borrow { s =>
        s.get(4).await().status should equal(Envelope.Status.BUS_UNAVAILABLE)
      }
      source.sessions.size should equal(0)
      pool.size should equal(0)
    }
  }

  test("Handles borrowing from multiple threads") {
    fixture { (source, pool) =>
      case object Borrow

      def newActor = actor {
        react {
          case Borrow => reply(pool.borrow(s => s.get(4).await))
        }
      }

      // use futures to start all operations and then block for completion
      val replys = (1 to 100).map(i => newActor).map(a => a !! Borrow).map(f => f.apply())

      // Some aspects of the test are non-deterministic, but there are some reasonable assertions we can make
      source.sessions.size should (be > 0 and be <= 100)

      // calculate the total number of operations performed on all of the EchoSession
      val totalops = source.sessions.foldLeft(0)((sum, s) => sum + s.numRequests)

      totalops should equal(100)
    }
  }

}