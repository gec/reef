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
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.sapi.client.factory.ReefFactory
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.{ SubscriptionBinding, SubscriptionCreationListener, SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.standalone.InMemoryNode
import org.totalgrid.reef.loader.commons.LoaderServicesList

class SubscriptionEventAcceptorShim[A](fun: SubscriptionEvent[A] => Unit) extends SubscriptionEventAcceptor[A] {
  def onEvent(event: SubscriptionEvent[A]) = fun(event)
}

class SubscriptionCanceler extends SubscriptionCreationListener {

  var subs = List.empty[SubscriptionBinding]

  def onSubscriptionCreated(binding: SubscriptionBinding) = this.synchronized {
    subs ::= binding
  }

  def cancel() = this.synchronized {
    subs.foreach { _.cancel() }
  }
}

abstract class ServiceClientSuite extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  // name of the model file to initialize the system with
  def modelFile: String = "../assemblies/assembly-common/filtered-resources/samples/integration/config.xml"

  // we use options so we can avoid starting the factories until the test is actually run
  private var factoryOption = Option.empty[ReefFactory]
  private var sessionOption = Option.empty[Client]
  private var clientOption = Option.empty[AllScadaService]

  val canceler = new SubscriptionCanceler

  def session = sessionOption.get
  def client = clientOption.get

  override def beforeAll() {
    // gets default connection settings or overrides using system properties
    val props = PropertyReader.readFromFile("../org.totalgrid.reef.test.cfg")
    val userConfig = new UserSettings(props)

    val conn = if (System.getProperty("remote-test") != null) {

      val config = new AmqpSettings(props)

      factoryOption = Some(new ReefFactory(config, new ReefServices))
      factoryOption.get.connect()
    } else {
      InMemoryNode.initialize("../standalone-node.cfg", true, modelFile)
      InMemoryNode.connection
    }
    conn.addServicesList(new LoaderServicesList)
    conn.addServicesList(new ReefServices)

    sessionOption = Some(conn.login(userConfig).await)
    clientOption = Some(session.getRpcInterface(classOf[AllScadaService]))
    client.addSubscriptionCreationListener(canceler)

    client.setHeaders(client.getHeaders.setTimeout(50000))
  }

  override def afterAll() {
    canceler.cancel()
    factoryOption.foreach(_.terminate())
  }

}

