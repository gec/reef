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
package org.totalgrid.reef.integration.authz

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.settings.NodeSettings

@RunWith(classOf[JUnitRunner])
class ApplicationAuthTest extends AuthTestBase {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/authorization/config.xml"

  val USER = "remote_application"

  test("Applications can't see live system") {
    as(USER) { admin =>
      unAuthed(USER + " shouldnt see system") { admin.getCommands() }
      unAuthed(USER + " shouldnt see system") { admin.getCommandHistory() }
      unAuthed(USER + " shouldnt see system") { admin.getPoints() }
      unAuthed(USER + " shouldnt see system") { admin.getEndpointConnections() }
    }
  }

  test("Apps can register and heartbeat") {
    as(USER) { fep =>
      val nodeSettings = new NodeSettings("node1", "any", "any")
      val appConfig = fep.registerApplication(nodeSettings, "fakeApplication", List("HMI"))

      fep.getApplicationByName(appConfig.getInstanceName) should equal(appConfig)

      fep.sendHeartbeat(appConfig)

      fep.sendApplicationOffline(appConfig)

      fep.unregisterApplication(appConfig)
    }
  }

  test("Cant heartbeat or delete another users application") {
    as(USER) { fep =>
      val nodeSettings = new NodeSettings("node1", "any", "any")
      val appConfig = fep.registerApplication(nodeSettings, "fakeApplication", List("HMI"))

      as("hmi_app") { hmi =>
        unAuthed("application heartbeated by non owner") {
          hmi.sendHeartbeat(appConfig)
        }

        unAuthed("application deleted by non owner") {
          hmi.unregisterApplication(appConfig)
        }
      }

      fep.unregisterApplication(appConfig)
    }
  }
}