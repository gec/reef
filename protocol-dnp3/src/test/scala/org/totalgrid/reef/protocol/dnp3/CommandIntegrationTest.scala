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
package org.totalgrid.reef.protocol.dnp3

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.util.{ EmptySyncVar }
import org.totalgrid.reef.executor.{ ReactActorExecutor, Executor }

@RunWith(classOf[JUnitRunner]) //disabled because it hangs under eclipse
class CommandIntegrationTest extends FunSuite with ShouldMatchers {

  val startPort = 33323

  test("Command Handling") {
    val num_pairs = 10
    val port_start = startPort
    val port_end = port_start + num_pairs - 1
    val lev = FilterLevel.LEV_WARNING
    val sm = new StackManager(false)
    val a = new CountingPublisherActor

    val master = new MasterStackConfig
    master.getMaster.setIntegrityRate(60000)
    val slave = new SlaveStackConfig

    slave.setDevice(new DeviceTemplate(100, 100, 100, 0, 0, 1, 1))

    val executor = new ReactActorExecutor {}
    executor.start
    val commandHandler = new InstantCommandResponder(CommandStatus.CS_SUCCESS, executor)

    val s = new PhysLayerSettings(FilterLevel.LEV_WARNING, 1000)
    val acceptors = (port_start to port_end).map { port =>
      val client = "client-" + port
      val server = "server-" + port
      sm.AddTCPClient(client, s, "127.0.0.1", port)
      sm.AddTCPServer(server, s, "0.0.0.0", port)

      val commandAcceptor = sm.AddMaster(client, client, lev, a.addPub, master)
      sm.AddSlave(server, server, lev, commandHandler, slave)
      commandAcceptor
    }

    sm.Start
    assert(a.waitForMinMessages(1, 10000))

    val responder = new CachingResponseAcceptor()

    var seq: Int = 999
    (1 to (150 / num_pairs)).foreach { i =>
      acceptors.foreach { ac =>
        seq += 1;
        { // putting this into its own frame to try to make garbage collector reap the BinaryOutput and Setpoint objects
          ac.AcceptCommand(new BinaryOutput(ControlCode.CC_PULSE_TRIP, 1, seq, seq), 0, seq, responder)
        }
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)

        seq += 1;
        {
          ac.AcceptCommand(new Setpoint(seq.toDouble * 100), 0, seq, responder)
        }
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)

        seq += 1;
        {
          ac.AcceptCommand(new Setpoint(seq.toInt * 100), 0, seq, responder)
        }
        responder.waitFor(seq, CommandStatus.CS_SUCCESS)
      }
    }

    sm.Stop
  }

  class CachingResponseAcceptor extends IResponseAcceptor {

    val responsesRecieved = new EmptySyncVar[(Int, CommandStatus)]
    override def AcceptResponse(response: CommandResponse, sequence: Int) {
      responsesRecieved.update((sequence, response.getMResult))
    }

    def waitFor(sequence: Int, status: CommandStatus) = {
      responsesRecieved.waitFor({ r => r._1 == sequence && r._2 == status })
    }
  }

  class InstantCommandResponder(status: CommandStatus, executor: Executor) extends ICommandAcceptor {

    override def AcceptCommand(obj: BinaryOutput, index: Long, seq: Int, accept: IResponseAcceptor) {
      executor.delay(10) {
        accept.AcceptResponse(response, seq)
      }
    }

    override def AcceptCommand(obj: Setpoint, index: Long, seq: Int, accept: IResponseAcceptor) {
      executor.delay(10) {
        accept.AcceptResponse(response, seq)
      }
    }

    private def response = new CommandResponse(status)
  }
}
