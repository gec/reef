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
import org.totalgrid.reef.client.service.proto.Processing.TriggerSet
import org.totalgrid.reef.client.service.proto.Model.Point

import presentation.TriggerView

import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.commons.{ ModelDeleter, LoaderServicesImpl }
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.client.service.ClientOperations

@Command(scope = "reef", name = "load", description = "Loads equipment and communication models")
class LoadConfigCommand extends ReefCommandSupport {

  @GogoOption(name = "-benchmark", aliases = Array[String](), description = "Override endpoint protocol to force all endpoints in configuration file to be simulated", required = false, multiValued = false)
  var benchmark = false

  @GogoOption(name = "-dryRun", description = "Just analyze file, don't actually send data to reef", required = false, multiValued = false)
  var dryRun = false

  @GogoOption(name = "-ignoreWarnings", description = "Still attempt upload even if configuration is invalid", required = false, multiValued = false)
  var ignoreWarnings = false

  @GogoOption(name = "-batchSize", description = "Upload batch size, 0 disables all batching", required = false, multiValued = false)
  var batchSize = 25

  @Argument(index = 0, name = "configFile", description = "Configuration file name with path", required = true, multiValued = false)
  var configFile: String = null

  override def doCommand(): Unit = {
    val loaderServices = new LoaderServicesImpl(reefClient)

    reefClient.setHeaders(reefClient.getHeaders.setTimeout(30000))
    LoadManager.loadFile(loaderServices, configFile, benchmark, dryRun, ignoreWarnings, batchSize)
  }
}

@Command(scope = "reef", name = "unload", description = "Remove all equipment, endpoints and messages")
class UnloadConfigCommand extends ReefCommandSupport {

  @GogoOption(name = "-batchSize", description = "Upload batch size, 0 disables all batching.", required = false, multiValued = false)
  var batchSize = 0

  @GogoOption(name = "--force", description = "Force unloading even if endpoints are still marked as COMMS_UP", required = false, multiValued = false)
  var force = false

  override def doCommand(): Unit = {
    val loaderServices = new LoaderServicesImpl(reefClient)

    reefClient.setHeaders(reefClient.getHeaders.setTimeout(30000))
    ModelDeleter.deleteEverything(loaderServices, false, force, Some(Console.out), batchSize)
  }

}

@Command(scope = "reef", name = "unload-children", description = "Delete a subset of the model starting with the indicated root nodes. The deleter will traverse the " +
  "model starting at the root nodes and delete all child Equipment, Points and Commands. If the root node is an Endpoint then all of its \"sourced\" Points and Commands " +
  "will be removed as well. If the root is a piece of Equipment all Points and Commands it \"owns\" will be delete. If all Points and Commands for an Endpoint are removed " +
  "we will also remove the Endpoint itself.  We also check all of the ConfigFiles in the system, any that were \"uses\" only by removed entities will be deleted as well. " +
  "If a Point is deleted all of the commands it is \"feedback\" for will also be deleted.")
class UnloadChildrenConfigCommand extends ReefCommandSupport {

  @GogoOption(name = "-batchSize", description = "Upload batch size, 0 disables all batching.", required = false, multiValued = false)
  var batchSize = 0

  @Argument(index = 0, name = "roots", description = "Names of parent nodes to delete", required = true, multiValued = true)
  var roots: java.util.List[String] = null

  override def doCommand(): Unit = {
    val loaderServices = new LoaderServicesImpl(reefClient)
    reefClient.setHeaders(reefClient.getHeaders.setTimeout(30000))

    val rootEntities = roots.toList.map { loaderServices.getEntityByName(_) }

    ModelDeleter.deleteChildren(loaderServices, roots.toList, false, false, Some(Console.out), batchSize) { (_, _) =>
      // dont delete anything extra
    }
  }
}

@Command(scope = "trigger", name = "trigger", description = "Lists triggers")
class TriggerCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "Point Name", description = "Point name.", required = false, multiValued = false)
  var pointName: String = null

  def doCommand() = {
    val ops = reefClient.getService(classOf[ClientOperations])
    Option(pointName) match {
      case Some(entId) =>
        val point = services.getPointByName(pointName)
        val trigger = ops.getOne(TriggerSet.newBuilder.setPoint(point).build)
        TriggerView.inspectTrigger(trigger)
      case None =>
        val triggers = ops.getMany(TriggerSet.newBuilder.setPoint(Point.newBuilder.setName("*")).build)
        TriggerView.printTable(triggers.toList)
    }
  }

}