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
import scala.collection.JavaConversions._

import org.totalgrid.reef.reactor.ReactActor
import org.totalgrid.reef.proto.{ FEP, SimMapping, Model }
import org.totalgrid.reef.util.{ Logging }

import org.totalgrid.reef.protocol.api.{ IProtocol, BaseProtocol }

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.apache.felix.gogo.commands

trait SimulatorManagement {
  def displayEndpoints(endpoints: List[String])
  def configEndpoints(endpoints: List[String], delay: String)
}

class BenchmarkProtocol extends BaseProtocol with SimulatorManagement with Logging {

  val name: String = "benchmark"
  val requiresPort = false

  private var map = immutable.Map.empty[String, Simulator]
  private var reactor: Option[ReactActor] = None

  override def displayEndpoints(endpoints: List[String]) = {

    val displayMap = if (endpoints.isEmpty)
      map
    else
      map.filterKeys(k => endpoints.contains(k))

    println("Simulated Endpoints with time delay:")
    for ((eName, simEndpoint) <- displayMap) {
      println("    " + eName + ": " + simEndpoint.getRepeatDelay)
    }
  }

  override def configEndpoints(endpoints: List[String], delay: String) = {

    if (!map.isEmpty) {

      //println("Simulated Endpoints:")
      //println(map.keys.mkString("    ", "\n    ", "\n"))

      try {
        if (delay.isEmpty) {
          println("ERROR: The time argument is an empty string.")
        } else {
          val d: Long = if (delay.charAt(0) == '+')
            delay.substring(1).toLong
          else
            delay.toLong

          if (d == 0) {
            println("ERROR: time argument cannot be 0.")
          } else {
            val updateEndpoints = if (endpoints.isEmpty)
              map
            else
              map.filterKeys(k => endpoints.contains(k))

            println("Updated Endpoints:")
            if (delay.startsWith("-") || delay.startsWith("+")) {

              for ((eName, simEndpoint) <- updateEndpoints) {
                simEndpoint.adjustUpdateParams(d)
                println("    " + eName + ": " + simEndpoint.getRepeatDelay)
              }
            } else {

              for ((eName, simEndpoint) <- updateEndpoints) {
                simEndpoint.setUpdateParams(d)
                println("    " + eName + ": " + simEndpoint.getRepeatDelay)
              }
            }
          }
        }

      } catch {
        case ex: NumberFormatException => println("ERROR: -time " + delay + " is not a number.")
        case ex2 => throw ex2
      }

    } else {
      println("No simulated endpoints found.")
    }

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

  def _addEndpoint(endpoint: String, port: String, files: List[Model.ConfigFile], publish: IProtocol.Publish, respond: IProtocol.Respond): IProtocol.Issue = {

    if (map.get(endpoint).isDefined) throw new IllegalArgumentException("Trying to re-add endpoint" + endpoint)

    val mapping = SimMapping.SimulatorMapping.parseFrom(IProtocol.find(files, "application/vnd.google.protobuf; proto=reef.proto.SimMapping.SimulatorMapping").getFile)
    val sim = new Simulator(endpoint, publish, respond, mapping, getReactor)
    sim.start
    map += endpoint -> sim
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

/**
 *
 */
@commands.Command(scope = "sim", name = "list", description = "List simulated endpoints. List all with no arguments.")
class SimulatorList(simulator: SimulatorManagement) extends OsgiCommandSupport {

  @commands.Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:list substation1", required = false, multiValued = true)
  private var endpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {
    println("")
    val ep = if (endpoints == null)
      List[String]()
    else
      endpoints.toList

    simulator.displayEndpoints(ep)

    null
  }

}

/**
 *
 */
@commands.Command(scope = "sim", name = "config", description = "Configure endpoint simulation properties. Example: sim:config -time 5")
class SimulatorConfig(simulator: SimulatorManagement) extends OsgiCommandSupport {

  //commands.Option(name = "-time", description = "Configure the time delay (in milliseconds) between generated measurements. If no endpoints are specified, all endpoints will be configured. Syntax: delay [+-]<number>. Example: 'sim config time -5' will shrink the current time delay on each point by 5ms. '5' will set the time delay to 5ms.", required = false, multiValued = true)
  @commands.Option(name = "-time", description = "Configure the time delay (in milliseconds) between generated measurements. If no endpoints are specified, all endpoints are configured. Prefixing the number with '+' or '-' will increase/decrease each endoint's current time delay by that amount.", required = false, multiValued = true)
  private var timeDelay: java.util.List[String] = null

  @commands.Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:config substation1 -time 200", required = false, multiValued = true)
  private var endpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {
    println("")
    val ep = if (endpoints == null)
      List[String]()
    else
      endpoints.toList

    if (timeDelay == null || timeDelay.size != 1)
      println("ERROR: sim:config requires one 'time' argument.")
    else
      simulator.configEndpoints(ep, timeDelay.get(0))

    null
  }
}
