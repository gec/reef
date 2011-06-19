/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.protocol.benchmark

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.proto.{ Model, SimMapping, Measurements, Commands }
import org.totalgrid.reef.promise.FixedPromise
import org.totalgrid.reef.protocol.api.{ Protocol, NullEndpointPublisher, Publisher }
import org.totalgrid.reef.executor.mock.MockExecutor

@RunWith(classOf[JUnitRunner])
class BenchmarkProtocolTest extends FunSuite with ShouldMatchers {
  val simpleMapping = SimMapping.SimulatorMapping.newBuilder
    .setDelay(100)
    .addMeasurements(makeAnalogSim("analog1"))
    .addMeasurements(makeAnalogSim("analog2"))
    .addCommands(makeCommandSim("success", Commands.CommandStatus.SUCCESS))
    .addCommands(makeCommandSim("fail", Commands.CommandStatus.HARDWARE_ERROR))
    .build

  def makeAnalogSim(name: String = "test") = {
    SimMapping.MeasSim.newBuilder
      .setName(name)
      .setType(Measurements.Measurement.Type.DOUBLE)
      .setInitial(0).setMin(-50).setMax(50).setMaxDelta(2).setChangeChance(1.0)
      .setUnit("raw")
  }

  def makeCommandSim(name: String, resp: Commands.CommandStatus) = {
    SimMapping.CommandSim.newBuilder
      .setName(name)
      .setResponseStatus(resp)
  }

  def getConfigFiles(index: SimMapping.SimulatorMapping = simpleMapping) = {
    Model.ConfigFile.newBuilder().setName("mapping")
      .setMimeType("application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping")
      .setFile(index.toByteString).build :: Nil
  }

  def getCmdRequest(name: String) = {
    Commands.CommandRequest.newBuilder.setName(name).build
  }

  class QueueingPublisher[A] extends Publisher[A] {
    val queue = new scala.collection.mutable.Queue[A]
    def publish(a: A) = {
      queue.enqueue(a)
      new FixedPromise(true)
    }
  }

  class BatchPublisher extends QueueingPublisher[Measurements.MeasurementBatch]
  class ResponsePublisher extends QueueingPublisher[Commands.CommandResponse]

  def fixture(test: (MockExecutor, Protocol, BatchPublisher, ResponsePublisher) => Unit) = {
    val exe = new MockExecutor
    val protocol = new BenchmarkProtocol(exe)
    val batch = new BatchPublisher
    val responses = new ResponsePublisher
    test(exe, protocol, batch, responses)
  }

  val endpointName = "endpoint"

  test("add/remove") {
    fixture { (exe, protocol, batch, responses) =>

      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher)
      batch.queue.size should equal(0)
      exe.executeNext(2, 1) // integrity poll
      batch.queue.size should equal(1)
      batch.queue.front.getMeasCount() should equal(2) // integrity poll
      exe.repeatNext(1, 1) should equal(100) // random measurement at 100 ms interval

      protocol.removeEndpoint(endpointName)

    }
  }

  test("command responded to") {
    fixture { (exe, protocol, batch, responses) =>

      val cmd = protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher)

      cmd.issue(getCmdRequest("success"), responses)
      responses.queue.size should equal(1)
      responses.queue.dequeue().getStatus should equal(Commands.CommandStatus.SUCCESS)

      cmd.issue(getCmdRequest("fail"), responses)
      responses.queue.size should equal(1)
      responses.queue.dequeue().getStatus should equal(Commands.CommandStatus.HARDWARE_ERROR)

      protocol.removeEndpoint(endpointName)
    }
  }

  test("Adding twice causes exception") {
    fixture { (exe, protocol, batch, responses) =>
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher)
      intercept[IllegalStateException] {
        protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher)
      }
    }
  }

  test("Removing unknown causes exception") {
    fixture { (exe, protocol, batch, responses) =>
      // need to call the NVII functions or else we are only testing the BaseProtocol code
      intercept[IllegalStateException] {
        protocol.removeEndpoint(endpointName)
      }
    }
  }
}