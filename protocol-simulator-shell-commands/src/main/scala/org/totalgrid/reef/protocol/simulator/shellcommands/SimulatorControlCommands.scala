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
package org.totalgrid.reef.protocol.simulator.shellcommands

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }

import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.util.Table
import org.totalgrid.reef.protocol.simulator.ControllableSimulator

import scala.collection.JavaConversions._

trait SimulatorControlCommands { self: OsgiCommandSupport =>

  def getSimulators(): Seq[ControllableSimulator] = {
    getBundleContext findServices withInterface[ControllableSimulator] andApply { x: ControllableSimulator => x }
  }

  def displaySimulators(sims: Seq[ControllableSimulator]) = {
    Table.printTable(headers, rows(sims))
  }

  def headers = "Endpoint" :: "SimType" :: "SimLevel" :: "UpdateRate" :: Nil

  def rows(simMap: Seq[ControllableSimulator]) = {
    simMap.map { sim =>
      sim.name ::
        sim.factory.name ::
        sim.simLevel.toString ::
        sim.getRepeatDelay.toString ::
        Nil
    }.toList
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

  @GogoOption(name = "-time", description = "Configure the time delay (in milliseconds) between generated measurements. If no endpoints are specified, all endpoints are configured.", required = true, multiValued = false)
  var timeDelay: String = null

  @Argument(index = 0, name = "endpoints", description = "Optional list of endpoints. Example: sim:config -time 200 substation1", required = false, multiValued = true)
  var javaEndpoints: java.util.List[String] = null

  override protected def doExecute(): Object = {

    def adjust(sim: ControllableSimulator) = {
      try {
        sim.setUpdateDelay(timeDelay.toLong)
      } catch {
        case ex: NumberFormatException => throw new Exception("ERROR: -time " + timeDelay + " is not a number.")
      }
    }

    var sims = getSimulators()

    val names = Option(javaEndpoints).map { _.toList }.getOrElse(Nil)

    if (!names.isEmpty) sims = sims.filterNot(s => names.find(_ == s.name).isEmpty)

    sims.foreach(adjust)

    displaySimulators(sims)

    null
  }

}
