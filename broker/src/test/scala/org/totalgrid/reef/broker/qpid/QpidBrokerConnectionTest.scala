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
import org.totalgrid.reef.api.japi.settings.AmqpSettings
import org.totalgrid.reef.api.japi.settings.util.PropertyReader

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
      val list = new SynchronizedList[Boolean]
      val listener = new BrokerConnectionListener {
        def onDisconnect(expected: Boolean) = list.append(expected)
      }
      conn.addListener(listener)
      conn.disconnect()
      list shouldEqual (true) within (defaultTimeout)
    }
  }

  test("Connection Timeout") {
    val config = new AmqpSettings("127.0.0.1", 10000, "", "", "")
    intercept[Exception](fixture(config) { conn => })
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

      list shouldEqual (6, 5) within (defaultTimeout)
    }
  }

}

