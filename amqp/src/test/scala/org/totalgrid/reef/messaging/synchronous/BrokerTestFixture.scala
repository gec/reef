package org.totalgrid.reef.messaging.synchronous

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

import org.totalgrid.reef.broker.newapi._
import org.totalgrid.reef.broker.api.BrokerConnectionInfo
import org.totalgrid.reef.broker.newqpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.broker.memory.MemoryBrokerConnectionFactory
import net.agileautomata.executor4s.Executors

trait BrokerTestFixture {

  protected def getFactory: (BrokerConnectionFactory, () => Unit)

  def broker[A](test: BrokerConnection => A): A = {
    val (factory, cleanup) = getFactory
    val conn = factory.connect
    try {
      test(conn)
    } finally {
      conn.disconnect()
      cleanup()
    }
  }

}

trait QpidBrokerTestFixture extends BrokerTestFixture {
  lazy val config = BrokerConnectionInfo.loadInfo("test")
  def getFactory = (new QpidBrokerConnectionFactory(config), () => {})
}

trait MemoryBrokerTestFixture extends BrokerTestFixture {

  def getFactory = {
    val exe = Executors.newScheduledSingleThread()
    (new MemoryBrokerConnectionFactory(exe), () => exe.shutdown())
  }

}
