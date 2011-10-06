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

import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto }
import org.totalgrid.reef.executor.{ Lifecycle, Executor }
import org.totalgrid.reef.app.{ KeyedMap, ServiceHandler, ServiceContext }

abstract class ConnectionHandler(fun: ConnProto => MeasurementStreamProcessingNode)
    extends ServiceContext[ConnProto] with Executor with Lifecycle {

  val serviceHandler = new ServiceHandler(this)

  var running = true

  private val map = new ConnectionMap(fun)

  def add(obj: ConnProto) = if (running) map.add(obj)
  def remove(obj: ConnProto) = if (running) map.remove(obj)
  def modify(obj: ConnProto) = if (running) map.modify(obj)
  def subscribed(list: List[ConnProto]) = if (running) list.foreach(map.add(_))

  def clear = map.clear
}

class ConnectionMap(fun: ConnProto => MeasurementStreamProcessingNode) extends KeyedMap[ConnProto] {

  protected override def getKey(c: ConnProto) = c.getUid

  private var map = Map.empty[String, MeasurementStreamProcessingNode]

  override def addEntry(ep: ConnProto) = {
    val entry = fun(ep)
    map += getKey(ep) -> entry
    entry.start
  }

  override def removeEntry(ep: ConnProto) = {
    map.get(getKey(ep)).get.stop
    map -= getKey(ep)
  }

  override def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.getAssignedTime != existing.getAssignedTime
  }
}