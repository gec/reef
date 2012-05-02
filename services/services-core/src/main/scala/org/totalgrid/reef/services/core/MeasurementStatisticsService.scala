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

import org.totalgrid.reef.services.framework.SimpleServiceBehaviors.SimpleRead
import org.totalgrid.reef.services.framework.{ RequestContext, ServiceEntryPoint }
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.measurementstore.Historian
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement, MeasurementStatistics }

class MeasurementStatisticsService(cm: Historian)
    extends ServiceEntryPoint[MeasurementStatistics]
    with SimpleRead {

  override val descriptor = Descriptors.measurementStatistics

  def getSubscribeKeys(req: MeasurementStatistics): List[String] = {
    Nil
  }
  override val subscriptionClass = classOf[Measurement]

  override def doGet(context: RequestContext, req: MeasurementStatistics): MeasurementStatistics = {

    if (req.point.name == None && req.point.uuid == None) {
      throw new ReefServiceException("Must include point name or UUID", Status.BAD_REQUEST)
    }

    val result = PointServiceConversion.findRecord(context, req.getPoint).getOrElse {
      throw new ReefServiceException("Cannot find point", Status.BAD_REQUEST)
    }

    val name = result.entity.value.name

    context.auth.authorize(context, Descriptors.measurement.id, "read", List(result.entityId))

    val count = cm.numValues(name)

    val oldestTime = cm.getOldest(name).map(m => m.getTime)

    val b = MeasurementStatistics.newBuilder
      .setPoint(PointServiceConversion.convertToProto(result))
      .setCount(count)

    oldestTime.map(b.setOldestTime(_))

    b.build
  }
}