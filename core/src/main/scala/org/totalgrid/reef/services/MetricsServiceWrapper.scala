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

import org.totalgrid.reef.api.service.IServiceAsync
import org.totalgrid.reef.app.CoreApplicationComponents

/**
 * attaches Services to the bus but wraps the response functions with 2 pieces of "middleware".
 *  - auth wrapper that does high level access granting based on resource and verb
 *  - metrics collectors that monitor how many and how long the requests are taking
 */
class MetricsServiceWrapper(components: CoreApplicationComponents, serviceConfiguration: ServiceOptions) {

  /// creates a shared hook to hand to all of the services so they all update the same
  /// statistic #s.
  private def generateHooks(id: String) = {
    val allServiceHolder = components.metricsPublisher.getStore(id)
    ProtoServicableMetrics.generateMetricsHooks(allServiceHolder, serviceConfiguration.metricsSplitByVerb)
  }

  lazy val allHooks = generateHooks("all") /// lazy since we dont allways use the allHooks object

  /// takes an endpoint and either returns that endpoint unaltered or wraps it metrics
  /// collecting code
  private def getInstrumentedRespondFunction(endpoint: IServiceAsync[_]): IServiceAsync[_] = {
    if (serviceConfiguration.metrics) {
      val hooks = if (serviceConfiguration.metricsSplitByService) {
        generateHooks(endpoint.descriptor.id) // make a new hook object for each service
      } else {
        allHooks // use the same hook object for all of the services
      }
      new ServiceMetrics(endpoint, hooks, serviceConfiguration.slowQueryThreshold)
    } else {
      endpoint
    }
  }

  /// binds a proto serving endpoint to the broker and depending on configuration
  /// will also instrument the call with hooks to track # and length of service requests
  def instrumentCallback(endpoint: IServiceAsync[_]): IServiceAsync[_] = {

    getInstrumentedRespondFunction(endpoint)

    /*
    val authWrappedResponder = if (serviceConfiguration.auth && endpoint.useAuth) {
      new AuthTokenVerifier(responder, exchange, allAuthMetrics)
    } else {
      responder
    }

    authWrappedResponder
    */
  }
}