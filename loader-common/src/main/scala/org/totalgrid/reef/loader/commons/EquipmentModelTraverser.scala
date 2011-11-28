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
package org.totalgrid.reef.loader.commons

import org.totalgrid.reef.proto.Model._
import org.totalgrid.reef.proto.FEP.{ CommChannel, CommEndpointConfig }

import scala.collection.JavaConversions._

import com.google.protobuf.GeneratedMessage

/**
 * trait the UI can implement to see the "tree" of objects as we walk them
 */
trait TraversalProgressNotifier {
  def display(entity: Entity, depth: Int)
}

/**
 * Given a starting entity we will traverse down the "owns" relationships looking for equipment, points and
 * commands. From each point and command it will backtrack up the "source" relationships looking for the source
 * endpoints and channels of those points/commands. Finally it looks at all of configFiles for those with "used"
 * relationship to any of our imported entities.
 */
class EquipmentModelTraverser(client: LoaderServices, collector: ModelCollector, notifier: Option[TraversalProgressNotifier]) {
  private val seenEntities = scala.collection.mutable.Map.empty[String, Entity]

  def collect(entity: Entity) {
    // start traversal at the root
    getEntity(entity.getUuid, 0)
  }

  def finish() {
    // much more efficient to grab all config files and filter off ones we don't know
    // versus asking for a config file for every entity
    client.getAllConfigFiles.await.toList.foreach { cf =>
      val users = cf.getEntitiesList.toList
      val knownUsers = users.filter(u => seenEntities.get(u.getUuid.getValue).isDefined)
      if (!knownUsers.isEmpty) {
        val cfEntity = client.getEntityByUid(cf.getUuid).await
        notifier.foreach { _.display(cfEntity, 0) }
        collector.addConfigFile(cf, cfEntity)
        knownUsers.foreach { collector.addEdge(_, cfEntity, "uses") }
      }
    }
  }

  private def handleEntity(entity: Entity, depth: Int): Entity = {
    seenEntities.put(entity.getUuid.getValue, entity)

    notifier.foreach { _.display(entity, depth) }

    getConcreteObject(client, entity) match {

      case point: Point =>
        collector.addPoint(point, entity)
        val commands = client.getFeedbackCommands(entity.getUuid).await.toList
        commands.foreach { c =>
          getEntity(c.getUuid, depth + 1) // load commands so we can set the feedback relationship immediatley
          collector.addEdge(entity, c, "feedback")
        }
        if (point.hasEndpoint) {
          val endpoint = getEntity(point.getEndpoint.getUuid, depth + 1)
          collector.addEdge(endpoint, entity, "source")
        }

      case command: Command =>
        collector.addCommand(command, entity)
        if (command.hasEndpoint) {
          val endpoint = getEntity(command.getEndpoint.getUuid, depth + 1)
          collector.addEdge(endpoint, entity, "source")
        }

      case endpoint: CommEndpointConfig =>
        // add channel first, adding endpoint will automatically create channel with wrong uuid
        if (endpoint.hasChannel) getEntity(endpoint.getChannel.getUuid, depth + 1)
        collector.addEndpoint(endpoint, entity)

      case channel: CommChannel =>
        collector.addChannel(channel, entity)

      case configFile: ConfigFile =>
        collector.addConfigFile(configFile, entity)

      case entity: Entity =>
        collector.addEquipment(entity)
        val relatedEntities = client.getEntityImmediateChildren(entity.getUuid, "owns").await
        relatedEntities.foreach { child =>
          traverseEquipment(child, depth + 1)
          collector.addEdge(entity, child, "owns")
        }
    }

    entity
  }

  /**
   * loads the "concrete" object associated with the entity or fails. This cost us an extra
   * dynamic cast (match statement) but makes the code more straightforward
   */
  private def getConcreteObject(client: LoaderServices, entity: Entity): GeneratedMessage = {

    val types = entity.getTypesList.toList

    if (types.find(_ == "Point").isDefined) {
      client.getPointByUid(entity.getUuid).await
    } else if (types.find(_ == "Command").isDefined) {
      client.getCommandByName(entity.getName).await
    } else if (types.find(_ == "CommunicationEndpoint").isDefined) {
      client.getEndpoint(entity.getUuid).await
    } else if (types.find(_ == "Channel").isDefined) {
      client.getCommunicationChannel(entity.getUuid).await
    } else if (types.find(_ == "ConfigurationFile").isDefined) {
      client.getConfigFileByUid(entity.getUuid).await
    } // TODO: do we want a "child having" type?
    else if (types.find(t => t == "Equipment" || t == "EquipmentGroup" || t == "Site" || t == "Root" || t == "Region").isDefined) {
      entity
    } else {
      throw new Exception("Unknown concrete type for: " + entity)
    }
  }

  // load the full entity from the system (populate all fields), we need to explicitly go and
  // retrieve the full object because we can't reply on the system to give us fully populated
  // entities for linked objects (configfiles inside endpoints just have name and uuid).
  private def getEntity(logicalNode: ReefUUID, depth: Int) = {
    val cached = seenEntities.get(logicalNode.getValue)
    if (!cached.isDefined) traverseEquipment(client.getEntityByUid(logicalNode).await, depth)
    else cached.get
  }

  private def traverseEquipment(entity: Entity, depth: Int): Entity = {
    val cached = seenEntities.get(entity.getUuid.getValue)
    if (!cached.isDefined) handleEntity(entity, depth)
    else cached.get
  }
}