package org.totalgrid.reef.client.sapi.rpc.impl.util

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
import org.totalgrid.reef.api.japi.client.{ SubscriptionEvent, SubscriptionEventAcceptor }

import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.api.japi.settings.util.PropertyReader
import org.totalgrid.reef.api.japi.settings.AmqpSettings
import org.totalgrid.reef.client.ReefFactory

class SubscriptionEventAcceptorShim[A](fun: SubscriptionEvent[A] => Unit) extends SubscriptionEventAcceptor[A] {
  def onEvent(event: SubscriptionEvent[A]) = fun(event)
}

// TODO: remove all explanation, recorder stuff
abstract class ClientSessionSuite(file: String, title: String, desc: Node) extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(file: String, title: String, desc: String) = {
    this(file, title, <div>{ desc }</div>)
  }

  // gets default connection settings or overrides using system properties
  val config = new AmqpSettings(PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg"))
  val factory = new ReefFactory(config)
  val conn = factory.connect()
  val session = conn.login("system", "system").await /// TODO - should load user out of config file
  val client = session.getRpcInterface(classOf[AllScadaService])
  val recorder = new InteractionRecorder {}

  override def beforeAll() {
    //client.addRequestSpy(client)
  }

  override def afterAll() {
    factory.terminate()
    recorder.save(file, title, desc)
  }

}

