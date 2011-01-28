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
import org.totalgrid.reef.proto.{ FEP, Mapping, Model }
import org.totalgrid.reef.util.{ Logging }

import org.totalgrid.reef.protocol.api.{ IProtocol, BaseProtocol }

class BenchmarkProtocol extends BaseProtocol with Logging {

  val name: String = "benchmark"
  val requiresPort = false

  protected var rate = 100
  protected var batchSize = 10

  private var map = immutable.Map.empty[String, Simulator]
  private var reactor: Option[ReactActor] = None

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

  /// rebalance the message load across the simulators
  def balance(): Unit = {
    if (map.size > 0) {
      val ratePerSim = rate / map.size / batchSize
      if (ratePerSim > 0) {
        val delay = (1000.0 / ratePerSim).toInt
        if (delay > 0) {
          map.values.foreach { sim =>
            sim.setUpdateParams(delay, batchSize)
          }
        } else {
          warn { "Non-positive delay" }
        }
      } else {
        warn { "Non-positive endpoint rate" }
      }
    } else {
      warn { "No devices to rebalance" }
    }
  }

  def _addPort(p: FEP.Port) = {}
  def _removePort(port: String) = {}

  def _addEndpoint(endpoint: String, port: String, files: List[Model.ConfigFile], publish: IProtocol.Publish, respond: IProtocol.Respond): IProtocol.Issue = {

    if (map.get(endpoint).isDefined) throw new IllegalArgumentException("Trying to re-add endpoint" + endpoint)

    val mapping = Mapping.IndexMapping.parseFrom(IProtocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.Mapping.IndexMapping").getFile)
    val sim = new Simulator(endpoint, publish, respond, mapping, getReactor)
    sim.start
    map += endpoint -> sim
    balance()
    sim.issue
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