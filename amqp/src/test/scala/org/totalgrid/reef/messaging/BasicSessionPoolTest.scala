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

import mock.MockClientSession
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.util.Conversion.convertIntToTimes
import org.totalgrid.reef.sapi.client.{Failure, SessionPool, ClientSession}
import org.totalgrid.reef.japi.{Envelope, ServiceIOException}


@RunWith(classOf[JUnitRunner])
class BasicSessionPoolTest extends FunSuite with ShouldMatchers {

  class MockSessionSource(throws: Boolean = false) extends SessionSource {

    val sessions = scala.collection.mutable.Set.empty[MockClientSession]

    def newSession(): ClientSession = {
      if(throws) throw new ServiceIOException("Test exception")
      val s = new MockClientSession
      sessions += s
      s
    }
  }

  def fixture(test : (MockSessionSource, SessionPool) => Unit) : Unit = fixture(false)(test)

  def fixture(throws: Boolean)(test : (MockSessionSource, SessionPool) => Unit) : Unit = {
    val mock = new MockSessionSource(throws)
    val pool = new BasicSessionPool(mock)
    test(mock, pool)
  }

  test("StartsWithZeroSize") {
    fixture { (source, pool) =>
      pool.size should equal(0)
    }
  }

  test("PoolReusesSessions") {
    fixture { (source, pool) =>
      3.times(pool.borrow(s => s))
      source.sessions.size should equal(1)
      pool.size should equal(1)
    }
  }

  test("RecursiveBorrowAcquiresNewSessions") {

    def borrow(pool: SessionPool, num: Int) : Unit =
      if(num > 0) pool.borrow(s => borrow(pool, num - 1))

    val num = 100

    fixture { (source, pool) =>
      borrow(pool, num)
      pool.size should equal(num)
      source.sessions.size should equal(num)
    }
  }

  test("ClosedSessionsAreRemoved") {
    fixture { (source, pool) =>
      pool.borrow(_.close())
      source.sessions.size should equal(1)
      pool.size should equal(0)
    }
  }

  test("ExceptionWhileBorrowingYieldsSpecialSession") {
    fixture(true) { (source, pool) =>
      pool.borrow { s =>
        s.get(4).await().status should equal(Envelope.Status.BUS_UNAVAILABLE)
      }
      source.sessions.size should equal(0)
      pool.size should equal(0)
    }
  }

}