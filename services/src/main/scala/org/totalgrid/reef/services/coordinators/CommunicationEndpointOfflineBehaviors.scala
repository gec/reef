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
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.services.framework.RequestContext

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType._

trait CommunicationEndpointOfflineBehaviors extends Logging {

  def measurementStore: MeasurementStore

  def markOffline(ce: CommunicationEndpoint, context: RequestContext) {
    // dont need to mark commands on or offline
    markPointsOffline(ce.points.value, context)
    logger.info("Marked: " + ce.entityName + " offline. Points: " + ce.points.value.size)
  }

  def markOnline(ce: CommunicationEndpoint, context: RequestContext) {
    logger.info("Marked: " + ce.entityName + " online.")
  }

  protected def markPointsOffline(points: List[Point], context: RequestContext) {
    val toPublish = generateOfflineMeasurements(points)
    measurementStore.set(toPublish.map { _._2 })
    context.operationBuffer.queueLast {
      toPublish.foreach { e =>
        context.subHandler.publishEvent(e._1, e._2, e._2.getName)
      }
    }
  }

  protected def removePointMeasurements(points: List[Point], context: RequestContext) {
    val measurements = measurementStore.get(points.map { _.entityName })
    measurementStore.remove(points.map { _.entityName })
    context.operationBuffer.queueLast {
      measurements.foreach { meas =>
        context.subHandler.publishEvent(REMOVED, meas._2, meas._2.getName)
      }
    }
  }

  protected def generateOfflineMeasurements(points: List[Point]): List[(SubscriptionEventType, Measurement)] = {
    val names = points.map(_.entityName)
    val measurements = measurementStore.get(names)
    val now = System.currentTimeMillis()
    val missing = if (measurements.size != names.size) {
      val returnedNames = measurements.keys.toList
      val missingNames = names.filterNot(returnedNames.contains)
      missingNames.map { name =>
        val meas = Measurements.Measurement.newBuilder.setName(name).setType(Measurements.Measurement.Type.NONE)
        (ADDED, markMeasAsOld(meas, now))
      }
    } else {
      Nil
    }

    val updated = measurements.map {
      case (_, meas) =>
        if (!meas.quality.detailQual.oldData.getOrElse(false)) {
          Some((MODIFIED, markMeasAsOld(meas.toBuilder, now)))
        } else {
          None
        }
    }.toList.flatten

    updated ::: missing
  }

  protected def markMeasAsOld(b: Measurement.Builder, time: Long) = {
    val q = Measurements.Quality.newBuilder
    q.setValidity(Measurements.Quality.Validity.QUESTIONABLE)
    q.setDetailQual(Measurements.DetailQual.newBuilder.setOldData(true))
    b.setQuality(q)
    b.setTime(time).setIsDeviceTime(false)
    b.setSystemTime(time)
    b.build()
  }
}