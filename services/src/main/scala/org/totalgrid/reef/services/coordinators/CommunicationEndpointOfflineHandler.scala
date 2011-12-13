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
package org.totalgrid.reef.services.coordinators

import org.totalgrid.reef.models.{ CommunicationEndpoint, Point }
import com.weiglewilczek.slf4s.Logging

import org.totalgrid.reef.measurementstore.MeasurementStore

import org.totalgrid.reef.client.service.proto.Measurements

trait CommunicationEndpointOfflineBehaviors extends Logging {

  def measurementStore: MeasurementStore

  def markOffline(ce: CommunicationEndpoint) {
    // dont need to mark commands on or offline
    markPointsOffline(ce.points.value)
    logger.info("Marked: " + ce.entityName + " offline. Points: " + ce.points.value.size)
  }

  def markOnline(ce: CommunicationEndpoint) {
    logger.info("Marked: " + ce.entityName + " online.")
  }

  protected def markPointsOffline(points: List[Point]) {
    val names = points.map(_.entityName)
    val measurements = measurementStore.get(names)
    val missing = if (measurements.size != names.size) {
      val returnedNames = measurements.keys.toList
      val missingNames = names.filterNot(returnedNames.contains)
      val now = System.currentTimeMillis()
      missingNames.map { name =>
        {
          val b = Measurements.Measurement.newBuilder
          val q = Measurements.Quality.newBuilder
          q.setValidity(Measurements.Quality.Validity.QUESTIONABLE)
          b.setQuality(q).setType(Measurements.Measurement.Type.NONE)
          b.setTime(now).setIsDeviceTime(false)
          b.setName(name)
          b.build
        }
      }
    } else {
      Nil
    }
    val updated = measurements.map {
      case (_, meas) =>
        val q = meas.getQuality.toBuilder
        q.setValidity(Measurements.Quality.Validity.QUESTIONABLE)
        q.mergeDetailQual(Measurements.DetailQual.newBuilder.setOldData(true).build)
        meas.toBuilder.setQuality(q).build
    }.toList

    measurementStore.set(updated ::: missing)
  }

}