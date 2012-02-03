/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.metrics.client.service

import org.totalgrid.reef.client.proto.Envelope.Status
import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Response }
import org.totalgrid.reef.client.sapi.service.AsyncServiceBase
import org.totalgrid.reef.metrics.client.impl.MetricsReadDescriptor
import org.totalgrid.reef.metrics.client.proto.Metrics.{ MetricsValue, MetricsRead }
import scala.collection.JavaConversions._
import org.totalgrid.reef.metrics.MetricsHolder

class MetricsService(metrics: MetricsHolder) extends AsyncServiceBase[MetricsRead] {

  override val descriptor = new MetricsReadDescriptor

  override def getAsync(req: MetricsRead, env: BasicRequestHeaders)(callback: (Response[MetricsRead]) => Unit) {

    val values = metrics.values(req.getFiltersList.toList)

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

    callback(Response(Status.OK, List(b.build)))
  }
}