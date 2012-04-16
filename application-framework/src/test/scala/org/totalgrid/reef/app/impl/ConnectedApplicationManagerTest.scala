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
package org.totalgrid.reef.app.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing._
import net.agileautomata.commons.testing._

import org.totalgrid.reef.client.sapi.client.ServiceTestHelpers._

import org.mockito.Mockito
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.settings._
import org.totalgrid.reef.client.{ Client, Connection }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.proto.Application.{ HeartbeatConfig, ApplicationConfig }
import org.totalgrid.reef.client.service.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.app.{ ConnectionProvider, ApplicationSettings, ConnectedApplication }
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.Envelope

@RunWith(classOf[JUnitRunner])
class ConnectedApplicationManagerTest extends FunSuite with ShouldMatchers {

  val userSettings = new UserSettings("user", "password")
  val nodeSettings = new NodeSettings("nodeName", "location", "network")

  val capabilites = List("Cap1", "Cap2")
  val baseInstanceName = "instance"
  val instanceName = "nodeName-instance"

  val settings = new ApplicationManagerSettings(userSettings, nodeSettings)

  class MockAppListener extends ConnectedApplication {
    def getApplicationSettings = new ApplicationSettings(baseInstanceName, capabilites)

    def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) = {
      connected.set(true)
    }

    def onApplicationShutdown() = {
      connected.set(false)
    }

    def onConnectionError(msg: String) = errors.append(msg)

    val connected = new SynchronizedVariable(false)
    var errors = new SynchronizedList[String]()

    def clearErrors() = errors = new SynchronizedList[String]()

    def errorsShouldInclude(msg: String) = {
      def evaluate(success: Boolean, last: List[String], timeout: Long) =
        if (!success) throw new Exception("Expected strings to include " + msg + " within " + timeout + " ms but final value was " + last)
      val (result, success) = errors.value.awaitUntil(500)(list => list.find(_.indexOf(msg) != -1).isDefined)
      evaluate(success, result, 500)
    }

    def connectedShouldBecome(value: Boolean) = {
      connected.shouldBecome(value)
    }
  }

  def makeServices(): (Connection, AllScadaService) = {
    val client = Mockito.mock(classOf[Client], new MockitoStubbedOnly)
    val services = Mockito.mock(classOf[AllScadaService], new MockitoStubbedOnly)
    val connection = Mockito.mock(classOf[Connection], new MockitoStubbedOnly)

    //Mockito.doReturn(success(true)).when(client).logout()
    Mockito.doNothing().when(client).logout()

    Mockito.doReturn(client).when(client).spawn()
    Mockito.doReturn(services).when(client).getService(classOf[AllScadaService])
    Mockito.doReturn(client).when(connection).login(userSettings)
    (connection, services)
  }

  test("Login failure retries") {
    val executor = new MockExecutor()

    val listener = new MockAppListener

    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val manager = new SimpleConnectedApplicationManager(executor, connectionProvider, settings)
    manager.addConnectedApplication(listener)

    val (connection, services) = makeServices()

    Mockito.doThrow(new ReefServiceException("Unknown user", Envelope.Status.RESPONSE_TIMEOUT))
      .when(connection).login(userSettings)

    manager.start()

    manager.handleConnection(connection)

    executor.runUntilIdle()

    listener errorsShouldInclude ("Unknown user")
    listener.clearErrors()

    executor.tick(10000.milliseconds)
    executor.runUntilIdle()

    listener errorsShouldInclude ("Unknown user")
  }

  test("Application registration failure causes relogin attempts") {
    val executor = new MockExecutor()

    val listener = new MockAppListener

    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val manager = new SimpleConnectedApplicationManager(executor, connectionProvider, settings)
    manager.addConnectedApplication(listener)
    manager.start()

    val (connection, services) = makeServices()

    Mockito.doReturn(failure("Can't register app")).when(services).registerApplication(nodeSettings, instanceName, capabilites)

    manager.handleConnection(connection)

    executor.runUntilIdle()

    listener errorsShouldInclude ("Can't register app")
    listener.clearErrors()

    Mockito.doThrow(new ReefServiceException("Unknown user", Envelope.Status.RESPONSE_TIMEOUT))
      .when(connection).login(userSettings)

    executor.tick(10000.milliseconds)
    executor.runUntilIdle()

    listener errorsShouldInclude ("Unknown user")
  }

  test("Sucessful Connection") {
    val executor = new MockExecutor()

    val listener = new MockAppListener

    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val manager = new SimpleConnectedApplicationManager(executor, connectionProvider, settings)
    manager.addConnectedApplication(listener)

    val (connection, services) = makeServices()

    Mockito.doReturn(failure("Can't register app")).when(services).registerApplication(nodeSettings, instanceName, capabilites)

    val appConfig = ApplicationConfig.newBuilder.setInstanceName("name").setHeartbeatCfg(HeartbeatConfig.newBuilder).build
    Mockito.doReturn(success(appConfig)).when(services).registerApplication(nodeSettings, instanceName, capabilites)
    val status = StatusSnapshot.newBuilder.build
    Mockito.doReturn(success(status)).when(services).sendHeartbeat(appConfig)

    Mockito.doReturn(success(status)).when(services).sendApplicationOffline(appConfig)

    manager.start()

    manager.handleConnection(connection)

    executor.runUntilIdle()

    listener connectedShouldBecome (true) within 500

    manager.stop()

    listener connectedShouldBecome (false) within 500

    Mockito.verify(services).sendApplicationOffline(appConfig)
  }

  test("Heartbeat failure causes disconnect") {
    val executor = new MockExecutor()

    val listener = new MockAppListener

    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    val manager = new SimpleConnectedApplicationManager(executor, connectionProvider, settings)
    manager.addConnectedApplication(listener)

    val (connection, services) = makeServices()

    Mockito.doReturn(failure("Can't register app")).when(services).registerApplication(nodeSettings, instanceName, capabilites)

    val appConfig = ApplicationConfig.newBuilder.setInstanceName("name").setHeartbeatCfg(HeartbeatConfig.newBuilder.setPeriodMs(100)).build
    Mockito.doReturn(success(appConfig)).when(services).registerApplication(nodeSettings, instanceName, capabilites)
    val status = StatusSnapshot.newBuilder.build
    Mockito.doReturn(success(status)).when(services).sendHeartbeat(appConfig)

    Mockito.doReturn(success(status)).when(services).sendApplicationOffline(appConfig)

    manager.start()

    manager.handleConnection(connection)

    executor.runUntilIdle()

    listener connectedShouldBecome (true) within 500

    executor.tick(10000.milliseconds)

    Mockito.doReturn(failure("Unexpected heartbeat failure")).when(services).sendHeartbeat(appConfig)

    executor.tick(10000.milliseconds)

    listener errorsShouldInclude ("Unexpected heartbeat failure")
    listener connectedShouldBecome (false) within 500

    Mockito.doReturn(success(status)).when(services).sendHeartbeat(appConfig)

    executor.tick(10000.milliseconds)
    listener connectedShouldBecome (true) within 500

  }

}