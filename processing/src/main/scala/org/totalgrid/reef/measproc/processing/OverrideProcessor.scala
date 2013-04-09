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
package org.totalgrid.reef.measproc.processing

import org.totalgrid.reef.measproc.MeasProcServiceContext
import org.totalgrid.reef.persistence.ObjectCache

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.service.proto.Processing.MeasOverride
import org.totalgrid.reef.client.service.proto.Measurements.{ DetailQual, Quality, Measurement }
import org.totalgrid.reef.jmx.Metrics

object OverrideProcessor {
  def transformSubstituted(meas: Measurement): Measurement = {
    val q = Quality.newBuilder(meas.getQuality).setSource(Quality.Source.SUBSTITUTED).setOperatorBlocked(true)
    val now = System.currentTimeMillis
    Measurement.newBuilder(meas).setQuality(q).setTime(now).setSystemTime(now).build
  }

  def transformNIS(meas: Measurement): Measurement = {
    val dq = DetailQual.newBuilder(meas.getQuality.getDetailQual).setOldData(true)
    val q = Quality.newBuilder(meas.getQuality).setDetailQual(dq).setOperatorBlocked(true)
    val now = System.currentTimeMillis
    Measurement.newBuilder(meas).setQuality(q).setTime(now).setSystemTime(now).build
  }
}

// TODO: OLD should not be set until a new field measurement comes in (61850-7-3).
class OverrideProcessor(publish: (Measurement, Boolean) => Unit, cache: ObjectCache[Measurement], current: String => Option[Measurement], metrics: Metrics)
    extends MeasProcServiceContext[MeasOverride] with Logging {

  import OverrideProcessor._

  private var map = scala.collection.immutable.Map[String, Option[Measurement]]()

  private val measSupressed = metrics.counter("measSupressed")
  private val overrideCurrentValueMiss = metrics.counter("overrideCurrentValueMiss")
  private val overridenCacheMiss = metrics.counter("overridenCacheMiss")
  private val overridesActive = metrics.gauge("overridesActive")

  def process(m: Measurement) {
    if (map.contains(m.getName)) {
      measSupressed(1)
      cache.put(m.getName, m)
    } else publish(m, false)
  }
  /* --- Implement service context ---- */
  def add(over: MeasOverride) {
    val name = over.getPoint.getName
    val currentlyNIS = map.contains(name)
    val replaceMeas = if (over.hasMeas) Some(over.getMeas) else None

    logger.info("Adding measurement override on: " + name)

    (currentlyNIS, replaceMeas) match {

      // new NIS request, no replace specified
      case (false, None) => {
        val curr = cacheCurrent(name) getOrElse { throw new Exception("No current value associated with NIS point: " + over) }
        map += (name -> None)
        publish(transformNIS(curr), true)
      }

      // new NIS request, replace specified
      case (false, Some(repl)) => {
        cacheCurrent(name)
        map += (name -> replaceMeas)
        publish(transformSubstituted(repl), true)
      }

      // point already NIS, no replace specified
      case (true, None) => logger.info("NIS to point already NIS, ignoring")

      // point already NIS, replace specified, treat as simple replace
      case (true, Some(repl)) => {
        map += (name -> replaceMeas)
        publish(transformSubstituted(repl), true)
      }
    }

    updateMetrics
  }

  def remove(over: MeasOverride) {
    val name = over.getPoint.getName

    logger.info("Removing measurement override on: " + name)

    map -= name
    updateMetrics
    cache.get(name) match {
      case None => overridenCacheMiss(1)
      case Some(cached) => {
        val now = System.currentTimeMillis
        val updatedMeasurement = Measurement.newBuilder(cached).setTime(now).setSystemTime(now).build()
        publish(updatedMeasurement, true)
        cache.delete(name)
      }
    }
  }

  def clear() {
    map = scala.collection.immutable.Map[String, Option[Measurement]]()
    updateMetrics
  }

  private def updateMetrics = {
    overridesActive(map.size)
  }

  private def cacheCurrent(name: String): Option[Measurement] = {
    current(name) match {
      case Some(curr) => cache.put(curr.getName, curr); Some(curr)
      case None => overrideCurrentValueMiss(1); None
    }
  }

}