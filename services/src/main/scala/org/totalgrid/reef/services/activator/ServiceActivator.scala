package org.totalgrid.reef.services.activator

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
import org.osgi.framework._

import org.totalgrid.reef.api.sapi.service.AsyncService

import org.totalgrid.reef.osgi.OsgiConfigReader

import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.services._
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.persistence.squeryl.{ SqlProperties, DbConnector }
import org.totalgrid.reef.persistence.squeryl.DbInfo
import org.totalgrid.reef.app.{ ConnectionCloseManagerEx, ConnectionConsumer }
import org.totalgrid.reef.client.sapi.ReefServices
import net.agileautomata.executor4s._
import org.totalgrid.reef.api.japi.settings.{ AmqpSettings, UserSettings, NodeSettings }
import org.totalgrid.reef.util.{ LifecycleManager, Cancelable }

object ServiceActivator {
  def create(sql: DbInfo, serviceOptions: ServiceOptions, userSettings: UserSettings, nodeSettings: NodeSettings, context: BundleContext) = {
    new ConnectionConsumer {
      def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {

        val connection = ReefServices(brokerConnection, exe)

        DbConnector.connect(sql, context)

        val (appConfig, authToken) = ServiceBootstrap.bootstrapComponents(connection, userSettings, nodeSettings)

        val metricsHolder = MetricsSink.getInstance(appConfig.getInstanceName)

        val mgr = new LifecycleManager
        val measStore = MeasurementStoreFinder.getInstance(context)

        val providers = new ServiceProviders(connection, measStore, serviceOptions, SqlAuthzService, Strand(exe), metricsHolder, authToken)

        val metrics = new MetricsServiceWrapper(metricsHolder, serviceOptions)
        val serviceContext = new ServiceContext(connection, metrics, exe)

        serviceContext.addCoordinator(providers.coordinators)

        val services = serviceContext.attachServices(providers.services)

        val serviceRegistrations = services.map { x =>
          context createService (x, "exchange" -> x.descriptor.id, interface[AsyncService[_]])
        }

        mgr.start()

        new Cancelable {
          def cancel() = {
            serviceRegistrations.foreach { _.unregister() }
            mgr.stop()
          }
        }
      }
    }
  }
}

class ServiceActivator extends BundleActivator {

  private var manager = Option.empty[ConnectionCloseManagerEx]

  def start(context: BundleContext) {

    val brokerConfig = new AmqpSettings(OsgiConfigReader(context, "org.totalgrid.reef.amqp").getProperties)
    val sql = SqlProperties.get(OsgiConfigReader(context, "org.totalgrid.reef.sql"))
    val options = ServiceOptions.fromConfig(OsgiConfigReader(context, "org.totalgrid.reef.services"))
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    val exe = context findService withInterface[Executor] andApply (x => x) match {
      case Some(x) => x
      case None => throw new Exception("Unable to find required executor pool")
    }
    manager = Some(new ConnectionCloseManagerEx(brokerConfig, exe))

    manager.get.addConsumer(ServiceActivator.create(sql, options, userSettings, nodeSettings, context))

    manager.foreach { _.start }
  }

  def stop(context: BundleContext) {
    manager.foreach(_.stop())
  }

}

