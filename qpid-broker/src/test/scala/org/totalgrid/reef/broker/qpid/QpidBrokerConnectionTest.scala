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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.broker._
import org.totalgrid.reef.client.settings.AmqpSettings
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.exception.ServiceIOException
import net.agileautomata.commons.testing.SynchronizedList

@RunWith(classOf[JUnitRunner])
class QpidBrokerConnectionTest extends BrokerConnectionTestBase {

  val defaults = new AmqpSettings(PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg"));

  def fixture(config: AmqpSettings)(test: BrokerConnection => Unit): Unit = {
    val factory = new QpidBrokerConnectionFactory(config)
    val conn = factory.connect
    try {
      test(conn)
    } finally {
      conn.disconnect()
    }
  }

  def testConnection(test: BrokerConnection => Unit) {
    fixture(defaults) { conn =>
      test(conn)
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

  test("Test Broker TTL") {

    // short TTL for testing (default is 5 seconds)
    val TTL = 100

    val config = new AmqpSettings(defaults.getHost, defaults.getPort, defaults.getUser, defaults.getPassword, defaults.getVirtualHost, 30, TTL, false, null, null, null, null)
    fixture(config) { broker =>

      val queue = broker.declareQueue("*", false, false)
      broker.declareExchange("test")
      broker.bindQueue(queue, "test", "hi", false)

      // publish some messages we expect to timeout
      broker.publish("test", "hi", "Old message should have expired".getBytes, None)
      broker.publish("test", "hi", "Other old message should have expired".getBytes, None)

      // sleep for double the TTL time to make sure qpid has had time to kill the messages
      Thread.sleep(TTL * 2)

      // publish some measurements that we will subscribe to before they expire
      val okMessages = List("Recent message should be delivered", "Other Recent message should be delivered")
      okMessages.foreach { msg =>
        broker.publish("test", "hi", msg.getBytes, None)
        Thread.sleep(TTL / 4)
      }

      val list = new SynchronizedList[String]
      val consumer = new BrokerMessageConsumer {
        def onMessage(msg: BrokerMessage) = list.append(new String(msg.bytes))
      }
      val sub = broker.listen(queue).start(consumer)

      // check that we only get the 2 recent message, not the older ones.
      list shouldBecome okMessages within (defaultTimeout)

      sub.close()
    }
  }
}

