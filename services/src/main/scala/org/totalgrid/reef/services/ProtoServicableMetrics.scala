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
package org.totalgrid.reef.services

import com.weiglewilczek.slf4s.Logging

import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.api.sapi.service.{ AsyncService, ServiceResponseCallback, CallbackTimer }

import org.totalgrid.reef.metrics.{ StaticMetricsHooksBase, MetricsHookSource }
import org.totalgrid.reef.api.japi.{ StatusCodes, Envelope }

/// the metrics collected on any single service request
class ServiceVerbHooks(source: MetricsHookSource, baseName: String) extends StaticMetricsHooksBase(source) {
  /// how many requests handled
  val countHook = counterHook(baseName + "Count")
  /// errors counted
  val errorHook = counterHook(baseName + "Errors")
  /// time of service requests
  val timerHook = averageHook(baseName + "Time")
}

/// trait to encapsulate the hooks used, sorted by verb
trait ServiceMetricHooks {

  protected val map: Map[Envelope.Verb, ServiceVerbHooks]

  def apply(verb: Envelope.Verb): Option[ServiceVerbHooks] = map.get(verb)
}

/**
 * instruments a service proto request entry point so metrics can be collected (by verb if configured)
 */
class ServiceMetrics[A](service: AsyncService[A], hooks: ServiceMetricHooks, slowQueryThreshold: Long)
    extends AsyncService[A]
    with Logging {

  override val descriptor = service.descriptor

  def respond(req: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) {

    def recordMetrics(metrics: ServiceVerbHooks)(time: Long, rsp: Envelope.ServiceResponse) {
      metrics.countHook(1)
      metrics.timerHook(time.toInt)
      if (time > slowQueryThreshold)
        logger.info("Slow Request: " + time + "ms to handle request: " + req + " response " + rsp)
      if (!StatusCodes.isSuccess(rsp.getStatus)) metrics.errorHook(1)
    }

    val proxyCallback = hooks(req.getVerb) match {
      case Some(metrics) => new CallbackTimer(callback, recordMetrics(metrics))
      case None => callback // no hooks, just pass through the request
    }

    service.respond(req, env, proxyCallback)
  }

}

object ProtoServicableMetrics {

  def generateMetricsHooks(source: MetricsHookSource, seperateVerbs: Boolean): ServiceMetricHooks = new ServiceMetricHooks {

    override val map = if (seperateVerbs) {
      // new bucket for hook counters for every verb
      Map(
        Envelope.Verb.GET -> new ServiceVerbHooks(source, "get"),
        Envelope.Verb.PUT -> new ServiceVerbHooks(source, "put"),
        Envelope.Verb.DELETE -> new ServiceVerbHooks(source, "delete"),
        Envelope.Verb.POST -> new ServiceVerbHooks(source, "post"))
    } else {
      // give all verbs same hooks
      val allVerbs = new ServiceVerbHooks(source, "")
      Map(
        Envelope.Verb.GET -> allVerbs,
        Envelope.Verb.PUT -> allVerbs,
        Envelope.Verb.DELETE -> allVerbs,
        Envelope.Verb.POST -> allVerbs)
    }

  }

}