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
package org.totalgrid.reef.services.metrics

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.services.framework.{ RequestContextSource, ServiceEntryPoint }
import org.totalgrid.reef.client.operations.Response

class ServiceMetricsInstrumenter[A <: AnyRef](service: ServiceEntryPoint[A], metricsHolder: ServiceMetricHooks, slowQueryThreshold: Long, chattyTransactionThreshold: Int)
    extends ServiceEntryPoint[A]
    with Logging {

  override val descriptor = service.descriptor

  override def respondAsync(verb: Envelope.Verb, source: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {

    val countingSource = new DatabaseActionsCounter(source)

    def recordMetrics(time: Long, rsp: Response[A]) {

      val metrics = metricsHolder.apply(verb).get

      metrics.count(1)
      metrics.timer(time.toInt)

      if (!rsp.isSuccess) metrics.errors(1)

      val counts = countingSource.databaseActionCounts

      metrics.actions(counts.actions)

      if (time > slowQueryThreshold) {
        logger.info("Slow Request: " + time + "ms to handle " + verb + " request: " + displayRequest(req))
      }
      if (counts.actions > chattyTransactionThreshold) {
        logger.info("Chatty transaction: " + counts.actions + " database queries to handle " + verb + " request: " + displayRequest(req))
      }
    }

    val proxyCallback = new CallbackInterceptor(callback, recordMetrics _).onResponse _

    service.respondAsync(verb, countingSource, req)(proxyCallback)
  }

  private def displayRequest(req: ServiceType) = {
    val klassString = req.getClass.getSimpleName
    // display a substring of the request on failure
    val shortRequest = req.toString.slice(0, 150)
    klassString + " with data: " + shortRequest
  }

}