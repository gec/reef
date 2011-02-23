/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.benchmark

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.proto.{ Model, SimMapping, Measurements, Commands }
import org.totalgrid.reef.util.SyncVar

@RunWith(classOf[JUnitRunner])
class BenchmarkProtocolTest extends FunSuite with ShouldMatchers {
  def makeSimpleMapping() = {
    SimMapping.SimulatorMapping.newBuilder
      .setDelay(100)
      .addMeasurements(makeAnalogSim())
      .addCommands(makeCommandSim("success", Commands.CommandStatus.SUCCESS))
      .addCommands(makeCommandSim("fail", Commands.CommandStatus.HARDWARE_ERROR))
      .build
  }
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

  def getConfigFiles(index: SimMapping.SimulatorMapping = makeSimpleMapping()) = {
    Model.ConfigFile.newBuilder().setName("mapping")
      .setMimeType("application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping")
      .setFile(index.toByteString).build :: Nil
  }

  def getCmdRequest(name: String) = {
    Commands.CommandRequest.newBuilder.setName(name).build
  }

  class Callbacks {

    val cmdResponses = new SyncVar(None: Option[Commands.CommandResponse])
    val measurements = new SyncVar(List.empty[Measurements.MeasurementBatch])

    def publish(m: Measurements.MeasurementBatch) {
      measurements.atomic(l => (m :: l).reverse)
    }
    def respond(c: Commands.CommandResponse) {
      cmdResponses.update(Some(c))
    }
  }

  val endpointName = "endpoint"

  test("add remove") {
    val protocol = new BenchmarkProtocol
    val cb = new Callbacks
    protocol.addEndpoint(endpointName, "", getConfigFiles(), cb.publish _, cb.respond _)

    cb.measurements.waitFor(_.size > 0)

    protocol.removeEndpoint(endpointName)

    val atStop = cb.measurements.lastValueAfter(50).size

    def check(l: List[Measurements.MeasurementBatch]): Boolean = l.size != atStop
    cb.measurements.waitFor(check _, 100, false) should equal(false)
  }

  test("command responded to") {
    val protocol = new BenchmarkProtocol
    val cb = new Callbacks
    val issue = protocol.addEndpoint(endpointName, "", getConfigFiles(), cb.publish _, cb.respond _)

    issue(getCmdRequest("success"))

    cb.cmdResponses.waitFor(_.map { _.getStatus == Commands.CommandStatus.SUCCESS }.getOrElse(false))

    issue(getCmdRequest("fail"))

    cb.cmdResponses.waitFor(_.map { _.getStatus == Commands.CommandStatus.HARDWARE_ERROR }.getOrElse(false))

    protocol.removeEndpoint(endpointName)
  }

  test("Readding causes exception") {
    val protocol = new BenchmarkProtocol
    val cb = new Callbacks
    // need to call the NVII functions or else we are only testing the BaseProtocol code
    protocol._addEndpoint(endpointName, "", getConfigFiles(), cb.publish _, cb.respond _)
    intercept[IllegalArgumentException] {
      protocol._addEndpoint(endpointName, "", getConfigFiles(), cb.publish _, cb.respond _)
    }
  }

  test("Removing unknown causes exception") {
    val protocol = new BenchmarkProtocol
    // need to call the NVII functions or else we are only testing the BaseProtocol code
    intercept[IllegalArgumentException] {
      protocol._removeEndpoint(endpointName)
    }
  }
}