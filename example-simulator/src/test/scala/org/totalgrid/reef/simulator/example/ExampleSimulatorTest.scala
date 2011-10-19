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
package org.totalgrid.reef.api.protocol.simulator

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.api.proto.{ Commands, SimMapping, Measurements }
import org.totalgrid.reef.simulator.example.ExampleSimulatorFactory
import org.totalgrid.reef.executor.mock.MockExecutor
import org.totalgrid.reef.api.protocol.api.Publisher
import org.totalgrid.reef.api.proto.Measurements.MeasurementBatch

@RunWith(classOf[JUnitRunner])
class ExampleSimulatorTest extends FunSuite with ShouldMatchers {

  def simpleMapping(name: String = "breaker.kW_a") = SimMapping.SimulatorMapping.newBuilder
    .setDelay(100)
    .addMeasurements(makeAnalogSim(name))
    .addMeasurements(makeAnalogSim("breaker.kW_b"))
    .addMeasurements(makeAnalogSim("breaker.kW_c"))
    .addMeasurements(makeStatusSim("breaker.Status"))
    .addCommands(makeCommandSim("breaker.Status.Trip"))
    .addCommands(makeCommandSim("breaker.Status.Close"))
    .build

  def makeAnalogSim(name: String) = {
    SimMapping.MeasSim.newBuilder
      .setName(name)
      .setType(Measurements.Measurement.Type.DOUBLE)
      .setInitial(0).setMin(-50).setMax(50).setMaxDelta(2).setChangeChance(1.0)
      .setUnit("raw")
  }

  def makeStatusSim(name: String) = {
    SimMapping.MeasSim.newBuilder
      .setName(name)
      .setType(Measurements.Measurement.Type.BOOL)
      .setUnit("status")
  }

  def makeCommandSim(name: String) =
    SimMapping.CommandSim.newBuilder.setName(name).setResponseStatus(Commands.CommandStatus.SUCCESS)

  def buildCommand(name: String) =
    Commands.CommandRequest.newBuilder.setType(Commands.CommandRequest.ValType.NONE).setName(name).build()

  test("Correctly identifies endpoint") {
    ExampleSimulatorFactory.getSimLevel("test", simpleMapping("breaker.kW_a")) should equal(1)
    ExampleSimulatorFactory.getSimLevel("test", simpleMapping("foobar")) should equal(-1)
    ExampleSimulatorFactory.getSimLevel("test", simpleMapping("breaker.prefixkW_a")) should equal(-1)
  }

  test("Publishes measurements when created") {
    val exe = new MockExecutor
    val pub = new Publisher[MeasurementBatch] {
      val queue = new scala.collection.mutable.Queue[MeasurementBatch]
      def publish(batch: MeasurementBatch) = {
        queue.enqueue(batch)
      }
    }

    def validateState() = {
      pub.queue.size should equal(0)
      exe.executeNext(1, 0)
      pub.queue.size should equal(1)
      pub.queue.dequeue().getMeasCount() should equal(4)
    }

    val sim = ExampleSimulatorFactory.createSimulator("test", exe, pub, simpleMapping())
    validateState()

    sim.issue(buildCommand("breaker.Status.Trip")) should equal(Commands.CommandStatus.SUCCESS)
    validateState()

    sim.issue(buildCommand("breaker.Status.Close")) should equal(Commands.CommandStatus.SUCCESS)
    validateState()

    sim.issue(buildCommand("foobar")) should equal(Commands.CommandStatus.NOT_SUPPORTED)
    pub.queue.isEmpty should equal(true)
    exe.numActionsPending should equal(0)
  }

}