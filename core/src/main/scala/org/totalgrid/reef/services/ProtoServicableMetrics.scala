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

import org.totalgrid.reef.messaging.ServiceDescriptor
import org.totalgrid.reef.util._
import org.totalgrid.reef.api.{ Envelope, RequestEnv }

/// the metrics collected on any single service request
class ProtoServicableVerbHooks(source: MetricsHookSource, baseName: String) extends StaticMetricsHooksBase(source) {
  /// how many requests handled
  val countHook = counterHook(baseName + "Count")
  /// errors counted
  val errorHook = counterHook(baseName + "Errors")
  /// time of service requests
  val timerHook = averageHook(baseName + "Time")
}

/// trait to encapsulate the hooks used, sorted by verb
trait ProtoServicableHooks {

  val map: Map[Envelope.Verb, ProtoServicableVerbHooks]
}

/**
 * instruments a service proto request entry point so metrics can be collected (by verb if configured) 
 */
class ProtoServicableMetrics[A](real: ServiceDescriptor[A], hooks: ProtoServicableHooks, slowQueryThreshold: Long)
    extends ServiceDescriptor[A]
    with Logging {

  override val descriptor = real.descriptor

  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {
    hooks.map.get(req.getVerb) match {
      case Some(metrics) =>
        // collect the metrics
        metrics.countHook(1)
        var timeElapsed: Long = 0
        val response = Timing.time({ timeElapsed = _ }) {
          real.respond(req, env)
        }
        metrics.timerHook(timeElapsed.toInt)
        if (timeElapsed > slowQueryThreshold) {
          info { "Slow Request: " + timeElapsed + "ms to handle request: " + req + " response " + response }
        }
        // hasErrorMessage always returns true for some reason, so we check empty
        if (response.getErrorMessage() != "") metrics.errorHook(1)
        response
      case _ =>
        // no hooks, just pass through the request
        real.respond(req, env)
    }
  }
}

object ProtoServicableMetrics {
  def generateMetricsHooks(source: MetricsHookSource, seperateVerbs: Boolean): ProtoServicableHooks = {

    var hookMap = Map.empty[Envelope.Verb, ProtoServicableVerbHooks]

    if (seperateVerbs) {
      // new bucket for hook counters for every verb
      hookMap += Envelope.Verb.GET -> new ProtoServicableVerbHooks(source, "get")
      hookMap += Envelope.Verb.PUT -> new ProtoServicableVerbHooks(source, "put")
      hookMap += Envelope.Verb.DELETE -> new ProtoServicableVerbHooks(source, "delete")
      hookMap += Envelope.Verb.POST -> new ProtoServicableVerbHooks(source, "post")
    } else {
      // give all verbs same hooks
      val allVerbs = new ProtoServicableVerbHooks(source, "")
      hookMap += Envelope.Verb.GET -> allVerbs
      hookMap += Envelope.Verb.PUT -> allVerbs
      hookMap += Envelope.Verb.DELETE -> allVerbs
      hookMap += Envelope.Verb.POST -> allVerbs
    }

    new ProtoServicableHooks {
      val map = hookMap
    }
  }

}