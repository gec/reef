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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.api.proto.Descriptors

import org.totalgrid.reef.api.proto.Measurements.MeasurementSnapshot

import scala.collection.JavaConversions._

import org.totalgrid.reef.measurementstore.RTDatabase
import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimpleRead
import org.totalgrid.reef.services.framework.{ RequestContext, ServiceEntryPoint }
import org.totalgrid.reef.japi.BadRequestException

class MeasurementSnapshotService(cm: RTDatabase)
    extends ServiceEntryPoint[MeasurementSnapshot]
    with SimpleRead {

  override val descriptor = Descriptors.measurementSnapshot

  override def getSubscribeKeys(req: MeasurementSnapshot): List[String] = {
    req.getPointNamesList().toList.map(_.replace("*", "#"))
  }

  override def doGet(context: RequestContext, req: MeasurementSnapshot): MeasurementSnapshot = {
    val measList = req.getPointNamesList().toList

    val searchList = if (measList.size == 1 && measList.head == "*") {
      Nil // TODO: get list of all points from other source
    } else {
      measList
    }

    val b = MeasurementSnapshot.newBuilder()
    // clients shouldn't ask for 0 measurements but if they do we should just return a blank rather than an error.
    if (searchList.size > 0) {
      val measurements = cm.get(searchList).values()
      val foundNames = measurements.map(_.getName).toList

      val missing = searchList.diff(foundNames)
      if (!missing.isEmpty) {
        throw new BadRequestException("Couldn't find measurements: " + missing.mkString(", "))
      }

      b.addAllPointNames(foundNames)
      b.addAllMeasurements(measurements)
    }
    b.build
  }
}