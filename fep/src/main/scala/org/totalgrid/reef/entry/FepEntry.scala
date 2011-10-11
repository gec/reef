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
package org.totalgrid.reef.entry

import org.osgi.framework._

import org.totalgrid.reef.executor.ReactActorExecutor

import org.totalgrid.reef.protocol.api.Protocol
import org.totalgrid.reef.osgi.OsgiConfigReader

import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.broker.api.BrokerProperties
import org.totalgrid.reef.japi.client.{ NodeSettings, UserSettings }
import org.totalgrid.reef.frontend._
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.app._
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.util.{ Cancelable, Logging }
import org.totalgrid.reef.sapi.request.impl.AllScadaServiceImpl

class FepActivator extends BundleActivator with Logging {

  private var map = Map.empty[Protocol, ConnectionConsumer]

  private var manager = Option.empty[ConnectionManagerEx]

  def start(context: BundleContext) {

    org.totalgrid.reef.executor.Executor.setupThreadPools

    val brokerOptions = BrokerProperties.get(new OsgiConfigReader(context, "org.totalgrid.reef.amqp"))
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    manager = Some(new ConnectionManagerEx(brokerOptions))

    context watchServices withInterface[Protocol] andHandle {
      case AddingService(p, _) => addProtocol(p, userSettings, nodeSettings)
      case ServiceRemoved(p, _) => removeProtocol(p)
    }

    manager.foreach { _.start }
  }

  def stop(context: BundleContext) = manager.foreach { _.stop }

  private def addProtocol(p: Protocol, userSettings: UserSettings, nodeSettings: NodeSettings) = map.synchronized {
    map.get(p) match {
      case Some(x) => logger.info("Protocol already added: " + p.name)
      case None =>
        val appConfigConsumer = new AppEnrollerConsumer {
          // Downside of using classes not functions, we can't partially evalute
          def applicationRegistered(factory: AMQPSyncFactory, client: AllScadaServiceImpl, appConfig: ApplicationConfig) = {
            create(factory, client, appConfig, List(p))
          }
        }
        val appEnroller = new ApplicationEnrollerEx(nodeSettings, "FEP-" + p.name, List("FEP"), appConfigConsumer)
        val userLogin = new UserLogin(userSettings, appEnroller)

        map = map + (p -> userLogin)
        manager.foreach { _.addConsumer(userLogin) }
    }
  }

  private def removeProtocol(p: Protocol) = map.synchronized {
    map.get(p) match {
      case Some(enroller) =>
        map = map - p
        manager.foreach { _.removeConsumer(enroller) }
      case None => logger.warn("Protocol not found: " + p.name)
    }
  }

  private def create(factory: AMQPSyncFactory, client: AllScadaServiceImpl, appConfig: ApplicationConfig, protocols: List[Protocol]) = {

    val exe = new ReactActorExecutor {}

    val services = new FrontEndProviderServicesImpl(client, factory, exe)

    val frontEndConnections = new FrontEndConnections(protocols, services)
    val populator = new EndpointConnectionPopulatorAction(services)
    val connectionContext = new EndpointConnectionSubscriptionFilter(frontEndConnections, populator)

    // the manager does all the work of announcing the system, retrieving resources and starting/stopping
    // protocol masters in response to events
    val fem = new FrontEndManager(
      services,
      exe,
      connectionContext,
      appConfig,
      protocols.map { _.name }.toList,
      5000)

    exe.start
    fem.start
    new Cancelable {
      def cancel() {
        fem.stop
        exe.stop
      }
    }
  }
}
