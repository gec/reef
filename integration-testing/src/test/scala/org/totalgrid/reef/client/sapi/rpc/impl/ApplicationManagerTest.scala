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
import org.totalgrid.reef.app._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }

import org.totalgrid.reef.app.impl.{ SimpleConnectedApplicationManager, ApplicationManagerSettings }
import com.weiglewilczek.slf4s.Logging

@RunWith(classOf[JUnitRunner])
class ApplicationManagerTest extends ServiceClientSuite with Logging {

  val userSettings = new UserSettings("applicationUser", "password")
  val nodeSettings = new NodeSettings("nodeName", "location", "network")
  val baseInstanceName = "test-app"
  val instanceName = "nodeName-test-app"
  val settings = new ApplicationManagerSettings(userSettings, nodeSettings, Some(1), 50, 50)

  test("ApplicationConnectionManager integration test") {
    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val appManager = new SimpleConnectedApplicationManager(client, connectionProvider, settings)

    val connected = new SynchronizedVariable(false)

    appManager.addConnectedApplication(new ConnectedApplication {
      def getApplicationSettings = new ApplicationSettings(baseInstanceName, List("HMI"))

      def onApplicationShutdown() = {
        logger.info("App shutdown")
        connected.set(false)
      }

      def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) = {
        logger.info("App started")
        connected.set(true)
      }

      def onConnectionError(msg: String) = logger.debug(msg)
    })

    withGuestUser(userSettings, "all") {
      try {
        appManager.start()
        appManager.handleConnection(connection)

        connected shouldBecome (true) within 50000

        (0 to 5).foreach { i =>
          val appConfig = client.getApplicationByName(instanceName).await
          appConfig.getOnline should equal(true)

          // remove the application offline (will cause a heartbeat error)
          client.unregisterApplication(appConfig).await

          // application will be marked offline
          connected shouldBecome (false) within 50000

          // we should automatically retry, logging back in
          connected shouldBecome (true) within 50000
          client.getApplicationByName(instanceName).await.getOnline should equal(true)
        }
        // now stop the manager like an application would, make sure our app gets cleaned up and we are
        // informed that we are going offline
        appManager.stop()

        connected shouldBecome (false) within 50000
        client.getApplicationByName(instanceName).await.getOnline should equal(false)
      } finally {
        appManager.stop()
      }
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