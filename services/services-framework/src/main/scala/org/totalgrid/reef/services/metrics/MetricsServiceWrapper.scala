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
import org.totalgrid.reef.jmx.Metrics

/**
 * attaches Services to the bus but wraps the response functions with 2 pieces of "middleware".
 *  - auth wrapper that does high level access granting based on resource and verb
 *  - metrics collectors that monitor how many and how long the requests are taking
 */
class MetricsServiceWrapper(metrics: Metrics, serviceConfiguration: ServiceOptions) {

  /// binds a proto serving endpoint to the broker and depending on configuration
  /// will also instrument the call with hooks to track # and length of service requests
  def instrumentCallback[A <: AnyRef](endpoint: ServiceEntryPoint[A]): ServiceEntryPoint[A] = {
    if (serviceConfiguration.metrics) {
      new ServiceMetricsInstrumenter(endpoint, metrics, serviceConfiguration.slowQueryThreshold, serviceConfiguration.chattyTransactionThreshold)
    } else {
      endpoint
    }
  }
}