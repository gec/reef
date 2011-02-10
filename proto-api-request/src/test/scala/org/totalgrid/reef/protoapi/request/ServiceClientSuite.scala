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
package org.totalgrid.reef.protoapi.request

import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.proto.Auth.{ Agent, AuthToken }
import org.totalgrid.reef.messaging.{ BrokerConnectionListener, BrokerConnectionInfo, ServicesList, ProtoClient }
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.protoapi.{ RequestEnv, ServiceHandlerHeaders }
import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }

abstract class ServiceClientSuite(file: String, title: String, desc: String) extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {
  val doc = new Documenter(file, title, desc)

  override def beforeAll() {
    factory.start
    val waiter = new ServiceClientSuite.BrokerConnectionState
    factory.addConnectionListener(waiter)
    waiter.waitUntilStarted()

  }
  override def afterAll() {
    factory.stop
    doc.save
  }

  import ServiceHandlerHeaders._

  val config = new BrokerConnectionInfo("127.0.0.1", 5672, "guest", "guest", "test")
  val factory = new AMQPSyncFactory with ReactActor {
    val broker = new QpidBrokerConnection(config)
  }

  lazy val client = connect

  def connect = {
    val client = new ProtoClient(factory, 5000, ServicesList.getServiceInfo)

    val agent = Agent.newBuilder.setName("core").setPassword("core").build
    val request = AuthToken.newBuilder.setAgent(agent).build
    val response = client.putOneOrThrow(request)

    if (client.defaultEnv.isEmpty) client.defaultEnv = Some(new RequestEnv)
    client.defaultEnv.get.setAuthToken(response.getToken)

    client
  }
}

object ServiceClientSuite {
  class BrokerConnectionState extends BrokerConnectionListener {
    private val connected = new SyncVar(false)

    override def opened() = connected.update(true)
    override def closed() = connected.update(false)

    def waitUntilStarted(timeout: Long = 5000) = connected.waitUntil(true, timeout)
    def waitUntilStopped(timeout: Long = 5000) = connected.waitUntil(false, timeout)
  }
}

