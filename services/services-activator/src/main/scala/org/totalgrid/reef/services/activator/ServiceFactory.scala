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
package org.totalgrid.reef.services.activator

import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings }
import org.totalgrid.reef.app.ConnectionConsumer
import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.services.authz.SqlAuthzService
import org.totalgrid.reef.services.{ ServiceContext, ServiceProviders, ServiceBootstrap }
import org.totalgrid.reef.services.settings.ServiceOptions
import org.totalgrid.reef.util.LifecycleManager
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.persistence.squeryl.DbConnection
import org.totalgrid.reef.client.Connection

/**
 * gets other modules used by the services so can implemented via OSGI or directly
 */
trait ServiceModulesFactory {
  def getDbConnector(): DbConnection

  def getMeasStore(): MeasurementStore
}

object ServiceFactory {
  def create(serviceOptions: ServiceOptions, userSettings: UserSettings, nodeSettings: NodeSettings, modules: ServiceModulesFactory) = {
    new ConnectionConsumer {
      def handleNewConnection(connection: Connection) = {

        val exe = connection.getInternal.getExecutor

        val dbConnection = modules.getDbConnector()

        val (appConfig, authToken) = ServiceBootstrap.bootstrapComponents(dbConnection, connection, userSettings, nodeSettings)

        val metricsHolder = MetricsSink.getInstance(appConfig.getInstanceName)

        val mgr = new LifecycleManager
        val measStore = modules.getMeasStore()

        measStore.connect()

        val client = connection.createClient(authToken)
        val services = client.getService(classOf[AllScadaService])
        val heartbeater = new ProcessHeartbeatActor(services, appConfig.getHeartbeatCfg, exe)
        val providers = new ServiceProviders(dbConnection, connection, measStore, serviceOptions,
          new SqlAuthzService(), metricsHolder, authToken, exe)

        val serviceContext = new ServiceContext(connection, exe)

        serviceContext.addCoordinator(providers.coordinators)

        serviceContext.attachServices(providers.services)

        mgr.start()
        heartbeater.start()

        new Cancelable {
          def cancel() = {

            client.logout()

            providers.coordinators.foreach { _.stopProcess() }
            mgr.stop()
            heartbeater.stop()

            measStore.disconnect()
          }
        }
      }
    }
  }
}