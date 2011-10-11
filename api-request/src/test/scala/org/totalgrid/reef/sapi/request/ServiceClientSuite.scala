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
package org.totalgrid.reef.sapi.request

import org.totalgrid.reef.executor.ReactActorExecutor
import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.proto.Auth.{ Agent, AuthToken }
import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.totalgrid.reef.proto.ReefServicesList
import xml.Node
import org.totalgrid.reef.util.SystemPropertyConfigReader
import org.totalgrid.reef.messaging.AmqpClientSession
import org.totalgrid.reef.broker.api.BrokerConnectionInfo

import org.totalgrid.reef.japi.client.{ SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.japi.request.utils.InteractionRecorder
import org.totalgrid.reef.sapi.request.impl.AllScadaServiceImpl
import org.totalgrid.reef.sapi.request.framework.SingleSessionClientSource

class SubscriptionEventAcceptorShim[T](fun: SubscriptionEvent[T] => _) extends SubscriptionEventAcceptor[T] {
  def onEvent(event: SubscriptionEvent[T]) = fun(event)
}

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

  // gets default connection settings or overrides using system properties
  val config = BrokerConnectionInfo.loadInfo(new SystemPropertyConfigReader())
  val factory = new AMQPSyncFactory with ReactActorExecutor {
    val broker = new QpidBrokerConnection(config)
  }

  lazy val client = connect

  val username = "system"
  val password = "system"

  def connect = {
    val client = new AmqpClientSession(factory, ReefServicesList, 5000) with AllScadaServiceImpl with InteractionRecorder with SingleSessionClientSource {
      def session = this
    }
    client.addRequestSpy(client)

    val agent = Agent.newBuilder.setName(username).setPassword(password).build
    val request = AuthToken.newBuilder.setAgent(agent).build
    val response = client.put(request).await().expectOne

    client.modifyHeaders(_.addAuthToken(response.getToken))

    client
  }
}

