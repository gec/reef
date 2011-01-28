/**
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.proto.{ Mapping, Measurements }

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner]) //disabled because it hangs under eclipse
class DNP3BindingTest extends FunSuite with ShouldMatchers {

  val startPort = 32323

  /// This test shows that the startup/teardown behavior is working without crashing
  test("StartupTeardownOnJVM") {
    val num_port = 100
    val num_stack = 10
    val sm = new StackManager(true) // the stack will start running as soon as a master is added		

    val cfg = new MasterStackConfig

    // startup <num_stack> masters on <num_port> ports
    (1 to num_port).foreach { port =>

      val s = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)
      sm.AddTCPClient(port.toString, s, "127.0.0.1", startPort)

      (1 to num_stack).foreach { stack =>
        val name = "port-" + port + "-stack" + stack
        cfg.getLink.setLocalAddr(stack)

        // the masters won't get any data, so setting the IPublisher to null is OK
        sm.AddMaster(port.toString, name, FilterLevel.LEV_WARNING, null, cfg)
      }
    }

    // the manager is already running at this point, so let's stop it explicitly
    sm.Stop
  }

  test("MasterToSlaveOnJVM") {
    val num_pairs = 100
    val port_start = startPort
    val port_end = port_start + num_pairs - 1
    val lev = FilterLevel.LEV_WARNING
    val sm = new StackManager(false)
    val a = new CountingPublisherActor

    val master = new MasterStackConfig
    master.getMaster.setIntegrityRate(60000)
    val slave = new SlaveStackConfig

    slave.setDevice(new DeviceTemplate(100, 100, 100))

    val s = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)
    (port_start to port_end).foreach { port =>
      val client = "client-" + port
      val server = "server-" + port
      sm.AddTCPClient(client, s, "127.0.0.1", port)
      sm.AddTCPServer(server, s, "0.0.0.0", port)

      sm.AddMaster(client, client, lev, a.addPub, master)
      sm.AddSlave(server, server, lev, null, slave)
    }

    sm.Start
    assert(a.waitForMinMessages(300, 10000))
    sm.Stop
  }

}
