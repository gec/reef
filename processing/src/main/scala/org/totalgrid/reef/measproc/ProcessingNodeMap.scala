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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.app.{ KeyedMap, SubscriptionHandlerBase, ServiceContext }
import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto }
import net.agileautomata.executor4s.Cancelable

class ProcessingNodeMap(connector: MeasStreamConnector)
    extends KeyedMap[ConnProto]
    with ServiceContext[ConnProto]
    with SubscriptionHandlerBase[ConnProto] {

  def subscribed(list: List[ConnProto]) = list.foreach(add(_))

  protected override def getKey(c: ConnProto) = c.getId.getValue

  private var map = Map.empty[String, Cancelable]

  override def addEntry(ep: ConnProto) = {
    val entry = connector.addStreamProcessor(ep)
    map += getKey(ep) -> entry
  }

  override def removeEntry(ep: ConnProto) = {
    map.get(getKey(ep)).get.cancel
    map -= getKey(ep)
  }

  override def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.getAssignedTime != existing.getAssignedTime
  }
}