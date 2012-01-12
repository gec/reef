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
import org.totalgrid.reef.metrics.MetricsHookContainer
import org.totalgrid.reef.client.service.proto.Measurements.{ MeasurementBatch, Measurement }
import org.totalgrid.reef.measproc.{ MeasBatchProcessor, processing, MeasProcObjectCaches }
import org.totalgrid.reef.persistence.InMemoryObjectCache

class MeasProcessingPipeline(
    caches: MeasProcObjectCaches,
    publish: Measurement => Unit,
    eventSink: Events.Event.Builder => Unit,
    pointNames: List[String]) extends MeasBatchProcessor with MetricsHookContainer {

  // pipeline ends up being defined backwards, output from each step is wired into input of previous step
  // basicProcessingNode -> overrideProc -> triggerProc -> batchOutput

  private var lastCache = new InMemoryObjectCache[Measurement]

  val batchOutput = new ProcessedMeasBatchOutputCache(publish, eventSink, caches.measCache)

  val triggerFactory = new processing.TriggerProcessingFactory(batchOutput.delayedEventSink, lastCache)
  val triggerProc = new processing.TriggerProcessor(batchOutput.pubMeas, triggerFactory, caches.stateCache)
  val overProc = new processing.OverrideProcessor(overrideProcess, caches.overCache, caches.measCache.get)
  val measurementFilter = new processing.MeasurementFilter(overProc.process, pointNames)

  // start the pipeline
  val processor = new MeasPipelinePump(measurementFilter.process, batchOutput.flushCache)

  addHookedObject(processor :: overProc :: triggerProc :: measurementFilter :: Nil)

  // Each MeasOverride add/remove is processed seperatley (not in a meas batch)
  def overrideProcess(m: Measurement, flushNow: Boolean) {

    triggerProc.process(m)
    if (flushNow) batchOutput.flushCache()
  }

  override def process(b: MeasurementBatch) = processor.process(b)
}