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

import org.totalgrid.reef.client.service.proto.Model._
import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel, Endpoint }

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
 * @param forDelete if we are deleting elements we want to include only ConfigFiles and Endpoints that we have seen
 *                  every user/linked object. If adding we want to include them if we have seen atleast one linked object
 */
class EquipmentModelTraverser(client: LoaderServices, collector: ModelCollector, forDelete: Boolean, notifier: Option[TraversalProgressNotifier]) {
  private val seenEntities = scala.collection.mutable.Map.empty[String, Entity]

  def collect(entity: Entity) {
    // start traversal at the root
    getEntity(entity.getUuid, 0)
  }

  def finish() {
    // more efficient to load all endpoints and only include endpoints that we have either
    // seen every point (for deletion) or atleast one point (for addition)
    client.getEndpoints().await.toList.foreach { e =>
      val points = e.getOwnerships.getPointsList.toList
      val traversedPoints = points.filter(pName => seenEntities.get(pName).isDefined)
      val commands = e.getOwnerships.getCommandsList.toList
      val traversedCommands = commands.filter(cName => seenEntities.get(cName).isDefined)

      if ((forDelete && traversedPoints == points && traversedCommands == commands) ||
        (!forDelete && (!traversedPoints.isEmpty || !traversedCommands.isEmpty))) {
        handleConcreteObject(client.getEntityByUuid(e.getUuid).await, e, 1)
      }
    }

    // much more efficient to grab all config files and filter off ones we don't know
    // versus asking for a config file for every entity
    client.getConfigFiles.await.toList.foreach { cf =>
      val users = cf.getEntitiesList.toList
      // never add a "user-less" config file, if we want it we'll grab it specifically
      if (!users.isEmpty) {
        val traversedUsers = users.filter(u => seenEntities.get(u.getUuid.getValue).isDefined)
        if ((forDelete && traversedUsers == users) || (!forDelete && !traversedUsers.isEmpty)) {
          val cfEntity = client.getEntityByUuid(cf.getUuid).await
          handleConcreteObject(cfEntity, cf, 1)
          traversedUsers.foreach { collector.addEdge(_, cfEntity, "uses") }
        }
      }
    }
  }

  private def alreadyProcessed(entity: Entity, depth: Int) = {
    seenEntities.get(entity.getName) match {
      case None =>
        notifier.foreach { _.display(entity, depth) }
        seenEntities.put(entity.getUuid.getValue, entity)
        seenEntities.put(entity.getName, entity)
        false
      case Some(_) =>
        true
    }

  }

  private def handleEntity(entity: Entity, depth: Int): Entity = {
    handleConcreteObject(entity, getConcreteObject(client, entity), depth)
  }

  private def handleConcreteObject(entity: Entity, concrete: GeneratedMessage, depth: Int): Entity = {
    if (!alreadyProcessed(entity, depth)) {
      concrete match {
        case point: Point =>
          collector.addPoint(point, entity)
          val commands = client.getFeedbackCommands(entity.getUuid).await.toList
          commands.foreach { c =>
            getEntity(c.getUuid, depth + 1) // load commands so we can set the feedback relationship immediatley
            collector.addEdge(entity, c, "feedback")
          }
          if (point.hasEndpoint) {
            collector.addEdge(point.getEndpoint, entity, "source")
          }

        case command: Command =>
          collector.addCommand(command, entity)
          if (command.hasEndpoint) {
            // we only add the edge to the source, deleting the endpoint
            collector.addEdge(command.getEndpoint, entity, "source")
          }

        case endpoint: Endpoint =>
          // add channel first, adding endpoint will automatically create channel with wrong uuid
          if (endpoint.hasChannel) getEntity(endpoint.getChannel.getUuid, depth + 1)
          collector.addEndpoint(endpoint, entity)
          // normally we don't recurse into an endpoints children (since then we couldn't delete arbitraty
          // equipment without taking all points/commands on the same endpoint.). However if an endpoint
          // is our root we do want to delete all of the points and commands.
          if (depth == 0) {
            endpoint.getOwnerships.getPointsList.toList.foreach(getEntity(_, depth + 1))
            endpoint.getOwnerships.getCommandsList.toList.foreach(getEntity(_, depth + 1))
          }
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
      client.getPointByUuid(entity.getUuid).await
    } else if (types.find(_ == "Command").isDefined) {
      client.getCommandByName(entity.getName).await
    } else if (types.find(_ == "CommunicationEndpoint").isDefined) {
      client.getEndpointByUuid(entity.getUuid).await
    } else if (types.find(_ == "Channel").isDefined) {
      client.getCommunicationChannelByUuid(entity.getUuid).await
    } else if (types.find(_ == "ConfigurationFile").isDefined) {
      client.getConfigFileByUuid(entity.getUuid).await
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
    if (!cached.isDefined) traverseEquipment(client.getEntityByUuid(logicalNode).await, depth)
    else cached.get
  }
  private def getEntity(logicalNodeName: String, depth: Int) = {
    val cached = seenEntities.get(logicalNodeName)
    if (!cached.isDefined) traverseEquipment(client.getEntityByName(logicalNodeName).await, depth)
    else cached.get
  }

  private def traverseEquipment(entity: Entity, depth: Int): Entity = {
    val cached = seenEntities.get(entity.getUuid.getValue)
    if (!cached.isDefined) handleEntity(entity, depth)
    else cached.get
  }
}