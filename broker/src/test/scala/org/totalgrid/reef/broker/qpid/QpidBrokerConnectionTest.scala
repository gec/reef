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
package org.totalgrid.reef.broker.qpid

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.broker._
import net.agileautomata.commons.testing._
import org.totalgrid.reef.clientapi.settings.AmqpSettings
import org.totalgrid.reef.clientapi.settings.util.PropertyReader
import org.totalgrid.reef.clientapi.exceptions.ServiceIOException

@RunWith(classOf[JUnitRunner])
class QpidBrokerConnectionTest extends FunSuite with ShouldMatchers {

  val defaults = new AmqpSettings(PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg"));
  val defaultTimeout = 5000
  def fixture(config: AmqpSettings)(test: BrokerConnection => Unit): Unit = {
    val factory = new QpidBrokerConnectionFactory(config)
    val conn = factory.connect
    try {
      test(conn)
    } finally {
      conn.disconnect()
    }
  }

  test("Qpid disconnect events") {
    fixture(defaults) { conn =>
      var list = List.empty[Boolean]
      val listener = new BrokerConnectionListener {
        def onDisconnect(expected: Boolean) = list ::= expected
      }
      conn.addListener(listener)
      conn.disconnect()
      list should equal(List(true))
    }
  }

  test("Connection Timeout") {
    val config = new AmqpSettings("127.0.0.1", 10000, "", "", "", 30)
    intercept[ServiceIOException](fixture(config) { conn => })
  }

  test("Bad Ssl configuration") {
    val config = new AmqpSettings(defaults.getHost, defaults.getPort, defaults.getUser, defaults.getPassword, defaults.getVirtualHost, 30, true, "badFileName", "")
    val ex = intercept[ServiceIOException](fixture(config) { conn => })
    ex.getMessage should include("badFileName")
  }

  test("Bad Ssl password") {

    val config = new AmqpSettings(defaults.getHost, defaults.getPort, defaults.getUser, defaults.getPassword, defaults.getVirtualHost, 30, true, "src/test/resources/trust-store.jks", "9090909")
    val ex = intercept[ServiceIOException](fixture(config) { conn => })
    ex.getMessage should include("SSL Context")
  }

  test("Valid Ssl configuration against non-ssl server") {

    val config = new AmqpSettings(defaults.getHost, defaults.getPort, defaults.getUser, defaults.getPassword, defaults.getVirtualHost, 30, true, "src/test/resources/trust-store.jks", "jjjjjjj")
    val ex = intercept[ServiceIOException](fixture(config) { conn => })
    ex.getMessage should include("SSLReceiver")
  }

  test("Qpid subscriptions work") {
    fixture(defaults) { broker =>

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
    fixture(defaults) { broker =>

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

  test("Qpid subscriptions throw correct exception on close") {
    fixture(defaults) { broker =>
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

  test("Qpid subscriptions are only closed once") {
    fixture(defaults) { broker =>
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = {}
      }
      val sub = broker.listen()

      sub.close()
    }
  }

  test("Throwing exception out of onMessage block") {
    fixture(defaults) { broker =>

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

}

