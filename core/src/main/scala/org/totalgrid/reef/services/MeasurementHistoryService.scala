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
package org.totalgrid.reef.services

import org.totalgrid.reef.proto.Envelope.Status

import org.totalgrid.reef.protoapi.{ RequestEnv, ProtoServiceException, ProtoServiceTypes }
import ProtoServiceTypes.Response

import org.totalgrid.reef.messaging.{ ServiceEndpoint, Descriptors }

import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementHistory }

import org.totalgrid.reef.measurementstore.Historian

import org.totalgrid.reef.services.ServiceProviderHeaders._

class MeasurementHistoryService(cm: Historian) extends ServiceEndpoint[MeasurementHistory] {
  val HISTORY_LIMIT = 10000

  override val descriptor = Descriptors.measurementHistory

  override def put(req: MeasurementHistory, env: RequestEnv) = noVerb("put")
  override def delete(req: MeasurementHistory, env: RequestEnv) = noVerb("delete")
  override def post(req: MeasurementHistory, env: RequestEnv) = noVerb("post")

  override def get(req: MeasurementHistory, env: RequestEnv): Response[MeasurementHistory] = {

    env.subQueue.foreach(queueName => throw new ProtoServiceException("Subscribe not allowed: " + queueName))

    val pointName = req.getPointName()
    val ascending = req.getAscending()
    val begin = req.getStartTime()
    val end = if (req.getEndTime() == 0) Long.MaxValue else req.getEndTime()
    val limit = if (req.getSampling() == MeasurementHistory.Sampling.NONE) {
      if (req.getLimit() == 0) HISTORY_LIMIT else req.getLimit()
    } else {
      HISTORY_LIMIT
    }

    var history = cm.getInRange(pointName, begin, end, limit, ascending)

    req.getSampling() match {
      case MeasurementHistory.Sampling.NONE =>
      case MeasurementHistory.Sampling.EXTREMES =>
        history = sampleExtremes(history)
    }

    val b = MeasurementHistory.newBuilder(req)
    history.foreach { m => b.addMeasurements(m) }

    new Response(Status.OK, b.build :: Nil)
  }

  private def sampleExtremes(meases: Seq[Measurement]): Seq[Measurement] = {
    // TODO: implement sampling routine
    meases
  }
}