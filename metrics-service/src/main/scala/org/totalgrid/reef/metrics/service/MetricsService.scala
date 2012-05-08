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
package org.totalgrid.reef.metrics.service

import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.sapi.client.Response
import org.totalgrid.reef.client.sapi.service.AsyncServiceBase
import org.totalgrid.reef.metrics.client.proto.Metrics.{ MetricsValue, MetricsRead }
import org.totalgrid.reef.metrics.MetricsHolder
import org.totalgrid.reef.metrics.client.MetricsReadDescriptor
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.RequestHeaders

class MetricsService(metrics: MetricsHolder) extends AsyncServiceBase[MetricsRead] {

  override val descriptor = new MetricsReadDescriptor

  private def buildValuesResponse(filters: List[String]) = {
    val values = metrics.values(filters)

    val b = MetricsRead.newBuilder
      .setReadTime(System.currentTimeMillis())

    values.foreach {
      case (key, v) =>
        val builder = MetricsValue.newBuilder.setName(key)
        v match {
          case d: Double => builder.setValue(d)
          case i: Int => builder.setValue(i.toDouble)
          case _ => builder.setValue(-1.0)
        }
        b.addResults(builder)
    }

    b.build()
  }

  override def getAsync(req: MetricsRead, env: RequestHeaders)(callback: (Response[MetricsRead]) => Unit) {
    val response = buildValuesResponse(req.getFiltersList.toList)
    callback(Response(Status.OK, List(response)))
  }

  override def deleteAsync(req: MetricsRead, env: RequestHeaders)(callback: (Response[MetricsRead]) => Unit) {

    req.getFiltersList.toList match {
      case Nil => callback(Response(Status.BAD_REQUEST, Nil))
      case List("*") => doReset(Nil)
      case filters: List[String] => doReset(filters)
    }

    def doReset(filters: List[String]) {
      metrics.reset(filters)
      callback(Response(Status.DELETED, List(buildValuesResponse(filters))))
    }
  }
}