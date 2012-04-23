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

import org.scalatest.{ Tag, FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.totalgrid.reef.client.sapi.sync.AllScadaService
import org.totalgrid.reef.client.sapi.rpc.{ AllScadaService => AsyncAllScadaService }
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.standalone.InMemoryNode
import org.totalgrid.reef.loader.commons.LoaderServicesList
import org.totalgrid.reef.client.sapi.client.SubscriptionCanceler
import org.totalgrid.reef.client.factory.ReefConnectionFactory
import org.totalgrid.reef.client._

class SubscriptionEventAcceptorShim[A](fun: SubscriptionEvent[A] => Unit) extends SubscriptionEventAcceptor[A] {
  def onEvent(event: SubscriptionEvent[A]) = fun(event)
}

abstract class ServiceClientSuite extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  // name of the model file to initialize the system with
  def modelFile: String = "../../assemblies/assembly-common/filtered-resources/samples/integration/config.xml"

  // we use options so we can avoid starting the factories until the test is actually run
  private var factoryOption = Option.empty[ConnectionFactory]
  private var connectionOption = Option.empty[Connection]
  private var sessionOption = Option.empty[Client]
  private var clientOption = Option.empty[AllScadaService]
  private var asyncClientOption = Option.empty[AsyncAllScadaService]

  val canceler = new SubscriptionCanceler

  def session = sessionOption.get
  def client = clientOption.get
  def async = asyncClientOption.get
  def connection = connectionOption.get

  private lazy val remoteTest = System.getProperty("remote-test") != null

  override def beforeAll() {
    // gets default connection settings or overrides using system properties
    val props = PropertyReader.readFromFile("../../org.totalgrid.reef.test.cfg")
    val userConfig = new UserSettings(props)

    val conn: Connection = if (remoteTest) {

      val config = new AmqpSettings(props)

      factoryOption = Some(ReefConnectionFactory.buildFactory(config, new ReefServices))
      factoryOption.get.connect()
    } else {
      InMemoryNode.initialize("../../standalone-node.cfg", true, None)
      InMemoryNode.connection
    }
    conn.addServicesList(new LoaderServicesList)
    conn.addServicesList(new ReefServices)
    connectionOption = Some(conn)

    sessionOption = Some(conn.login(userConfig))
    clientOption = Some(session.getService(classOf[AllScadaService]))
    asyncClientOption = Some(session.getService(classOf[AsyncAllScadaService]))

    client.addSubscriptionCreationListener(canceler)

    client.setHeaders(client.getHeaders.setTimeout(50000))
    // TODO: filtering is done after the result limit is applied
    client.setHeaders(client.getHeaders.setResultLimit(5000))

    ModelPreparer.load(modelFile, session)
  }

  override def afterAll() {
    canceler.cancel()
    factoryOption.foreach(_.terminate())
  }

  // we preface all of the tests with the REMOTE name so we can easily tell if it failed
  // during the first run
  override protected def test(testName: String, testTags: Tag*)(testFun: => Unit) {
    val name = if (remoteTest) "REMOTE-" + testName else testName
    super.test(name, testTags: _*)(testFun)
  }
}

