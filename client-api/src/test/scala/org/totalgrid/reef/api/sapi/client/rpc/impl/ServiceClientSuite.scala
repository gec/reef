package org.totalgrid.reef.api.sapi.client.rpc.impl

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

import xml.Node
import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.api.japi.client.{ SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.api.sapi.impl.SystemPropertyConfigReader
import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.api.sapi.impl.ReefServicesList

import org.totalgrid.reef.broker.qpid.{ QpidBrokerConnectionInfo, QpidBrokerConnectionFactory }
import org.totalgrid.reef.api.sapi.client.rpc.framework.ApiBase
import org.totalgrid.reef.api.sapi.client.rpc.utils.InteractionRecorder

class SubscriptionEventAcceptorShim[A](fun: SubscriptionEvent[A] => Unit) extends SubscriptionEventAcceptor[A] {
  def onEvent(event: SubscriptionEvent[A]) = fun(event)
}

abstract class ClientSessionSuite(file: String, title: String, desc: Node) extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  // gets default connection settings or overrides using system properties
  val config = QpidBrokerConnectionInfo.loadInfo(new SystemPropertyConfigReader)
  val factory = new QpidBrokerConnectionFactory(config)
  val broker = factory.connect
  val exe = Executors.newScheduledThreadPool()
  val conn = new DefaultConnection(ReefServicesList, broker, exe, 5000)
  val session = conn.login("system", "system").await /// TODO - should load user out of config file
  val client = new ApiBase(session) with AllScadaServiceImpl
  val recorder = new InteractionRecorder {}

  override def beforeAll() {
    //client.addRequestSpy(client)
  }

  override def afterAll() {
    broker.disconnect()
    exe.shutdown()
    recorder.save(file, title, desc)
  }

}

