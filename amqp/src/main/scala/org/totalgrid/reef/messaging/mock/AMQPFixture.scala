/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.messaging.mock

import scala.actors.TIMEOUT

import java.io.IOException
import scala.concurrent.MailBox

import org.totalgrid.reef.messaging._
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection

import org.totalgrid.reef.reactor.ReactActor
import sync.AMQPSyncFactory

object AMQPFixture {

  val default = BrokerConnectionInfo.loadInfo("test")

  //define a mock for the session handler inline
  class MockHandler extends ChannelObserver {
    val b = new MailBox

    override def online(bi: BrokerChannel) {
      b send "connected"
    }

    override def offline() {}

    def waitForConnect(waitms: Long): Boolean = {
      b.receiveWithin(waitms) {
        case "connected" => true
        case _ => false
      }
    }
  }

  def run(test: AMQPProtoFactory => Unit): Unit = {
    run(default, false) { test }
  }

  def run(requireConn: Boolean)(test: AMQPProtoFactory => Unit): Unit = {
    run(default, requireConn) { test }
  }

  def run(config: BrokerConnectionInfo, requireConn: Boolean)(test: AMQPProtoFactory => Unit): Unit = {
    run(new QpidBrokerConnection(config), requireConn) { test }
  }

  def mock(requireConn: Boolean)(test: AMQPProtoFactory => Unit): Unit = {
    run(new MockBrokerInterface, requireConn) { test }
  }

  def run(connection: BrokerConnection, requireConnection: Boolean)(test: AMQPProtoFactory => Unit): Unit = {

    val amqp = new AMQPProtoFactory with ReactActor {
      val broker = connection
    }

    amqp.start

    try {
      if (requireConnection) {
        val mock = new MockHandler
        amqp.add(mock)
        assert(mock.waitForConnect(10000))
      }
      test(amqp)
    } finally {
      amqp.stop
    }
  }

  def sync(test: AMQPSyncFactory => Unit): Unit = {
    sync(default, true) { test }
  }

  def sync(config: BrokerConnectionInfo, requireConn: Boolean)(test: AMQPSyncFactory => Unit): Unit = {
    sync(new QpidBrokerConnection(config), requireConn) { test }
  }

  def mockSync(test: AMQPSyncFactory => Unit): Unit = {
    sync(new MockBrokerInterface, true) { test }
  }

  def sync(connection: BrokerConnection, requireConnection: Boolean)(test: AMQPSyncFactory => Unit): Unit = {
    val amqp = new AMQPSyncFactory with ReactActor {
      val broker = connection
    }

    amqp.start

    try {
      if (requireConnection) {
        val mock = new MockHandler
        amqp.add(mock)
        assert(mock.waitForConnect(10000))
      }
      test(amqp)
    } finally {
      amqp.stop
    }
  }

}

