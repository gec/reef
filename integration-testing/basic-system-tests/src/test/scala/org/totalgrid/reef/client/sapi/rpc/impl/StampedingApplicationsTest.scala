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
import org.totalgrid.reef.app._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }

import org.totalgrid.reef.app.impl.{ SimpleConnectedApplicationManager, ApplicationManagerSettings }
import com.weiglewilczek.slf4s.Logging
import net.agileautomata.commons.testing.SynchronizedVariable
import org.totalgrid.reef.client.settings.util.PropertyReader

/**
 * tests that a large number of applications can be running and heartbeating (10 millisecond heartbeats)
 * without crashing the system.
 */
@RunWith(classOf[JUnitRunner])
class StampedingApplicationsTest extends ServiceClientSuite with Logging {

  lazy val appManager = makeAppManager()
  lazy val connectedStates = new SyncList[(String, Boolean)]
  lazy val errors = new SyncList[(String, String)]
  lazy val APPS = 30
  lazy val names = (1 to APPS).map { i => "App%03d".format(i) }

  test("Add all applications") {

    appManager.start()
    appManager.handleConnection(connection)

    names.foreach { n => appManager.addConnectedApplication(makeApplication(n)) }

    connectedStates.lengthShouldBecome(APPS) within APPS * 300

    connectedStates.get().filter(_._2 == true).map(_._1).sorted should equal(names)

  }

  test("Verify no errors occur") {
    errors.lengthShouldRemain(0) during 1000

    // val futures = (0 to 100).map { i => async.getApplications() }

    errors.lengthShouldRemain(0) during 1000

    // futures.map { _.await }

    errors.lengthShouldRemain(0) during 5000
  }

  test("Shutdown and remove applications") {
    appManager.stop()

    connectedStates.lengthShouldBecome(APPS * 2) within APPS * 300

    connectedStates.get().filter(_._2 == false).map(_._1).sorted should equal(names)

    names.foreach { n => client.unregisterApplication(client.getApplicationByName("node-" + n)) }
  }

  private def makeAppManager() = {
    val props = PropertyReader.readFromFile("../../org.totalgrid.reef.test.cfg")
    val userConfig = new UserSettings(props)
    val nodeSettings = new NodeSettings("node", "location", "network")
    val settings = new ApplicationManagerSettings(userConfig, nodeSettings, Some(10), 50, 50)
    val connectionProvider = Mockito.mock(classOf[ConnectionProvider])
    new SimpleConnectedApplicationManager(client, connectionProvider, settings)
  }

  private def makeApplication(name: String) = new ConnectedApplication {
    def getApplicationSettings = new ApplicationSettings(name, List("HMI"))

    def onApplicationShutdown() = {
      logger.info(name + " shutdown")
      connectedStates.append((name, false))
    }

    def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) = {
      logger.info(name + " started")
      connectedStates.append((name, true))
    }

    def onConnectionError(msg: String) = {
      logger.info(msg)
      errors.append((name, msg))
    }
  }

  class SyncList[A] extends SynchronizedVariable[List[A]](Nil) {
    def append(a: A) = modify(_ ::: List(a))
    def lengthShouldBecome(length: Int) = {
      def evaluate(success: Boolean, last: List[A], timeout: Long) =
        if (!success) throw new Exception("Expected list length:" + length + " within " + timeout + " ms but final value was " + last.size)
      new Become(_.size == length)(evaluate)
    }
    def lengthShouldRemain(length: Int) = {
      def evaluate(failure: Boolean, last: List[A], timeout: Long) =
        if (failure) throw new Exception("Expected list length:" + length + " during " + timeout + " ms but final value was " + last.size)
      new Remain(_.size == length)(evaluate)
    }
  }
}