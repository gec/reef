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

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.service.proto.Model.Point
import org.totalgrid.reef.jmx.Metrics

/**
 * checks to see if the measurements are on the whitelist provided with the endpoint and filters
 * out the unexpected measurements and adds a log message indicating what is being ignored.
 */
class MeasurementWhiteList(protected val next: Measurement => Unit, expectedPoints: List[Point], metrics: Metrics)
    extends Logging {

  val allowedPointNamesLookup = expectedPoints.map { p => p.getName -> p }.toMap
  var ignored = Map.empty[String, Boolean]

  private val ignoredMeasurements = metrics.counter("ignoredMeasurements")

  def process(meas: Measurement) {
    allowedPointNamesLookup.get(meas.getName) match {
      case Some(p) => next(meas.toBuilder.setPointUuid(p.getUuid).build)
      case None =>
        ignoredMeasurements(1)
        ignored.get(meas.getName) match {
          case Some(_) =>
          case None =>
            ignored += meas.getName -> false
            logger.info("Ignoring unexpected measurement: " + meas.getName)
        }
    }
  }
}