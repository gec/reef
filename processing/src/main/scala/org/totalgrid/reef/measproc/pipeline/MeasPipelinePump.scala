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

import org.totalgrid.reef.metrics.MetricsHooks
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, Measurement }

class MeasPipelinePump(procFun: Measurement => Unit, flushCache: () => Unit)
    extends MetricsHooks with Logging {

  protected lazy val measProcessingTime = timingHook("measProcessingTime")
  protected lazy val measProcessed = counterHook("measProcessed")

  protected lazy val batchProcessingTime = timingHook("batchesProcessingTime")
  protected lazy val batchProcessed = counterHook("batchesProcessed")
  protected lazy val batchSize = averageHook("batchSize")

  def process(b: MeasurementBatch) = {
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

  private def deBatch[A](b: MeasurementBatch)(f: Measurement => A) = {
    import scala.collection.JavaConversions._

    b.getMeasList.toList.foreach { m =>
      if (m.hasTime) f(m)
      else f(Measurement.newBuilder(m).setTime(b.getWallTime).build)
    }
  }
}