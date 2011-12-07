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

object ModelCollector {
  def makeEdge(parent: Entity, child: Entity, rel: String) =
    EntityEdge.newBuilder.setParent(parent).setChild(child).setRelationship(rel).build
}

/**
 * when we are traversing the remote trees we are collecting the following objects and this
 * interface helps decouple the remote traverser from the local importer. The order of operations
 * is a prefix tree traversal but this doesn't mean that all operations will be valid at a given time.
 * In particular points may report their
 */
trait ModelCollector {
  def addPoint(obj: Point, entity: Entity)
  def addCommand(obj: Command, entity: Entity)
  def addEndpoint(obj: Endpoint, entity: Entity)
  def addChannel(obj: CommChannel, entity: Entity)
  def addEquipment(entity: Entity)
  def addConfigFile(configFile: ConfigFile, entity: Entity)
  def addEdge(edge: EntityEdge): Unit

  final def addEdge(parent: Entity, child: Entity, rel: String): Unit =
    addEdge(ModelCollector.makeEdge(parent, child, rel))
}