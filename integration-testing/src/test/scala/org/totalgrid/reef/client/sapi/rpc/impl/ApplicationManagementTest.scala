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

import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.settings.NodeSettings
import org.totalgrid.reef.proto.ProcessStatus.StatusSnapshot
import org.totalgrid.reef.client.exceptions.BadRequestException
import org.totalgrid.reef.proto.Application.HeartbeatConfig

@RunWith(classOf[JUnitRunner])
class ApplicationManagementTest extends ClientSessionSuite("Application.xml", "Application", <div></div>) {

  val nodeSettings = new NodeSettings("testNode", "testServer", "testNetwork")

  test("Register, heartbeat then offline application") {

    val appConfig = client.registerApplication(nodeSettings, "testNode-Register", Nil).await
    val hconfig = appConfig.getHeartbeatCfg

    (1 to 5).foreach { i =>
      client.sendHeartbeat(makeProto(hconfig, true)).await
    }

    // send offline application
    client.sendHeartbeat(makeProto(hconfig, false)).await

    intercept[BadRequestException] {
      // cannot heartbeat an already offline application
      client.sendHeartbeat(makeProto(hconfig, true)).await
    }

    client.unregisterApplication(appConfig).await
  }

  test("Unregister then try heartbeating") {
    val appConfig = client.registerApplication(nodeSettings, "testNode-Register", Nil).await
    val hconfig = appConfig.getHeartbeatCfg

    client.unregisterApplication(appConfig).await

    intercept[BadRequestException] {
      // cannot heartbeat an already offline application
      client.sendHeartbeat(makeProto(hconfig, false)).await
    }
  }

  private def makeProto(configuration: HeartbeatConfig, online: Boolean) = StatusSnapshot.newBuilder
    .setProcessId(configuration.getProcessId)
    .setInstanceName(configuration.getInstanceName)
    .setTime(System.currentTimeMillis)
    .setOnline(online).build
}