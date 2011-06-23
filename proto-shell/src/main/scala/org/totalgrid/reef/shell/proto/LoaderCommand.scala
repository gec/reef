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
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Argument, Command, Option => GogoOption }
import org.totalgrid.reef.proto.Processing.TriggerSet
import org.totalgrid.reef.proto.Model.Point

import presentation.TriggerView
import RequestFailure._

@Command(scope = "reef", name = "load", description = "Loads equipment and communication models")
class LoadConfigCommand extends ReefCommandSupport {

  @GogoOption(name = "-benchmark", aliases = Array[String](), description = "Override endpoint protocol to force all endpoints in configuration file to be simulated", required = false, multiValued = false)
  var benchmark = false

  @GogoOption(name = "-dryRun", description = "Just analyze file, don't actually send data to reef", required = false, multiValued = false)
  var dryRun = false

  @GogoOption(name = "-d", description = "Delete all objects in configuration file from server", required = false, multiValued = false)
  var delete = false

  @GogoOption(name = "-ignoreWarnings", description = "Still attempt upload even if configuration is invalid", required = false, multiValued = false)
  var ignoreWarnings = false

  @Argument(index = 0, name = "configFile", description = "Configuration file name with path", required = true, multiValued = false)
  var configFile: String = null

  override def doCommand(): Unit = {
    import org.totalgrid.reef.loader.LoadManager
    LoadManager.loadFile(reefSession, configFile, benchmark, dryRun, ignoreWarnings, !delete)
  }

}

@Command(scope = "trigger", name = "trigger", description = "Lists triggers")
class TriggerCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Point Name", description = "Point name.", required = false, multiValued = false)
  var pointName: String = null

  def doCommand() = {
    Option(pointName) match {
      case Some(entId) =>
        val point = services.getPointByName(pointName)
        val trigger = interpretAs("Trigger set not found.") {
          reefSession.get(TriggerSet.newBuilder.setPoint(point).build).await().expectOne
        }
        TriggerView.inspectTrigger(trigger)
      case None =>
        val triggers = interpretAs("No trigger sets found.") {
          reefSession.get(TriggerSet.newBuilder.setPoint(Point.newBuilder.setName("*")).build).await().expectMany()
        }
        TriggerView.printTable(triggers)
    }
  }

}