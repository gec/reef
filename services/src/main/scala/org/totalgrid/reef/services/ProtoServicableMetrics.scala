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

import org.totalgrid.reef.metrics.{ StaticMetricsHooksBase, MetricsHookSource }
import org.totalgrid.reef.clientapi.proto.Envelope
import org.totalgrid.reef.clientapi.sapi.client.Response
import org.totalgrid.reef.services.framework.{ RequestContext, RequestContextSource, ServiceEntryPoint }
import org.squeryl.Session

/// the metrics collected on any single service request
class ServiceVerbHooks(source: MetricsHookSource, baseName: String) extends StaticMetricsHooksBase(source) {
  /// how many requests handled
  val countHook = counterHook(baseName + "Count")
  /// errors counted
  val errorHook = counterHook(baseName + "Errors")
  /// time of service requests
  val timerHook = averageHook(baseName + "Time")
  /// number of database actions
  val actionsHook = averageHook(baseName + "Actions")
}

/// trait to encapsulate the hooks used, sorted by verb
trait ServiceMetricHooks {

  protected val map: Map[Envelope.Verb, ServiceVerbHooks]

  def apply(verb: Envelope.Verb): Option[ServiceVerbHooks] = map.get(verb)
}

class CallbackTimer[A](callback: Response[A] => Unit, timerFun: (Long, Response[A]) => Unit) {

  val start = System.currentTimeMillis

  def onResponse(rsp: Response[A]) {
    timerFun(System.currentTimeMillis - start, rsp)
    callback(rsp)
  }

}

class SessionStats {
  var selects = 0
  var updates = 0
  var inserts = 0
  var deletes = 0

  /**
   * total number of actions in the session.
   */
  def actions = selects + updates + inserts + deletes

  override def toString = {
    "Total: " + actions + " S: " + selects + " U: " + updates + " I: " + inserts + " D: " + deletes
  }

  def addQuery(s: String) {

    // TODO: replace if/else startsWith with match
    if (s.startsWith("Select")) {
      selects += 1
    } else if (s.startsWith("insert")) {
      inserts += 1
    } else if (s.startsWith("update")) {
      updates += 1
    } else if (s.startsWith("delete")) {
      deletes += 1
    }
  }
}

/**
 * instruments a service proto request entry point so metrics can be collected (by verb if configured)
 */
class ServiceMetrics[A <: AnyRef](service: ServiceEntryPoint[A], hooks: ServiceMetricHooks, slowQueryThreshold: Long, chattyTransactionThreshold : Int)
    extends ServiceEntryPoint[A]
    with Logging {

  override val descriptor = service.descriptor

  override def respondAsync(verb: Envelope.Verb, source: RequestContextSource, req: ServiceType)(callback: Response[ServiceType] => Unit): Unit = {

    val stats = new SessionStats

    def recordMetrics(metrics: ServiceVerbHooks)(time: Long, rsp: Response[A]) {
      metrics.countHook(1)
      metrics.timerHook(time.toInt)
      if (time > slowQueryThreshold)
        logger.info("Slow Request: " + time + "ms to handle request: " + req)
      if (!rsp.success) metrics.errorHook(1)
      metrics.actionsHook(stats.actions)
      if (stats.actions > chattyTransactionThreshold)
        logger.info("Chatty transaction: " + stats.actions + " database queries to handle request: " + req)
    }

    val proxyCallback = hooks(verb) match {
      case Some(metrics) => new CallbackTimer(callback, recordMetrics(metrics)).onResponse _
      case None => callback // no hooks, just pass through the request
    }

    val s = new RequestContextSource {
      def transaction[A](f: (RequestContext) => A) = {
        source.transaction { trans =>
          Session.currentSession.setLogger(stats.addQuery _)
          f(trans)

        }
      }
    }

    service.respondAsync(verb, s, req)(proxyCallback)
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