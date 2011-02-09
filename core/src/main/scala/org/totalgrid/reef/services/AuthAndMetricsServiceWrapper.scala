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
import org.totalgrid.reef.app.CoreApplicationComponents

/**
 * attaches Services to the bus but wraps the response functions with 2 pieces of "middleware".
 *  - auth wrapper that does high level access granting based on resource and verb
 *  - metrics collectors that monitor how many and how long the requests are taking 
 */
class AuthAndMetricsServiceWrapper(components: CoreApplicationComponents, serviceConfiguration: ServiceOptions) {

  /// creates a shared hook to hand to all of the services so they all update the same
  /// statistic #s.
  def generateHooks(exch: String) = {
    val allServiceHolder = components.metricsPublisher.getStore(exch)
    ProtoServicableMetrics.generateMetricsHooks(allServiceHolder, serviceConfiguration.metricsSplitByVerb)
  }
  def getAuthMetrics() = {
    val hooks = new AuthTokenMetrics("")
    if (serviceConfiguration.metrics) {
      hooks.setHookSource(components.metricsPublisher.getStore("all"))
    }
    hooks
  }
  lazy val allHooks = generateHooks("all") /// lazy since we dont allways use the allHooks object
  lazy val allAuthMetrics = getAuthMetrics()

  /// takes an endpoint and either returns that endpoint unaltered or wraps it metrics
  /// collecting code
  def getInstrumentedRespondFunction(endpoint: ServiceDescriptor[_], exch: String): ServiceDescriptor[_] = {
    if (serviceConfiguration.metrics) {
      val hooks = if (serviceConfiguration.metricsSplitByService) {
        generateHooks(exch) // make a new hook object for each service
      } else {
        allHooks // use the same hook object for all of the services
      }
      new ProtoServicableMetrics(endpoint, hooks, serviceConfiguration.slowQueryThreshold)
    } else {
      endpoint
    }
  }

  /// binds a proto serving endpoint to the broker and depending on configuration
  /// will also instrument the call with hooks to track # and length of service requests
  def instrumentCallback(exchange: String, endpoint: ServiceDescriptor[_]): ServiceDescriptor[_] = {

    val responder = getInstrumentedRespondFunction(endpoint, exchange)

    val authWrappedResponder = if (serviceConfiguration.auth && endpoint.useAuth) {
      new AuthTokenVerifier(responder, exchange, allAuthMetrics)
    } else {
      responder
    }

    authWrappedResponder
  }
}