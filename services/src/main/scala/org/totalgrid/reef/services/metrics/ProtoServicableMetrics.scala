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

import org.totalgrid.reef.metrics.MetricsHookSource
import org.totalgrid.reef.client.proto.Envelope

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