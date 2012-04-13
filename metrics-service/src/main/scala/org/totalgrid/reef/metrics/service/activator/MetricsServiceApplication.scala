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
package org.totalgrid.reef.metrics.service.activator

import org.totalgrid.reef.metrics.service.MetricsService

import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.app.{ ApplicationSettings, ConnectedApplication }
import org.totalgrid.reef.client.{ SubscriptionBinding, AnyNodeDestination }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.{ Client, Connection }
import org.totalgrid.reef.metrics.client.MetricsServiceList
import com.weiglewilczek.slf4s.Logging

class MetricsServiceApplication extends ConnectedApplication with Logging {
  def getApplicationSettings = new ApplicationSettings("Metrics", "Metrics")

  private var serviceBinding = Option.empty[SubscriptionBinding]

  def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) = {
    connection.addServicesList(new MetricsServiceList)

     // TODO: FIX FIX FIX FIX
    //serviceBinding = Some(connection.bindService(new MetricsService(MetricsSink), appLevelClient, new AnyNodeDestination, true))
  }

  def onApplicationShutdown() = {
    serviceBinding.foreach { _.cancel() }
  }

  def onConnectionError(msg: String) = {
    logger.warn("Metrics service setup error: " + msg)
  }
}