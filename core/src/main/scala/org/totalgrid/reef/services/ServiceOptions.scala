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

import scala.collection.immutable._

import org.totalgrid.reef.util.{ ConfigReader, BuildEnv }

object ServiceOptions {

  def loadInfo(env: String): ServiceOptions = get(BuildEnv.cfgFileReader(env))

  def loadInfo(): ServiceOptions = loadInfo(BuildEnv.environment)

  def get(cr: ConfigReader): ServiceOptions = {
    val metrics = cr.getBoolean("org.totalgrid.reef.services.metrics", true)
    val metricsSplitByVerb = cr.getBoolean("org.totalgrid.reef.services.metricsSplitByVerb", false)
    val metricsSplitByService = cr.getBoolean("org.totalgrid.reef.services.metricsSplitByService", false)
    val useAuth = cr.getBoolean("org.totalgrid.reef.services.useAuth", true)
    val slowQueryThresholdMs = cr.getInt("org.totalgrid.reef.services.slowQueryThresholdMs", 500)
    val maxMeas = cr.getLong("org.totalgrid.reef.services.maxMeasurements", 2 * 1024 * 1024)
    val trimPeriod = cr.getInt("org.totalgrid.reef.services.trimPeriodMinutes", 15)

    ServiceOptions(metrics, metricsSplitByVerb, metricsSplitByService /*, useAuth*/ , slowQueryThresholdMs, maxMeas, trimPeriod)
  }

}

case class ServiceOptions(
  /// whether to instrument service requests at all
  val metrics: Boolean,
  /// track verbs separately(false => 3 pts/service; true => 15 pts/service)
  val metricsSplitByVerb: Boolean,
  /// track services separately (true => N verbs * M service; or N verbs * 1)  
  val metricsSplitByService: Boolean,

  /// whether we are turning on "auth checking" for all services, only optional during transitory phase
  // val auth: Boolean,

  /// threshold for when a request took too long and should be logged
  val slowQueryThreshold: Long,
  /// maximum # of measurements to allow in the history table
  val maxMeasurements: Long,
  /// how often to clean excess measurements from history table
  val trimPeriodMinutes: Long)

