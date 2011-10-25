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
import org.totalgrid.reef.proto.FEP._

class EquipmentRemoverCache extends ModelDeleterCache

/**
 * when we are caching entries for deletion we will delete them by type, not
 * order in the tree so we store each type in its own list
 */
trait ModelDeleterCache extends ModelCollector {

  var points = List.empty[Point]
  var commands = List.empty[Command]
  var endpoints = List.empty[CommEndpointConfig]
  var channel = List.empty[CommChannel]
  var equipment = List.empty[Entity]
  var configFiles = List.empty[ConfigFile]

  def addPoint(obj: Point, entity: Entity) = {
    // need to clear off the logicalNode because delete uses searchQuery
    // TODO: fix services so they only first do unique query then search query on delete
    points ::= obj.toBuilder.clearLogicalNode.build
  }
  def addCommand(obj: Command, entity: Entity) = {
    commands ::= obj.toBuilder.clearLogicalNode.build
  }
  def addEndpoint(obj: CommEndpointConfig, entity: Entity) = {
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
  def addEdge(edge: EntityEdge) = {}

  def doDeletes(local: LoaderServices) {
    // we need to delete endpoints first because we can't delete points and commands that
    // are sourced by endpoints
    endpoints.foreach(local.delete(_).await)
    channel.foreach(local.delete(_).await)
    commands.foreach(local.delete(_).await)
    points.foreach(local.delete(_).await)
    equipment.foreach(local.delete(_).await)
    configFiles.foreach(local.delete(_).await)
  }

  def size = endpoints.size + channel.size + commands.size + points.size + equipment.size + configFiles.size
}