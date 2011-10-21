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

import org.totalgrid.reef.client.sapi.Descriptors
import org.totalgrid.reef.measurementstore.Historian
import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimpleRead
import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.services.framework.{ RequestContext, ServiceEntryPoint }
import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementHistory }

class MeasurementHistoryService(cm: Historian)
    extends ServiceEntryPoint[MeasurementHistory]
    with SimpleRead {

  val HISTORY_LIMIT = 10000

  override val descriptor = Descriptors.measurementHistory

  override def getSubscribeKeys(req: ServiceType): List[String] = {
    if (req.hasEndTime) throw new BadRequestException("Cannot subscribe to measurement when endTime has been set.")
    if (req.hasSampling && req.getSampling != MeasurementHistory.Sampling.NONE)
      throw new BadRequestException("Cannot subscribe to \"sampled\" data stream, leave sampling field blank or NONE")
    req.getPointName :: Nil
  }

  override def doGet(context: RequestContext, req: ServiceType): ServiceType = {

    val pointName = req.getPointName()

    val keepNewest = req.getKeepNewest()
    val begin = req.getStartTime()
    val end = if (req.getEndTime() == 0) Long.MaxValue else req.getEndTime()
    val limit = if (req.getSampling() == MeasurementHistory.Sampling.NONE) {
      if (req.getLimit() == 0) HISTORY_LIMIT else req.getLimit()
    } else {
      HISTORY_LIMIT
    }

    if (limit > HISTORY_LIMIT)
      throw new BadRequestException("Maximum number of measurements available through this interface is " + HISTORY_LIMIT + ". Reduce limit parameter.")

    // read values out of the historian
    var history = cm.getInRange(pointName, begin, end, limit, !keepNewest)

    req.getSampling() match {
      case MeasurementHistory.Sampling.NONE =>
      case MeasurementHistory.Sampling.EXTREMES =>
        history = sampleExtremes(history)
    }

    // we need to flip the data, since we always return the data in ascending order
    history = if (keepNewest) history.reverse else history

    val b = MeasurementHistory.newBuilder(req)
    history.foreach { m => b.addMeasurements(m) }

    b.build
  }

  private def sampleExtremes(meases: Seq[Measurement]): Seq[Measurement] = {
    // TODO: implement sampling routine
    meases
  }
}