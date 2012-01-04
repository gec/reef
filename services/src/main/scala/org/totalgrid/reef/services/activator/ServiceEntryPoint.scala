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

import org.totalgrid.reef.services.ServiceOptions
import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings, AmqpSettings }
import org.totalgrid.reef.app.ConnectionCloseManagerEx
import org.totalgrid.reef.client.settings.util.PropertyReader
import net.agileautomata.executor4s._
import org.totalgrid.reef.util.ShutdownHook
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.measurementstore.squeryl.SqlMeasurementStore
import org.totalgrid.reef.client.sapi.service.AsyncService

object ServiceEntryPoint extends ShutdownHook {
  def main(args: Array[String]) = {

    val exe = Executors.newResizingThreadPool(1.minutes)

    val brokerConfig = new AmqpSettings(PropertyReader.readFromFile("org.totalgrid.reef.amqp.cfg"))
    val sql = new DbInfo(PropertyReader.readFromFile("org.totalgrid.reef.sql.cfg"))
    val options = new ServiceOptions(PropertyReader.readFromFile("org.totalgrid.reef.services.cfg"))
    val userSettings = new UserSettings(PropertyReader.readFromFile("org.totalgrid.reef.user.cfg"))
    val nodeSettings = new NodeSettings(PropertyReader.readFromFile("org.totalgrid.reef.node.cfg"))

    val manager = new ConnectionCloseManagerEx(brokerConfig, exe)

    val modules = new ServiceModulesFactory {
      def getDbConnector() = DbConnector.connect(sql)
      def getMeasStore() = new SqlMeasurementStore({ () => })
      def publishServices(services: Seq[AsyncService[_]]) = {}
    }

    manager.addConsumer(ServiceFactory.create(options, userSettings, nodeSettings, modules))

    manager.start()

    waitForShutdown {
      manager.stop()
    }
  }
}