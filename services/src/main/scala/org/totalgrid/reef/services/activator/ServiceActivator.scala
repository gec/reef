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

import net.agileautomata.executor4s._

import org.osgi.framework._
import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.services._
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.app.ConnectionCloseManagerEx
import org.totalgrid.reef.clientapi.settings.{ AmqpSettings, UserSettings, NodeSettings }
import org.totalgrid.reef.clientapi.sapi.service.AsyncService
import org.totalgrid.reef.measurementstore.MeasurementStoreFinder
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.osgi.{ ExecutorBundleActivator, OsgiConfigReader }

class ServiceActivator extends ExecutorBundleActivator with Logging {

  private var manager = Option.empty[ConnectionCloseManagerEx]

  def start(context: BundleContext, exe: Executor) {

    logger.info("Starting Service bundle..")

    val brokerConfig = new AmqpSettings(OsgiConfigReader(context, "org.totalgrid.reef.amqp").getProperties)
    val sql = new DbInfo(OsgiConfigReader(context, "org.totalgrid.reef.sql").getProperties)
    val options = new ServiceOptions(OsgiConfigReader(context, "org.totalgrid.reef.services").getProperties)
    val userSettings = new UserSettings(OsgiConfigReader(context, "org.totalgrid.reef.user").getProperties)
    val nodeSettings = new NodeSettings(OsgiConfigReader(context, "org.totalgrid.reef.node").getProperties)

    val modules = new ServiceModulesFactory {
      def getDbConnector() = DbConnector.connect(sql, context)
      def getMeasStore() = MeasurementStoreFinder.getInstance(context)

      def publishServices(services: Seq[AsyncService[_]]) {
        services.foreach { x =>
          context createService (x, "exchange" -> x.descriptor.id, interface[AsyncService[_]])
        }
      }
    }

    manager = Some(new ConnectionCloseManagerEx(brokerConfig, exe))

    manager.get.addConsumer(ServiceFactory.create(options, userSettings, nodeSettings, modules))

    manager.foreach { _.start }
  }

  def stop(context: BundleContext, exe: Executor) {
    manager.foreach(_.stop())

    logger.info("Stopped Service bundle..")
  }

}

