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
import org.totalgrid.reef.broker.BrokerConnection
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.services.authz.SqlAuthzService
import org.totalgrid.reef.services.{ ServiceContext, ServiceProviders, ServiceBootstrap, ServiceOptions }
import org.totalgrid.reef.util.{ Cancelable, LifecycleManager }
import org.totalgrid.reef.measurementstore.MeasurementStore
import org.totalgrid.reef.client.sapi.service.AsyncService
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection

/**
 * gets other modules used by the services so can implemented via OSGI or directly
 */
trait ServiceModulesFactory {
  def getDbConnector(): Unit

  def getMeasStore(): MeasurementStore

  def publishServices(services: Seq[AsyncService[_]])
}

object ServiceFactory {
  def create(serviceOptions: ServiceOptions, userSettings: UserSettings, nodeSettings: NodeSettings, modules: ServiceModulesFactory) = {
    new ConnectionConsumer {
      def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {

        val connection = new DefaultConnection(brokerConnection, exe, 5000)
        connection.addServicesList(ReefServices)

        modules.getDbConnector()

        val (appConfig, authToken) = ServiceBootstrap.bootstrapComponents(connection, userSettings, nodeSettings)

        val metricsHolder = MetricsSink.getInstance(appConfig.getInstanceName)

        val mgr = new LifecycleManager
        val measStore = modules.getMeasStore()

        val client = connection.login(authToken).getRpcInterface(classOf[AllScadaService])
        val heartbeater = new ProcessHeartbeatActor(client, appConfig.getHeartbeatCfg, exe)
        val providers = new ServiceProviders(connection, measStore, serviceOptions, SqlAuthzService, metricsHolder, authToken)

        val serviceContext = new ServiceContext(connection, exe)

        serviceContext.addCoordinator(providers.coordinators)

        val services = serviceContext.attachServices(providers.services)

        modules.publishServices(services)

        mgr.start()
        heartbeater.start()

        new Cancelable {
          def cancel() = {
            providers.coordinators.foreach { _.stopProcess() }
            mgr.stop()
            heartbeater.stop()
          }
        }
      }
    }
  }
}