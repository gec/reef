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

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }

import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.util.Table

trait SimulatorControlCommands { self: OsgiCommandSupport =>

  def getSimulators(): Map[String, Option[SimulatorPlugin]] = {
    val simulators = getBundleContext findServices withInterface[SimulatorManagement] andApply { (manager: SimulatorManagement, properties: Props) =>
      manager.getSimulators
    }
    simulators.reduce(_ ++ _)
  }

  def displaySimulators(sims: Map[String, Option[SimulatorPlugin]]) = {
    Table.printTable(headers, rows(sims))
  }

  def headers = "Endpoint" :: "SimType" :: "SimLevel" :: "UpdateRate" :: Nil

  def rows(simMap: Map[String, Option[SimulatorPlugin]]) = {
    simMap.map {
      case (name, sim) =>
        name ::
          sim.map { _.factory.name }.getOrElse("None") ::
          sim.map { _.simLevel.toString }.getOrElse("-") ::
          sim.map { getRate(_).getOrElse("-") }.getOrElse("-").toString ::
          Nil
    }.toList
  }

  def getRate(sim: SimulatorPlugin) = {
    sim match {
      case c: ControllableSimulator => Some(c.getRepeatDelay)
      case _ => None
    }
  }
}

@Command(scope = "sim", name = "list", description = "List currently simulated endpoints")
class SimulatorList extends OsgiCommandSupport with SimulatorControlCommands {

  override protected def doExecute(): Object = {

    displaySimulators(getSimulators)

    null
  }
}

@Command(scope = "sim", name = "config", description = "Configure benchmark endpoint simulation properties. Example: sim:config -time 5")
class SimulatorConfig extends OsgiCommandSupport with SimulatorControlCommands {

  @GogoOption(name = "-time", description = "Configure the time delay (in milliseconds) between generated measurements. If no endpoints are specified, all endpoints are configured. Prefixing the number with '+' or '-' will increase/decrease each endoint's current time delay by that amount.", required = true, multiValued = false)
  var timeDelay: String = null

  @Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:config -time 200 substation1", required = false, multiValued = true)
  var javaEndpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {

    val adjustmentFunction: (ControllableSimulator => Unit) = try {
      timeDelay match {
        case inc if inc.startsWith("+") => adjustTime(_, inc.substring(1).toLong)
        case dec if dec.startsWith("-") => adjustTime(_, -dec.substring(1).toLong)
        case other: String => setTime(_, other.toLong)
      }
    } catch {
      case ex: NumberFormatException => throw new Exception("ERROR: -time " + timeDelay + " is not a number.")
    }

    val sims = getSimulators().filter { case (n, s) => s.isDefined && s.get.isInstanceOf[ControllableSimulator] }

    sims.foreach { case (n, s) => adjustmentFunction(s.get.asInstanceOf[ControllableSimulator]) }

    displaySimulators(sims)

    null
  }

  private def adjustTime(sim: ControllableSimulator, adjust: Long) {
    val adjustedDelay = (sim.getRepeatDelay + adjust).max(0)
    sim.setUpdateParams(adjustedDelay)
  }

  private def setTime(sim: ControllableSimulator, delay: Long) {
    sim.setUpdateParams(delay)
  }
}
