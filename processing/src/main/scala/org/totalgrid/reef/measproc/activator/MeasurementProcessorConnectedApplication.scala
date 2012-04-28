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
package org.totalgrid.reef.measproc.activator

import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.app.{ ApplicationSettings, ConnectedApplication }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.{ Client, Connection }
import org.totalgrid.reef.measproc.{ ProcessingNodeMap, MeasStreamConnector, MeasurementProcessorServicesImpl, FullProcessor }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.registration.EventPublisher

class MeasurementProcessorConnectedApplication(measStore: MeasurementStore) extends ConnectedApplication with Logging {

  var measProc = Option.empty[FullProcessor]

  def getApplicationSettings = new ApplicationSettings("Processing", "Processing")

  def onApplicationShutdown() = {
    measProc.foreach(_.stop)
    measStore.disconnect()
  }

  def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) {
    measProc = Some(makeMeasProc(appLevelClient, appConfig, connection.getServiceRegistration.getEventPublisher))
    measProc.foreach { _.start }
  }

  def onConnectionError(msg: String) = {
    logger.warn("Error logging in MeasProc: " + msg)
  }

  private def makeMeasProc(client: Client, appConfig: ApplicationConfig, eventPub: EventPublisher) = {
    def perStreamService = {
      new MeasurementProcessorServicesImpl(client.spawn(), eventPub)
    }

    measStore.connect()

    val connector = new MeasStreamConnector(perStreamService, measStore, appConfig.getInstanceName)
    val connectionHandler = new ProcessingNodeMap(connector)

    val services = new MeasurementProcessorServicesImpl(client, eventPub)
    new FullProcessor(services, connectionHandler, appConfig, client.getInternal.getExecutor)
  }
}