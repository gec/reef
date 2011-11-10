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

import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.measproc.{ MeasStreamConnector, MeasurementProcessorServicesImpl, FullProcessor, ProcessingNodeMap }
import org.totalgrid.reef.app._
import org.totalgrid.reef.util.Cancelable
import org.osgi.framework.BundleContext
import org.totalgrid.reef.measurementstore.{ MeasurementStore, MeasurementStoreFinder }
import org.totalgrid.reef.clientapi.settings.{ AmqpSettings, UserSettings, NodeSettings }
import net.agileautomata.executor4s.Executor
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.osgi.{ ExecutorBundleActivator, OsgiConfigReader }

object ProcessingActivator {
  def createMeasProcessor(userSettings: UserSettings, nodeSettings: NodeSettings, measStore: MeasurementStore): UserLogin = {
    val appConfigConsumer = new AppEnrollerConsumer {
      // Downside of using classes not functions, we can't partially evalute
      def applicationRegistered(client: Client, services: AllScadaService, appConfig: ApplicationConfig) = {
        val services = new MeasurementProcessorServicesImpl(client)

        val connector = new MeasStreamConnector(services, measStore, appConfig.getInstanceName)
        val connectionHandler = new ProcessingNodeMap(connector)

        val measProc = new FullProcessor(services, connectionHandler, appConfig, services)

        measProc.start

        new Cancelable {
          def cancel() {
            measProc.stop
          }
        }
      }
    }
    val appEnroller = new ApplicationEnrollerEx(nodeSettings, "Processing-" + nodeSettings.getDefaultNodeName, List("Processing"), appConfigConsumer)
    val userLogin = new UserLogin(userSettings, appEnroller)
    userLogin
  }
}

class ProcessingActivator extends ExecutorBundleActivator with Logging {

  private var manager = Option.empty[ConnectionCloseManagerEx]

  def start(context: BundleContext, exe: Executor) {

    logger.info("Starting Processing bundle..")

    val brokerOptions = new AmqpSettings(OsgiConfigReader(context, "org.totalgrid.reef.amqp").getProperties)
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    val measStore = MeasurementStoreFinder.getInstance(context)

    manager = Some(new ConnectionCloseManagerEx(brokerOptions, exe))

    manager.get.addConsumer(ProcessingActivator.createMeasProcessor(userSettings, nodeSettings, measStore))

    manager.foreach { _.start }
  }

  def stop(context: BundleContext, exe: Executor) = {

    manager.foreach { _.stop }

    logger.info("Stopped Processing bundle..")
  }

}
