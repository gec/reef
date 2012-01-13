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

import org.totalgrid.reef.persistence.ObjectCache
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }
import org.totalgrid.reef.client.service.proto.Processing.{ Filter => FilterProto }
import FilterProto.{ FilterType => Type }

object FilterTrigger {

  def apply(config: FilterProto, cache: ObjectCache[Measurement]) = {
    val cond = config.getType match {
      case Type.DUPLICATES_ONLY => new NoDuplicates
      case Type.DEADBAND => new Deadband(config.deadbandValue.getOrElse(0))
    }
    new FilterTrigger(cache, cond)
  }

  sealed trait Filter {
    def allow(m: Measurement, current: Measurement): Boolean
  }

  class NoDuplicates extends Filter {
    def allow(m: Measurement, current: Measurement) = {
      m.intVal != current.intVal ||
        m.doubleVal != current.doubleVal ||
        m.boolVal != current.boolVal ||
        m.stringVal != current.stringVal
    }
  }

  class Deadband(band: Double) extends Filter {
    def allow(m: Measurement, current: Measurement) = {
      if (m.hasDoubleVal && current.hasDoubleVal) {
        math.abs(m.getDoubleVal - current.getDoubleVal) > band
      } else if (m.hasIntVal && current.hasIntVal) {
        math.abs(m.getIntVal - current.getIntVal).asInstanceOf[Double] > band
      } else {
        true
      }
    }
  }

  def sameQuality(qual: Quality, q: Quality) = {
    qual.validity == q.validity &&
      qual.source == q.source &&
      qual.test == q.test &&
      qual.operatorBlocked == q.operatorBlocked &&
      qual.detailQual.overflow == q.detailQual.overflow &&
      qual.detailQual.outOfRange == q.detailQual.outOfRange &&
      qual.detailQual.badReference == q.detailQual.badReference &&
      qual.detailQual.oscillatory == q.detailQual.oscillatory &&
      qual.detailQual.failure == q.detailQual.failure &&
      qual.detailQual.oldData == q.detailQual.oldData &&
      qual.detailQual.inconsistent == q.detailQual.inconsistent &&
      qual.detailQual.inaccurate == q.detailQual.inaccurate
  }
}

class FilterTrigger(cache: ObjectCache[Measurement], band: FilterTrigger.Filter) extends Trigger.Condition {
  import FilterTrigger.sameQuality

  def apply(m: Measurement, prev: Boolean): Boolean = {
    cache.get(m.getName) match {
      case None => {
        cache.put(m.getName, m)
        true
      }
      case Some(current) => {
        if ((m.unit != current.unit) || !sameQuality(m.getQuality, current.getQuality) || band.allow(m, current)) {
          cache.put(m.getName, m)
          true
        } else {
          false
        }
      }
    }
  }
}