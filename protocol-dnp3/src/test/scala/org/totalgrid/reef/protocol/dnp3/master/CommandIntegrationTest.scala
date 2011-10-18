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
package org.totalgrid.reef.protocol.dnp3.master

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.protocol.dnp3._
import org.totalgrid.reef.protocol.dnp3.mock.InstantCommandResponder
import org.totalgrid.reef.protocol.dnp3.common.LogAdapter
import org.totalgrid.reef.protocol.dnp3.mock.{CachingResponseAcceptor, CountingPublisher}

@RunWith(classOf[JUnitRunner]) //disabled because it hangs under eclipse
class CommandIntegrationTest extends FunSuite with ShouldMatchers {

  val startPort = 33323

  test("Command Handling") {
    val num_pairs = 10
    val port_start = startPort
    val port_end = port_start + num_pairs - 1
    val lev = FilterLevel.LEV_WARNING
    val sm = new StackManager
    val adapter = new LogAdapter
    sm.AddLogHook(adapter)
    val counter = new CountingPublisher


    val master = new MasterStackConfig
    master.getMaster.setIntegrityRate(60000)
    val slave = new SlaveStackConfig

    slave.setDevice(new DeviceTemplate(100, 100, 100, 0, 0, 1, 1))

    val commandHandler = new InstantCommandResponder(CommandStatus.CS_SUCCESS)

    val s = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)
    val acceptors = (port_start to port_end).map { port =>
      val client = "client-" + port
      val server = "server-" + port
      sm.AddTCPClient(client, s, "127.0.0.1", port)
      sm.AddTCPServer(server, s, "0.0.0.0", port)

      val commandAcceptor = sm.AddMaster(client, client, lev, counter.newPublisher, master)
      sm.AddSlave(server, server, lev, commandHandler, slave)
      commandAcceptor
    }

    counter.waitForMinMessages(1, 10000) should equal(true)

    val responder = new CachingResponseAcceptor

    val bo = new BinaryOutput(ControlCode.CC_PULSE_TRIP, 1, 100, 100)
    val spInt = new Setpoint(100)
    val spDbl = new Setpoint(99.8)

    var seq: Int = 999
    def next() = {
      seq += 1
      seq
    }

    (1 to (150 / num_pairs)).foreach { i =>
      acceptors.foreach { ac =>

        ac.AcceptCommand(bo, 0, next(), responder)
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)

        ac.AcceptCommand(spInt, 0, next(), responder)
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)

        ac.AcceptCommand(spDbl, 0, next(), responder)
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)
      }
    }

    sm.Shutdown()
  }

}
