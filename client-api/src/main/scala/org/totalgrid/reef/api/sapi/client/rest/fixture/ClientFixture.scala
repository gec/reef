package org.totalgrid.reef.api.sapi.client.rest.fixture

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
import org.totalgrid.reef.api.sapi.impl.ReefServicesList
import org.totalgrid.reef.api.sapi.client.rest.Connection
import org.totalgrid.reef.broker.BrokerConnectionFactory
import org.totalgrid.reef.broker.qpid.{ QpidBrokerConnectionFactory, QpidBrokerConnectionInfo }
import org.totalgrid.reef.broker.memory.MemoryBrokerConnectionFactory
import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection

trait BrokerFixture {
  def factory: BrokerConnectionFactory
  def cleanup(): Unit = {}
}

class QpidBrokerFixture(config: QpidBrokerConnectionInfo) extends BrokerFixture {
  private val fac = new QpidBrokerConnectionFactory(config)
  def factory = fac
}

class MemoryBrokerFixture extends BrokerFixture {
  private val exe = Executors.newScheduledThreadPool()
  private val fac = new MemoryBrokerConnectionFactory(exe)
  override def factory = fac
  override def cleanup() = exe.shutdown()
}

object ConnectionFixture {

  val qpidDefault = QpidBrokerConnectionInfo.loadInfo("test")

  def qpid(config: QpidBrokerConnectionInfo = qpidDefault)(test: Connection => Unit): Unit = {
    using(new QpidBrokerFixture(config))(test)
  }

  def mock(test: Connection => Unit): Unit = using(new MemoryBrokerFixture)(test)

  def using(fixture: BrokerFixture)(test: Connection => Unit): Unit = {
    val exe = Executors.newScheduledThreadPool()
    try {
      val broker = fixture.factory.connect
      val conn = new DefaultConnection(ReefServicesList, broker, exe, 5000)
      test(conn)
    } finally {
      exe.shutdown()
      fixture.cleanup()
    }
  }
}

