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

import scala.collection.immutable

import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.proto.{ FEP, SimMapping, Model }
import org.totalgrid.reef.util.{ Logging }

import org.totalgrid.reef.protocol.api.{ IProtocol, IPublisher, ICommandHandler, BaseProtocol }

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
class BenchmarkProtocol extends BaseProtocol with SimulatorManagement with Logging {

  val name: String = "benchmark"
  val requiresPort = false

  private var map = immutable.Map.empty[String, Simulator]
  private var reactor: Option[ReactActor] = None

  def getSimulators(names: List[String]): Map[String, ControllableSimulator] = {
    if (names.isEmpty) map else map.filterKeys(k => names.contains(k))
  }

  // get or create and start the shared reactor
  private def getReactor: ReactActor = {
    reactor match {
      case Some(r) => r
      case None =>
        val r = new ReactActor {}
        r.start
        reactor = Some(r)
        r
    }
  }

  def _addPort(p: FEP.Port) = {}
  def _removePort(port: String) = {}

  def _addEndpoint(endpoint: String, port: String, files: List[Model.ConfigFile], publisher: IPublisher): ICommandHandler = {

    if (map.get(endpoint).isDefined) throw new IllegalArgumentException("Trying to re-add endpoint" + endpoint)

    val mapping = SimMapping.SimulatorMapping.parseFrom(IProtocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping").getFile)
    val sim = new Simulator(endpoint, publisher, mapping, getReactor)
    sim.start
    map += endpoint -> sim
    sim
  }

  def _removeEndpoint(endpoint: String) = {
    map.get(endpoint) match {
      case Some(sim) =>
        sim.stop
        map -= endpoint
      case None =>
        throw new IllegalArgumentException("Trying to remove endpoint" + endpoint)
    }
    if (map.size == 0) {
      reactor.foreach { _.stop }
      reactor = None
    }
  }

}

