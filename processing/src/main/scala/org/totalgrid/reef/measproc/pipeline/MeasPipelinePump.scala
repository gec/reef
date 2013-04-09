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

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.service.proto.Measurements.{ MeasurementBatch, Measurement }
import org.totalgrid.reef.jmx.Metrics

class MeasPipelinePump(procFun: Measurement => Unit, flushCache: () => Unit, metrics: Metrics)
    extends Logging {

  private val measProcessingTime = metrics.timer("measProcessingTime")
  private val measProcessed = metrics.counter("measProcessed")

  private val batchProcessingTime = metrics.timer("batchesProcessingTime")
  private val batchProcessed = metrics.counter("batchesProcessed")
  private val batchSize = metrics.average("batchSize")

  def process(b: MeasurementBatch) {
    batchProcessingTime[Unit] {
      deBatch(b) { meas =>
        logger.debug("Processing: " + meas)
        measProcessingTime(procFun(meas))
      }
      flushCache()
    }
    measProcessed(b.getMeasCount)
    batchSize(b.getMeasCount)
    batchProcessed(1)
  }

  private def deBatch[A](batch: MeasurementBatch)(f: Measurement => A) {
    import scala.collection.JavaConversions._

    val now = System.currentTimeMillis()
    batch.getMeasList.toList.foreach { m =>
      val b = m.toBuilder
      if (!m.hasSystemTime) b.setSystemTime(now)
      if (!m.hasTime) b.setTime(batch.getWallTime)
      f(b.build)
    }
  }
}