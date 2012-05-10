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
import org.totalgrid.reef.client.service.proto.FEP._
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.client.service.proto.Alarms.EventConfig
import org.totalgrid.reef.client.operations.scl.ScalaBatchOperations

class EquipmentRemoverCache extends ModelDeleterCache

/**
 * when we are caching entries for deletion we will delete them by type, not
 * order in the tree so we store each type in its own list
 */
trait ModelDeleterCache extends ModelCollector {

  var points = List.empty[Point]
  var commands = List.empty[Command]
  var endpoints = List.empty[Endpoint]
  var channel = List.empty[CommChannel]
  var equipment = List.empty[Entity]
  var configFiles = List.empty[ConfigFile]
  var eventConfigs = List.empty[EventConfig]
  var edges = List.empty[EntityEdge]

  def addPoint(obj: Point, entity: Entity) = {
    points ::= obj
  }
  def addCommand(obj: Command, entity: Entity) = {
    commands ::= obj.toBuilder.clearEndpoint.build
  }
  def addEndpoint(obj: Endpoint, entity: Entity) = {
    endpoints ::= obj
  }
  def addChannel(obj: CommChannel, entity: Entity) = {
    channel ::= obj
  }
  def addEquipment(entity: Entity) = {
    equipment ::= entity
  }
  def addConfigFile(obj: ConfigFile, entity: Entity) = {
    configFiles ::= obj
  }
  def addEventConfig(eventConfig: EventConfig) = {
    eventConfigs ::= eventConfig
  }
  def addEdge(edge: EntityEdge) = {
    // we need to delete "source" links manually to unhook endpoints from their points and commands
    // other edges we can ignore, they will get implictly deleted when the entities are deleted
    // we could delete all edges but it would be much slower
    if (edge.getRelationship == "source") edges ::= edge
  }

  def doDeletes(local: LoaderServices, batchSize: Int) {
    // we need to delete endpoints first because we can't delete points and commands that
    // are sourced by endpoints
    // NOTE: we need the List.empty[GeneratedMessage] to tell the compiler what the type is, when it tries to guess it can run forever
    val toDelete: List[GeneratedMessage] = edges ::: endpoints ::: channel ::: commands ::: points ::: equipment ::: configFiles ::: eventConfigs ::: List.empty[GeneratedMessage]

    ScalaBatchOperations.batchOperations(local, batchSize) {
      toDelete.foreach(local.delete(_))
    }.await()
  }

  def size = endpoints.size + channel.size + commands.size + points.size + equipment.size + configFiles.size + eventConfigs.size
}