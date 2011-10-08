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

import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.helpers.SymbolResponseProgressRenderer
import com.google.protobuf.GeneratedMessage

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

@Command(scope = "reef", name = "unload", description = "Remove all equipment, endpoints and messages")
class UnloadConfigCommand extends ReefCommandSupport {

  override def doCommand(): Unit = {

    services.session.modifyHeaders(_.setResultLimit(50000))

    // needed to add explict typing to this list, scala compiler eats up all memory in system and never completes
    val endpoints: List[GeneratedMessage] = services.getAllEndpoints().toList
    val entities: List[GeneratedMessage] = services.getAllEntitiesWithTypes("Equipment" :: "EquipmentGroup" :: Nil).toList
    val channels: List[GeneratedMessage] = services.getAllCommunicationChannels().toList
    val cfs: List[GeneratedMessage] = services.getAllConfigFiles().toList
    // we need to remove the logicalNode since its been deleted by this time and therefore the delete
    // fails because our "request" doesn't match every term
    // TODO: delete should just use uuid or uid searching if set
    val points: List[GeneratedMessage] = services.getAllPoints().toList.map { _.toBuilder.clearLogicalNode.build }
    val commands: List[GeneratedMessage] = services.getCommands().toList.map { _.toBuilder.clearLogicalNode.build }
    val messages: List[GeneratedMessage] = services.getAllEventConfigurations(false).toList

    val protos: List[GeneratedMessage] = endpoints ::: entities ::: channels ::: cfs ::: points ::: commands ::: messages

    val renderer = new SymbolResponseProgressRenderer(Console.out)
    renderer.start(protos.size)
    protos.foreach { p =>
      val result = reefSession.delete(p).await()
      result.expectMany()
      renderer.update(result.status, p)
    }
    renderer.finish
  }

  //  def endpointResources(endpoints: List[CommEndpointConfig]) = {
  //
  //    val ownerships = endpoints.map { e => e.getOwnerships }
  //    val ports = endpoints.map { e => if (e.hasChannel) Some(e.getChannel) else None }.flatten
  //    val configFiles = endpoints.map { e => e.getConfigFilesList.toList }.flatten
  //    val points = ownerships.map { _.getPointsList.toList }.flatten
  //    val commands = ownerships.map { _.getCommandsList.toList }.flatten
  //
  //    endpoints ::: ports ::: configFiles :::
  //      points.map { pName => PointProto.newBuilder.setName(pName).build } :::
  //      commands.map { cName => CommandProto.newBuilder.setName(cName).build }
  //  }
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