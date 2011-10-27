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
package org.totalgrid.reef.client.sapi.rpc.impl.util

import xml.Node
import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.totalgrid.reef.clientapi.{ SubscriptionEvent, SubscriptionEventAcceptor }

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.clientapi.settings.util.PropertyReader
import org.totalgrid.reef.client.ReefFactory
import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import org.totalgrid.reef.clientapi.settings.{ UserSettings, AmqpSettings }

class SubscriptionEventAcceptorShim[A](fun: SubscriptionEvent[A] => Unit) extends SubscriptionEventAcceptor[A] {
  def onEvent(event: SubscriptionEvent[A]) = fun(event)
}

abstract class ClientSessionSuite(file: String, title: String, desc: Node) extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  // we use options so we can avoid starting the factories until the test is actually run
  private var factoryOption = Option.empty[ReefFactory]
  private var sessionOption = Option.empty[Client]
  private var clientOption = Option.empty[AllScadaService]

  val recorder = new InteractionRecorder {}

  def session = sessionOption.get
  def client = clientOption.get

  override def beforeAll() {
    // gets default connection settings or overrides using system properties

    val props = PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg")

    val config = new AmqpSettings(props)
    val userConfig = new UserSettings(props)
    factoryOption = Some(new ReefFactory(config))
    val conn = factoryOption.get.connect()
    sessionOption = Some(conn.login(userConfig.getUserName, userConfig.getUserPassword).await)
    clientOption = Some(session.getRpcInterface(classOf[AllScadaService]))
    client.addRequestSpy(recorder)
  }

  override def afterAll() {
    recorder.save(file, title, desc)
    factoryOption.foreach(_.terminate())
  }

}

