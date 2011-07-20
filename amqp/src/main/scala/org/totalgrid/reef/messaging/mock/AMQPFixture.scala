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
package org.totalgrid.reef.messaging.mock

import org.totalgrid.reef.broker._
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.broker.mock.MockBrokerConnection

import org.totalgrid.reef.executor.ReactActorExecutor
import org.totalgrid.reef.messaging._

import sync.AMQPSyncFactory

object AMQPFixture {

  val default = BrokerConnectionInfo.loadInfo("test")

  def run(config: BrokerConnectionInfo = default, requireConn: Boolean = false)(test: AMQPProtoFactory => Unit): Unit = {
    using(new QpidBrokerConnection(config), requireConn)(test)
  }

  def mock(requireConn: Boolean = false)(test: AMQPProtoFactory => Unit): Unit = {
    using(new MockBrokerConnection, requireConn)(test)
  }

  def using(connection: BrokerConnection, requireConnection: Boolean = false)(test: AMQPProtoFactory => Unit): Unit = {

    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = connection
    }

    try {
      if (requireConnection) amqp.connect(10000) else amqp.start()
      test(amqp)
    } finally {
      amqp.disconnect(10000)
    }
  }

  def sync(test: AMQPSyncFactory => Unit): Unit = {
    sync(default, true) { test }
  }

  def sync(config: BrokerConnectionInfo, requireConn: Boolean)(test: AMQPSyncFactory => Unit): Unit = {
    sync(new QpidBrokerConnection(config), requireConn) { test }
  }

  def mockSync(test: AMQPSyncFactory => Unit): Unit = {
    sync(new MockBrokerConnection, true) { test }
  }

  def sync(connection: BrokerConnection, requireConnection: Boolean)(test: AMQPSyncFactory => Unit): Unit = {
    val amqp = new AMQPSyncFactory with ReactActorExecutor {
      val broker = connection
    }

    try {
      if (requireConnection) amqp.connect(10000) else amqp.start()
      test(amqp)
    } finally {
      amqp.disconnect(10000)
    }
  }

}

