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

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.jmx.Metrics

/// the metrics collected on any single service request
class ServiceVerbHooks(source: Metrics) {
  /// how many requests handled
  val count = source.counter("Count")
  /// errors counted
  val errors = source.counter("Errors")
  /// time of service requests
  val timer = source.average("Time")
  /// number of database actions
  val actions = source.average("Actions")
}

/// trait to encapsulate the hooks used, sorted by verb
trait ServiceMetricHooks {

  protected val map: Map[Envelope.Verb, ServiceVerbHooks]

  def apply(verb: Envelope.Verb): Option[ServiceVerbHooks] = map.get(verb)
}