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

import org.totalgrid.reef.client.service.proto.Events
import org.totalgrid.reef.client.service.proto.Measurements.{ MeasurementBatch, Measurement }
import org.totalgrid.reef.measproc._
import org.totalgrid.reef.client.service.proto.Model.Point
import org.totalgrid.reef.jmx.MetricsManager

class MeasProcessingPipeline(
    caches: MeasProcObjectCaches,
    publish: Measurement => Unit,
    eventSink: Events.Event.Builder => Unit,
    points: List[Point],
    endpointName: String) extends MeasBatchProcessor {

  val metricsMgr = MetricsManager("org.totalgrid.reef.measproc", endpointName)

  // pipeline ends up being defined backwards, output from each step is wired into input of previous step
  // basicProcessingNode -> overrideProc -> triggerProc -> batchOutput

  val lastCacheManager = new LastMeasurementCacheManager

  val batchOutput = new ProcessedMeasBatchOutputCache(publish, eventSink, caches.measCache)

  val triggerFactory = new processing.TriggerProcessingFactory(batchOutput.delayedEventSink, lastCacheManager.cache)
  val triggerProc = new processing.TriggerProcessor(batchOutput.pubMeas, triggerFactory, caches.stateCache, metricsMgr.metrics("Triggers"))
  val overProc = new processing.OverrideProcessor(overrideProcess, caches.overCache, caches.measCache.get, metricsMgr.metrics("Overrides"))
  val measWhiteList = new processing.MeasurementWhiteList(overProc.process, points, metricsMgr.metrics("WhiteList"))

  // start the pipeline
  val processor = new MeasPipelinePump(measWhiteList.process, batchOutput.flushCache, metricsMgr.metrics("Pipeline"))

  metricsMgr.register()

  // Each MeasOverride add/remove is processed seperatley (not in a meas batch)
  def overrideProcess(m: Measurement, flushNow: Boolean) {

    triggerProc.process(m)
    if (flushNow) batchOutput.flushCache()
  }

  override def process(b: MeasurementBatch) {
    processor.process(b)
  }

  def close() {
    metricsMgr.unregister()
  }
}