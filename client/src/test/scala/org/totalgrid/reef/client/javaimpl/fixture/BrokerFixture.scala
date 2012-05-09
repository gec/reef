package org.totalgrid.reef.client.javaimpl.fixture

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

import net.agileautomata.executor4s.Executors

import org.totalgrid.reef.broker.BrokerConnectionFactory
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.broker.memory.MemoryBrokerConnectionFactory
import org.totalgrid.reef.client.settings.AmqpSettings
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.impl.ConnectionImpl

trait BrokerFixture {
  def factory: BrokerConnectionFactory

  def cleanup() {}
}

class QpidBrokerFixture(config: AmqpSettings) extends BrokerFixture {
  private val fac = new QpidBrokerConnectionFactory(config)

  def factory = fac
}

class MemoryBrokerFixture extends BrokerFixture {
  private val exe = Executors.newScheduledThreadPool()
  private val fac = new MemoryBrokerConnectionFactory(exe)

  override def factory = fac

  override def cleanup() {
    exe.terminate()
  }
}

object ConnectionFixture {

  val qpidDefault = new AmqpSettings(PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg"))

  def qpid(config: AmqpSettings = qpidDefault)(test: ConnectionImpl => Unit): Unit = {
    using(new QpidBrokerFixture(config))(test)
  }

  def mock(test: ConnectionImpl => Unit) {
    using(new MemoryBrokerFixture)(test)
  }

  def using(fixture: BrokerFixture)(test: ConnectionImpl => Unit): Unit = {
    val exe = Executors.newScheduledThreadPool()
    try {
      val broker = fixture.factory.connect
      val conn = new ConnectionImpl(broker, exe, 5000)
      conn.getServiceRegistry.addServiceTypeInformation(ExampleServiceList.info)
      test(conn)
    } finally {
      exe.terminate()
      fixture.cleanup()
    }
  }
}

