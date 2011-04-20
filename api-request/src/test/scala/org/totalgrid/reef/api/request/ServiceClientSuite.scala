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
package org.totalgrid.reef.api.request

import impl.AllScadaServiceImpl
import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.messaging.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.proto.Auth.{ Agent, AuthToken }
import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.totalgrid.reef.api.{ ServiceHandlerHeaders, IConnectionListener }
import org.totalgrid.reef.proto.ReefServicesList
import utils.InteractionRecorder
import xml.Node
import org.totalgrid.reef.util.{ SystemPropertyConfigReader, SyncVar }
import org.totalgrid.reef.messaging.{ BrokerConnectionInfo, ProtoClient }

abstract class ClientSessionSuite(file: String, title: String, desc: Node) extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  override def beforeAll() {
    factory.connect(5000)
  }
  override def afterAll() {
    factory.disconnect(5000)

    client.save(file, title, desc)
  }

  import ServiceHandlerHeaders._

  // gets default connection settings or overrides using system properties
  val config = BrokerConnectionInfo.loadInfo(new SystemPropertyConfigReader())
  val factory = new AMQPSyncFactory with ReactActor {
    val broker = new QpidBrokerConnection(config)
  }

  lazy val client = connect

  def connect = {
    val client = new ProtoClient(factory, ReefServicesList, 5000) with AllScadaServiceImpl with InteractionRecorder {
      val ops = this
    }

    val agent = Agent.newBuilder.setName("core").setPassword("core").build
    val request = AuthToken.newBuilder.setAgent(agent).build
    val response = client.putOneOrThrow(request)

    client.getDefaultHeaders.addAuthToken(response.getToken)

    client
  }
}

object ServiceClientSuite {
  // TODO: move BrokerConnectionState into amqp
  class BrokerConnectionState extends IConnectionListener {
    private val connected = new SyncVar(false)

    override def opened() = connected.update(true)
    override def closed() = connected.update(false)

    def waitUntilStarted(timeout: Long = 5000) = connected.waitUntil(true, timeout)
    def waitUntilStopped(timeout: Long = 5000) = connected.waitUntil(false, timeout)
  }
}

