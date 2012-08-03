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

import collection.JavaConversions._
import collection.immutable

import org.totalgrid.reef.persistence.ObjectCache
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.measproc.MeasProcServiceContext

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Processing.TriggerSet
import org.totalgrid.reef.jmx.Metrics

class TriggerProcessor(protected val next: Measurement => Unit,
  protected val factory: TriggerFactory,
  protected val stateCache: ObjectCache[Boolean],
  metrics: Metrics)
    extends MeasProcServiceContext[TriggerSet]
    with Logging {

  protected var map = immutable.Map[String, List[Trigger]]()

  private val triggersActive = metrics.gauge("triggersActive")

  def process(m: Measurement) {
    val triggerList = map.get(m.getName)

    triggerList match {
      case Some(list) =>
        logger.debug("Applying triggers: " + list.size + " to meas: " + m)
        val res = Trigger.processAll(m, stateCache, list)
        logger.debug("Trigger result: " + res)
        res.foreach(next(_))
      case None =>
        next(m)
    }
  }

  def add(set: TriggerSet) {
    logger.debug("TriggerSet received: " + set)
    val pointName = set.getPoint.getName
    val trigList = set.getTriggersList.toList.map(proto => factory.buildTrigger(proto, pointName))
    map += (pointName -> trigList)
    updateMetrics()
  }

  def remove(set: TriggerSet) {
    logger.debug("TriggerSet removed: " + set)
    map -= set.getPoint.getName
    updateMetrics()
  }

  def clear() {
    map = immutable.Map[String, List[Trigger]]()
    updateMetrics()
  }

  private def updateMetrics() {
    triggersActive(map.size)
  }
}
