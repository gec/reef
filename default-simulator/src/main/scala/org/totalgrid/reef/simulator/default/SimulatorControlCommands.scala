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
package org.totalgrid.reef.simulator.default

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.apache.felix.gogo.commands.{ Argument, Command, Option }

/*
abstract class SimulatorControlCommands(simulator: SimulatorManagement) extends OsgiCommandSupport {

  //var javaEndpoints: java.util.List[String]

  import scala.collection.JavaConversions._
  // helper function to turn java List into scala List
  def endpoints(): List[String] = scala.Option(javaEndpoints).map(_.toList).getOrElse(Nil)

  def getSimulatorsList() = simulator.getSimulators(endpoints).values

  def printSimulators() = {
    val sims = simulator.getSimulators(endpoints)

    println("")
    if (sims.isEmpty) {
      println("No simulated endpoints found.")
    } else {
      println("Simulated Endpoints with time delay:")

      for ((eName, simEndpoint) <- sims) {
        println("    " + eName + ": " + simEndpoint.getRepeatDelay)
      }
    }
  }
}

@Command(scope = "sim", name = "list", description = "List simulated endpoints. List all with no arguments.")
class SimulatorList(simManager: SimulatorManagement) extends SimulatorControlCommands(simManager) {

  @Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:list substation1", required = false, multiValued = true)
  var javaEndpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {

    printSimulators()

    null
  }
}

@Command(scope = "sim", name = "config", description = "Configure endpoint simulation properties. Example: sim:config -time 5")
class SimulatorConfig(simManager: SimulatorManagement) extends SimulatorControlCommands(simManager) {

  @Option(name = "-time", description = "Configure the time delay (in milliseconds) between generated measurements. If no endpoints are specified, all endpoints are configured. Prefixing the number with '+' or '-' will increase/decrease each endoint's current time delay by that amount.", required = true, multiValued = false)
  var timeDelay: String = null

  @Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:config substation1 -time 200", required = false, multiValued = true)
  var javaEndpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {

    try {
      timeDelay match {
        case inc if inc.startsWith("+") => adjustTime(inc.substring(1).toLong)
        case dec if dec.startsWith("-") => adjustTime(-dec.substring(1).toLong)
        case other: String => setTime(other.toLong)
      }
    } catch {
      case ex: NumberFormatException => println("ERROR: -time " + timeDelay + " is not a number.")
    }

    printSimulators()

    null
  }

  private def adjustTime(adjust: Long) {
    getSimulatorsList.foreach { sim =>
      // minimum delay is 0
      val adjustedDelay = (sim.getRepeatDelay + adjust).max(0)
      sim.setUpdateParams(adjustedDelay)
    }
  }

  private def setTime(delay: Long) {
    getSimulatorsList.foreach { sim => sim.setUpdateParams(delay) }
  }
}
*/ 