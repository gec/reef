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

import java.util.Dictionary
import org.totalgrid.reef.api.japi.settings.util.{ PropertyReader, PropertyLoading }

object ServiceOptions {

  def fromFile(fileName: String) = new ServiceOptions(PropertyReader.readFromFile(fileName))
}

case class ServiceOptions(
    /// whether to instrument service requests at all
    metrics: Boolean,
    /// track verbs separately(false => 3 pts/service; true => 15 pts/service)
    metricsSplitByVerb: Boolean,
    /// track services separately (true => N verbs * M service; or N verbs * 1)  
    metricsSplitByService: Boolean,

    /// whether we are turning on "auth checking" for all services, only optional during transitory phase
    // val auth: Boolean,

    /// threshold for when a request took too long and should be logged
    slowQueryThreshold: Long,
    /// maximum # of measurements to allow in the history table
    maxMeasurements: Long,
    /// how often to clean excess measurements from history table
    trimPeriodMinutes: Long) {

  def this(props: Dictionary[Object, Object]) = this(
    PropertyLoading.getBoolean("org.totalgrid.reef.services.metrics", props),
    PropertyLoading.getBoolean("org.totalgrid.reef.services.metricsSplitByVerb", props),
    PropertyLoading.getBoolean("org.totalgrid.reef.services.metricsSplitByService", props),
    PropertyLoading.getInt("org.totalgrid.reef.services.slowQueryThresholdMs", props),
    PropertyLoading.getLong("org.totalgrid.reef.services.maxMeasurements", props),
    PropertyLoading.getInt("org.totalgrid.reef.services.trimPeriodMinutes", props))
}

