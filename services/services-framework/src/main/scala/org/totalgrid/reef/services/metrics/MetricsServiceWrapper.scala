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

import org.totalgrid.reef.services.framework.ServiceEntryPoint
import org.totalgrid.reef.services.settings.ServiceOptions
import org.totalgrid.reef.jmx.{ Tag, MetricsSource }
import org.totalgrid.reef.client.proto.Envelope

/**
 * attaches Services to the bus but wraps the response functions with 2 pieces of "middleware".
 *  - auth wrapper that does high level access granting based on resource and verb
 *  - metrics collectors that monitor how many and how long the requests are taking
 */
class MetricsServiceWrapper(metricsSource: MetricsSource, serviceConfiguration: ServiceOptions) {

  /// when we don't keep counts indidually we need to give all the instrumeter the same set of hooks
  private lazy val allMetrics = getHooks("all")

  /// binds a proto serving endpoint to the broker and depending on configuration
  /// will also instrument the call with hooks to track # and length of service requests
  def instrumentCallback[A <: AnyRef](endpoint: ServiceEntryPoint[A]): ServiceEntryPoint[A] = {
    if (serviceConfiguration.metrics) {
      val serviceName = endpoint.descriptor.id()

      val hooks = serviceConfiguration.metricsSplitByService match {
        case false => allMetrics
        case true => getHooks(serviceName)
      }

      new ServiceMetricsInstrumenter(endpoint, hooks, serviceConfiguration.slowQueryThreshold, serviceConfiguration.chattyTransactionThreshold)
    } else {
      endpoint
    }
  }

  private def getHooks(serviceName: String) = {
    serviceConfiguration.metricsSplitByVerb match {
      case true => perVerbHooks(metricsSource, serviceName)
      case false => allVerbsHooks(metricsSource, serviceName)
    }
  }

  private def perVerbHooks(source: MetricsSource, serviceName: String): ServiceMetricHooks = {
    val tagList = Tag("instance", serviceName) :: Tag("type", "Service") :: Nil
    new ServiceMetricHooks {
      override val map = Map(
        Envelope.Verb.GET -> new ServiceVerbHooks(source.metrics("get", tagList)),
        Envelope.Verb.PUT -> new ServiceVerbHooks(source.metrics("put", tagList)),
        Envelope.Verb.DELETE -> new ServiceVerbHooks(source.metrics("delete", tagList)),
        Envelope.Verb.POST -> new ServiceVerbHooks(source.metrics("post", tagList)))
    }
  }

  private def allVerbsHooks(source: MetricsSource, serviceName: String): ServiceMetricHooks = {
    val tagList = Tag("instance", serviceName) :: Tag("type", "Service") :: Nil
    val hooks = new ServiceVerbHooks(source.metrics("all", tagList))
    new ServiceMetricHooks {
      override val map = Map(
        Envelope.Verb.GET -> hooks,
        Envelope.Verb.PUT -> hooks,
        Envelope.Verb.DELETE -> hooks,
        Envelope.Verb.POST -> hooks)
    }
  }
}