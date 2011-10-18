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

import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.measurementstore.{ MeasurementStoreToMeasurementCacheAdapter, MeasurementStore }
import org.totalgrid.reef.persistence.InMemoryObjectCache
import org.totalgrid.reef.api.proto.Measurements.Measurement
import org.totalgrid.reef.api.proto.Processing.MeasurementProcessingConnection
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.executor.ReactActorExecutor

class MeasStreamConnector(client: MeasurementProcessorServices, measStore: MeasurementStore, instanceName: String) {
  val metricsPublisher = MetricsSink.getInstance(instanceName)

  // caches used to store measurements and overrides
  val measCache = new MeasurementStoreToMeasurementCacheAdapter(measStore)

  // TODO: make override caches configurable like measurement store

  val overCache = new InMemoryObjectCache[Measurement]
  val triggerStateCache = new InMemoryObjectCache[Boolean]

  val caches = MeasProcObjectCaches(measCache, overCache, triggerStateCache)

  def addStreamProcessor(streamConfig: MeasurementProcessingConnection): Cancelable = {
    // TODO: wont need explict reactor when client is stranded
    val reactor = new ReactActorExecutor {}
    reactor.start
    val streamHandler = new MeasurementStreamProcessingNode(client, caches, streamConfig, reactor)
    streamHandler.setHookSource(metricsPublisher.getStore("measproc-" + streamConfig.getLogicalNode.getName))
    new Cancelable {
      def cancel() {
        streamHandler.cancel()
        reactor.stop
      }
    }
  }

}