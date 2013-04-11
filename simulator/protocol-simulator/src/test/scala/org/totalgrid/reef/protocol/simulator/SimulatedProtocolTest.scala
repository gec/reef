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
package org.totalgrid.reef.protocol.simulator

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.service.proto.{ Model, SimMapping, Measurements, Commands }
import org.totalgrid.reef.protocol.api.{ NullEndpointPublisher, Publisher }
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.executor4s.Executor
import java.lang.Exception
import org.totalgrid.reef.client.service.proto.Model.Command
import org.mockito.Mockito
import org.totalgrid.reef.client.Client

@RunWith(classOf[JUnitRunner])
class SimulatedProtocolTest extends FunSuite with ShouldMatchers {

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
    Commands.CommandRequest.newBuilder.setCommand(Command.newBuilder.setName(name)).build
  }

  class QueueingPublisher[A] extends Publisher[A] {
    val queue = new scala.collection.mutable.Queue[A]
    def publish(a: A) = {
      queue.enqueue(a)
    }
  }

  class BatchPublisher extends QueueingPublisher[Measurements.MeasurementBatch]
  class ResponsePublisher extends QueueingPublisher[Commands.CommandStatus]

  def fixture(test: (MockExecutor, SimulatedProtocol, BatchPublisher, ResponsePublisher) => Unit) = {
    val exe = new MockExecutor
    val protocol = new SimulatedProtocol(exe)
    val batch = new BatchPublisher
    val responses = new ResponsePublisher
    test(exe, protocol, batch, responses)
  }

  class MockSimulatorFactory(simLevel: Int) extends SimulatorPluginFactory {

    var map = Map.empty[String, MockSimPlugin]

    def name: String = "MockSimulatorFactory"

    def getSimLevel(endpointName: String, config: SimMapping.SimulatorMapping): Int = simLevel

    def create(endpointName: String, executor: Executor, publisher: Publisher[Measurements.MeasurementBatch], config: SimMapping.SimulatorMapping): SimulatorPlugin = {
      val mock = new MockSimPlugin(this)
      map += endpointName -> mock
      mock
    }

    class MockSimPlugin(parent: MockSimulatorFactory) extends SimulatorPlugin {
      var response = Commands.CommandStatus.SUCCESS

      def name = "mock"
      def factory: SimulatorPluginFactory = parent
      def simLevel: Int = 0
      def issue(cr: Commands.CommandRequest): Commands.CommandStatus = response
      def shutdown() = map.find(x => x._2.equals(this)) match {
        case Some((name, plugin)) =>
          map -= name
        case None => throw new Exception("Plugin not found")
      }
    }
  }

  val client = Mockito.mock(classOf[Client])
  val endpointName = "endpoint"

  test("add endpoint first") {
    fixture { (exe, protocol, batch, responses) =>
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      val fac = new MockSimulatorFactory(0)
      protocol.addPluginFactory(fac)
      fac.map.size should equal(1)
      protocol.removeEndpoint(endpointName)
      fac.map.size should equal(0)
    }
  }

  test("add simulator first") {
    fixture { (exe, protocol, batch, responses) =>
      val fac = new MockSimulatorFactory(0)
      protocol.addPluginFactory(fac)
      fac.map.size should equal(0)
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      fac.map.size should equal(1)
      protocol.removeEndpoint(endpointName)
      fac.map.size should equal(0)
    }
  }

  test("Removing factory destroys all simulators") {
    fixture { (exe, protocol, batch, responses) =>
      val fac = new MockSimulatorFactory(0)
      protocol.addPluginFactory(fac)
      fac.map.size should equal(0)
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      fac.map.size should equal(1)
      protocol.removePluginFactory(fac)
      fac.map.size should equal(0)
    }
  }

  test("Uses highest sim level") {
    fixture { (exe, protocol, batch, responses) =>
      val fac1 = new MockSimulatorFactory(1)
      val fac2 = new MockSimulatorFactory(2)
      val fac3 = new MockSimulatorFactory(3)
      protocol.addPluginFactory(fac1)
      protocol.addPluginFactory(fac2)
      protocol.addPluginFactory(fac3)

      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      fac1.map.size should equal(0)
      fac2.map.size should equal(0)
      fac3.map.size should equal(1)
    }
  }

  test("Correctly substitues for highest sim level") {
    fixture { (exe, protocol, batch, responses) =>
      val fac1 = new MockSimulatorFactory(1)
      val fac2 = new MockSimulatorFactory(2)

      protocol.addPluginFactory(fac1)
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      fac1.map.size should equal(1)
      fac2.map.size should equal(0)

      protocol.addPluginFactory(fac2)
      fac1.map.size should equal(0)
      fac2.map.size should equal(1)

      protocol.removePluginFactory(fac2)
      fac1.map.size should equal(1)
      fac2.map.size should equal(0)
    }
  }

  test("command responded to without plugin") {
    fixture { (exe, protocol, batch, responses) =>

      val cmd = protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)

      cmd.issue(getCmdRequest("success"), responses)
      responses.queue.size should equal(1)
      responses.queue.dequeue() should equal(Commands.CommandStatus.NOT_SUPPORTED)

      protocol.addPluginFactory(new MockSimulatorFactory(1))
      cmd.issue(getCmdRequest("success"), responses)
      responses.queue.size should equal(1)
      responses.queue.dequeue() should equal(Commands.CommandStatus.SUCCESS)
    }
  }

  test("Adding twice causes exception") {
    fixture { (exe, protocol, batch, responses) =>
      protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
      intercept[IllegalStateException] {
        protocol.addEndpoint(endpointName, "", getConfigFiles(), batch, NullEndpointPublisher, client)
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