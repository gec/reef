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
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection

import org.totalgrid.reef.broker.memory.MemoryBrokerConnection
import org.totalgrid.reef.broker.api._

trait BrokerTestFixture {

  protected def getBroker: BrokerConnection

  def broker[A](test: BrokerConnection => A): A = {
    val broker = getBroker
    assert(broker.connect())
    try {
      test(broker)
    } finally {
      broker.disconnect()
    }
  }

}

trait QpidBrokerTestFixture extends BrokerTestFixture {
  lazy val config = BrokerConnectionInfo.loadInfo("test")
  def getBroker = new QpidBrokerConnection(config)
}

trait MemoryBrokerTestFixture extends BrokerTestFixture {
  def getBroker = new MemoryBrokerConnection
}
