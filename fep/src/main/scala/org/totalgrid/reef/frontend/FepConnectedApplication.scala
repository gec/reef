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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.app.{ ApplicationSettings, ConnectedApplication }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.settings.UserSettings
import org.totalgrid.reef.protocol.api.ProtocolManager
import org.totalgrid.reef.client.{ ServiceProviderInfo, ServicesList, Client, Connection }
import org.totalgrid.reef.client.types.ServiceTypeInformation

object FepConnectedApplication {
  object FepServiceProvider extends ServicesList {
    import scala.collection.JavaConversions._
    def getServiceTypeInformation: java.util.List[ServiceTypeInformation[_, _]] = Nil

    def getServiceProviders: java.util.List[ServiceProviderInfo] = List(FrontEndProviderServices.serviceInfo)
  }
}

class FepConnectedApplication(protocolName: String, manager: ProtocolManager, protocolSpecificUser: UserSettings)
    extends ConnectedApplication with Logging {

  def getApplicationSettings = new ApplicationSettings("FEP-" + protocolName, "FEP")

  private var fem = Option.empty[FrontEndManager]
  private var client = Option.empty[Client]

  def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client) = {

    connection.addServicesList(FepConnectedApplication.FepServiceProvider)

    client = Some(connection.login(protocolSpecificUser))

    fem = Some(makeFepNode(client.get, appConfig))
    fem.foreach { _.start() }
  }

  def onApplicationShutdown() {
    fem.foreach { _.stop() }
    client.foreach { _.logout() }
  }

  def onConnectionError(msg: String) {
    logger.info("FEP Error connecting: " + msg)
  }

  private def makeFepNode(client: Client, appConfig: ApplicationConfig) = {

    client.setHeaders(client.getHeaders.setResultLimit(10000))

    val services = client.getService(classOf[FrontEndProviderServices])

    def endpointClient = { client.spawn() }

    val frontEndConnections = new FrontEndConnections(Map(protocolName -> manager), endpointClient)
    val populator = new EndpointConnectionPopulatorAction(services)
    val connectionContext = new EndpointConnectionSubscriptionFilter(frontEndConnections, populator, client.getInternal.getExecutor)

    // the manager does all the work of announcing the system, retrieving resources and starting/stopping
    // protocol masters in response to events
    new FrontEndManager(
      services,
      client.getInternal.getExecutor,
      connectionContext,
      appConfig,
      List(protocolName),
      5000)
  }
}