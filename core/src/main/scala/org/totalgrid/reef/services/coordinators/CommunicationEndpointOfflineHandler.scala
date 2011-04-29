/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.coordinators

import org.totalgrid.reef.models._
import org.totalgrid.reef.util.Logging

import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.measurementstore.{ MeasurementStore }

import org.totalgrid.reef.proto.Measurements
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess._

trait CommunicationEndpointOfflineBehaviors extends Logging {

  def measurementStore: MeasurementStore

  def markOffline(ce: CommunicationEndpoint) {
    markPointsOffline(ce.points.value)
    markCommandsOffline(ce.commands.value)
    info("Marked: " + ce.entityName + " offline. Points: " + ce.points.value.size + " Commands: " + ce.commands.value.size)
  }

  def markOnline(ce: CommunicationEndpoint) {
    markCommandsOnline(ce.commands.value)
    info("Marked: " + ce.entityName + " online. Commands: " + ce.commands.value.size)
  }

  private def markPointsOffline(points: List[Point]) {
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
    val updated = measurements.map(m => {
      val q = m._2.getQuality.toBuilder
      q.setValidity(Measurements.Quality.Validity.QUESTIONABLE)
      q.mergeDetailQual(Measurements.DetailQual.newBuilder.setOldData(true).build)
      m._2.toBuilder.setQuality(q).build
    }).toList
    measurementStore.set(updated ::: missing)
  }

  private def markCommandsOffline(commands: List[Command]) {
    commands.foreach { c =>
      c.connected = false
      ApplicationSchema.commands.update(c)
    }
  }

  private def markCommandsOnline(commands: List[Command]) {
    // TODO: mark all the commands online in one SQL query reef_techdebt-11
    commands.foreach { c =>
      c.connected = true
      ApplicationSchema.commands.update(c)
    }
  }
}