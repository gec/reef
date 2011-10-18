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
package org.totalgrid.reef.measproc.pipeline

import org.totalgrid.reef.api.proto.Measurements.Measurement
import org.totalgrid.reef.api.proto.Events
import org.totalgrid.reef.persistence.ObjectCache
import org.totalgrid.reef.persistence.KeyValue

class ProcessedMeasBatchOutputCache(
    measPublish: Measurement => Unit,
    eventSink: Events.Event.Builder => Unit,
    measCache: ObjectCache[Measurement]) {

  // List of measurements that get built during a batch process
  var list: List[Measurement] = Nil
  var eventList: List[Events.Event.Builder] = Nil

  def delayedEventSink(e: Events.Event.Builder) {
    e.setTime(System.currentTimeMillis)
    eventList = e :: eventList
  }

  // function that builds the list and publishes to the bus
  def pubMeas(m: Measurement) = {
    list = m :: list
  }

  // flushes the list to the measurement cache
  def flushCache() = {
    val measurements = list.reverse
    val measurementCacheUpdates = measurements.map { m => KeyValue(m.getName, m) }
    val pubEvents = eventList.reverse
    eventList = Nil
    list = Nil
    measCache.put(measurementCacheUpdates)
    measurements.foreach(measPublish(_))
    pubEvents.foreach(eventSink(_))
  }

}