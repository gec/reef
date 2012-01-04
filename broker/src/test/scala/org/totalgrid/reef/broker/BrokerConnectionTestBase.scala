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
package org.totalgrid.reef.broker

import org.totalgrid.reef.client.exception.ServiceIOException

import net.agileautomata.commons.testing._

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

abstract class BrokerConnectionTestBase extends FunSuite with ShouldMatchers {

  /**
   * should be implemented by a broker test suite package
   */
  def testConnection(test: BrokerConnection => Unit)

  val defaultTimeout = 5000

  test("Disconnect events are fired") {
    testConnection { conn =>
      var list = List.empty[Boolean]
      val listener = new BrokerConnectionListener {
        def onDisconnect(expected: Boolean) = list ::= expected
      }
      conn.addListener(listener)
      conn.disconnect()
      list should equal(List(true))
    }
  }

  test("Subscriptions work") {
    testConnection { broker =>

      val list = new SynchronizedList[Int]
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = list.append(msg.bytes.length)
      }
      val sub = broker.listen().start(consumer)

      // bind the queue to the test exchange and send it a message
      broker.declareExchange("test")
      broker.bindQueue(sub.getQueue, "test", "hi", false)
      broker.publish("test", "hi", "hello".getBytes, None)
      broker.publish("test", "hi", "friend".getBytes, None)

      list shouldBecome List(5, 6) within (defaultTimeout)
    }
  }

  test("Messages arrive in order") {
    testConnection { broker =>

      val list = new SynchronizedList[Int]
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = list.append(new String(msg.bytes).toInt)
      }
      val sub = broker.listen().start(consumer)

      // bind the queue to the test exchange and send it a message
      broker.declareExchange("test2")
      broker.bindQueue(sub.getQueue, "test2", "hi", false)

      val range = 0 to 1000

      range.foreach { i => broker.publish("test2", "hi", i.toString.getBytes, None) }

      list shouldBecome range.toList within (defaultTimeout)
    }
  }

  test("Subscriptions throw correct exception on close") {
    testConnection { broker =>
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = {}
      }
      val sub = broker.listen()

      broker.disconnect()

      intercept[ServiceIOException] {
        sub.start(consumer)
      }
    }
  }

  test("Subscriptions are only closed once") {
    testConnection { broker =>
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = {}
      }
      val sub = broker.listen()

      sub.close()
    }
  }

  test("Throwing exception out of onMessage block") {
    testConnection { broker =>

      var explode = false
      val list = new SynchronizedList[Int]
      val exceptions = new SynchronizedList[Int]
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = {
          if (explode) {
            exceptions.append(msg.bytes.length)
            throw new IllegalArgumentException
          }
          list.append(msg.bytes.length)
        }
      }
      val sub = broker.listen().start(consumer)

      broker.declareExchange("test")
      broker.bindQueue(sub.getQueue, "test", "hi", false)
      broker.publish("test", "hi", "hello".getBytes, None)
      list shouldBecome List(5) within (defaultTimeout)
      explode = true
      broker.publish("test", "hi", "hellohello".getBytes, None)
      exceptions shouldBecome List(10) within (defaultTimeout)
      explode = false
      broker.publish("test", "hi", "friend".getBytes, None)

      list shouldBecome List(5, 6) within (defaultTimeout)
    }
  }

  test("Routing keys work as expected") {
    testConnection { broker =>

      val list1 = new SynchronizedList[String]
      val consumer1 = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = list1.append(new String(msg.bytes))
      }
      val list2 = new SynchronizedList[String]
      val consumer2 = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = list2.append(new String(msg.bytes))
      }
      val sub1 = broker.listen().start(consumer1)
      val sub2 = broker.listen().start(consumer2)

      // bind the queue to the test exchange and send it a message
      broker.declareExchange("test3")
      broker.bindQueue(sub1.getQueue, "test3", "key1", false)
      broker.bindQueue(sub2.getQueue, "test3", "key2", false)

      broker.publish("test3", "badKey", "key1Msg".getBytes, None)
      broker.publish("test3", "key1", "key1Msg".getBytes, None)
      broker.publish("test3", "key2", "key2Msg".getBytes, None)

      list1 shouldBecome (List("key1Msg")) within (defaultTimeout)
      list2 shouldBecome (List("key2Msg")) within (defaultTimeout)

    }
  }

  class MockConsumer extends BrokerMessageConsumer {
    val messages = new SynchronizedList[List[Byte]]
    def onMessage(msg: BrokerMessage) = messages.append(msg.bytes.toList)
  }

  val r = new java.util.Random
  def randomBytes(count: Int) = {
    val arr = new Array[Byte](count)
    r.nextBytes(arr)
    arr
  }

  val testBytes: List[Byte] = randomBytes(100).toList

  test("Queue rotates consumers") {
    testConnection { conn =>
      val queue = conn.declareQueue(exclusive = false)
      conn.declareExchange("ex1")
      conn.bindQueue(queue, "ex1", "#")
      val mc1 = new MockConsumer
      val mc2 = new MockConsumer
      conn.listen(queue).start(mc1)
      conn.listen(queue).start(mc2)
      2.times(conn.publish("ex1", "foobar", testBytes.toArray, None))

      mc1.messages shouldBecome testBytes within 5000
      mc2.messages shouldBecome testBytes within 5000
    }
  }

  test("Exchange replicates messages") {
    testConnection { conn =>
      val mc1 = new MockConsumer
      val mc2 = new MockConsumer
      val sub1 = conn.listen().start(mc1)
      val sub2 = conn.listen().start(mc2)
      conn.declareExchange("ex1")
      conn.bindQueue(sub1.getQueue, "ex1", "#")
      conn.bindQueue(sub2.getQueue, "ex1", "#")
      conn.publish("ex1", "foobar", testBytes.toArray, None)

      mc1.messages shouldBecome testBytes within 5000
      mc2.messages shouldBecome testBytes within 5000
    }
  }

  test("All messages are received when connection is used concurrently") {
    testConnection { conn =>
      val count = new SynchronizedVariable[Int](0)
      val mc = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = count.modify(_ + 1)
      }
      conn.declareExchange("ex")
      val sub = conn.listen().start(mc)
      conn.bindQueue(sub.getQueue, "ex", "#")
      val bytes = 100.create(randomBytes(1))
      bytes.foreach(arr => onAnotherThread(conn.publish("ex", "foo", arr, None)))

      count shouldBecome 100 within 5000
      count shouldRemain 100 during 500
    }
  }
}