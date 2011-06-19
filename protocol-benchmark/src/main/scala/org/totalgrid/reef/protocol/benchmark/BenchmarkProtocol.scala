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

import scala.collection.immutable

import org.totalgrid.reef.proto.{ FEP, SimMapping, Model }
import org.totalgrid.reef.util.{ Logging }

import org.totalgrid.reef.protocol.api._
import org.totalgrid.reef.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.executor.{ Executor, ReactActorExecutor }
import java.lang.{ IllegalStateException, Override }

/**
 * interface the BenchmarkProtocol exposes to the simulator shell commands to get
 * the list of the running simulators
 */
trait SimulatorManagement {
  def getSimulators(names: List[String]): Map[String, ControllableSimulator]
}

/**
 * controllable parts of the simulators for shell adjustment
 */
trait ControllableSimulator {
  def getRepeatDelay: Long

  def setUpdateParams(newDelay: Long)
}

/**
 * Protocol implementation that creates and manages simulators to test system behavior
 * under configurable load.
 */
class BenchmarkProtocol(exe: Executor) extends ChannelIgnoringProtocol with SimulatorManagement with Logging {

  import Protocol._

  final override def name: String = "benchmark"
  final override def requiresChannel = false

  private var map = immutable.Map.empty[String, Simulator]

  def getSimulators(names: List[String]): Map[String, ControllableSimulator] =
    if (names.isEmpty) map else map.filterKeys(k => names.contains(k))

  override def addEndpoint(
    endpoint: String,
    channel: String,
    files: List[Model.ConfigFile],
    batchPublisher: BatchPublisher,
    endpointPublisher: EndpointPublisher): CommandHandler = map.get(endpoint) match {

    case Some(x) =>
      throw new IllegalStateException("Endpoint has already been added: " + endpoint)
    case None =>
      val file = Protocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping").getFile
      val mapping = SimMapping.SimulatorMapping.parseFrom(file)
      val sim = new Simulator(endpoint, batchPublisher, mapping, exe)
      sim.start
      map += endpoint -> sim
      sim
  }

  override def removeEndpoint(endpoint: String) = map.get(endpoint) match {
    case Some(sim) =>
      sim.stop()
      map -= endpoint
    case None =>
      throw new IllegalStateException("Trying to remove endpoint that doesn't exist: " + endpoint)
  }

}

