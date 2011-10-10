package org.totalgrid.reef.broker.memory

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

import org.totalgrid.reef.broker.api._
import net.agileautomata.commons.testing._

@RunWith(classOf[JUnitRunner])
class MemoryBrokerConnectionTest extends FunSuite with ShouldMatchers {

  class MockConsumer extends MessageConsumer {
    val messages = new SynchronizedList[Array[Byte]]
    def receive(bytes: Array[Byte], replyTo: Option[Destination]) = messages.append(bytes)
  }

  val testBytes: Array[Byte] = Array(0x0A, 0x0B)

  def fixture(test: BrokerConnection => Unit) = {
    val conn = new MemoryBrokerConnection
    conn.connect() should equal(true)
    try {
      test(conn)
    } finally {
      conn.shutdown()
    }
  }

  test("Queue rotates consumers") {
    fixture { conn =>
      val queue = conn.declareQueue()
      conn.declareExchange("ex1")
      conn.bindQueue(queue, "ex1", "#")
      val mc1 = new MockConsumer
      val mc2 = new MockConsumer
      conn.listen(queue, mc1)
      conn.listen(queue, mc2)
      2.times(conn.publish("ex1", "foobar", testBytes, None))

      mc1.messages shouldBecome testBytes within 5000
      mc2.messages shouldBecome testBytes within 5000
    }
  }

  test("Exchange replicates messages") {
    fixture { conn =>
      val q1 = conn.declareQueue()
      val q2 = conn.declareQueue()
      conn.declareExchange("ex1")
      conn.bindQueue(q1, "ex1", "#")
      conn.bindQueue(q2, "ex1", "#")
      val mc1 = new MockConsumer
      val mc2 = new MockConsumer
      conn.listen(q1, mc1)
      conn.listen(q2, mc2)
      conn.publish("ex1", "foobar", testBytes, None)

      mc1.messages shouldBecome testBytes within 5000
      mc2.messages shouldBecome testBytes within 5000
    }
  }

}