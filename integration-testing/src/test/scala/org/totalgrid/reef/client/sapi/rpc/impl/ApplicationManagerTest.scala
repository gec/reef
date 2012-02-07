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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.mockito.Mockito
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings }
import net.agileautomata.commons.testing.SynchronizedVariable
import org.totalgrid.reef.app.{ ApplicationSettings, ApplicationConnectionListener, ConnectionProvider, SimpleApplicationConnectionManager }

@RunWith(classOf[JUnitRunner])
class ApplicationManagerTest extends ServiceClientSuite {

  val userSettings = new UserSettings("applicationUser", "password")
  val nodeSettings = new NodeSettings("nodeName", "location", "network")
  val instanceName = "test-app"
  val settings = ApplicationSettings(userSettings, nodeSettings, instanceName, List("HMI"), Some(100), 50)

  test("ApplicationConnectionManager integration test") {
    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val appManager = new SimpleApplicationConnectionManager(client, connectionProvider)

    val connected = new SynchronizedVariable(false)

    appManager.addConnectionListener(new ApplicationConnectionListener {
      def onConnectionStatusChanged(isConnected: Boolean) = connected.set(isConnected)

      def onConnectionError(msg: String, exception: Option[Exception]) = null
    })

    withGuestUser(userSettings, "all") {
      appManager.start(settings)
      appManager.handleConnection(connection)

      connected shouldBecome (true) within 50000

      val appConfig = client.getApplicationByName(instanceName).await
      appConfig.getOnline should equal(true)

      // mark the application offline (will cause a heartbeat error)
      client.sendApplicationOffline(appConfig).await

      // application will be marked offline
      connected shouldBecome (false) within 50000
      client.getApplicationByName(instanceName).await.getOnline should equal(false)

      // we should automatically retry, logging back in
      connected shouldBecome (true) within 50000
      client.getApplicationByName(instanceName).await.getOnline should equal(true)

      // now stop the manager like an application would, make sure our app gets cleaned up and we are
      // informed that we are going offline
      appManager.stop()

      connected shouldBecome (false) within 50000
      client.getApplicationByName(instanceName).await.getOnline should equal(false)
    }

    client.unregisterApplication(client.getApplicationByName(instanceName).await).await
  }

  private def withGuestUser(userSettings: UserSettings, permission: String = "read_only")(fun: => Unit) = {
    val agent = client.createNewAgent(userSettings.getUserName, userSettings.getUserPassword, List(permission)).await
    try {
      fun
    } finally {
      client.deleteAgent(agent).await
    }
  }

}