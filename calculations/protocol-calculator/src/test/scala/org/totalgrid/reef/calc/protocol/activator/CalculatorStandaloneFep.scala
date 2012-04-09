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
package org.totalgrid.reef.calc.protocol.activator

import org.totalgrid.reef.app.impl.{ SimpleConnectedApplicationManager, ApplicationManagerSettings }
import org.totalgrid.reef.frontend.FepConnectedApplication
import org.totalgrid.reef.calc.protocol.CalculatorProtocol
import org.totalgrid.reef.app.ConnectionCloseManagerEx
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.client.settings.{ AmqpSettings, NodeSettings, UserSettings }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.util.ShutdownHook

object CalculatorStandaloneFep extends ShutdownHook {
  def main(args: Array[String]) {
    val properties = PropertyReader.readFromFile("./standalone-node.cfg")

    val userSettings = new UserSettings(properties)
    val brokerSettings = new AmqpSettings(properties)
    val nodeSettings = new NodeSettings(properties)

    val brokerConnection = new QpidBrokerConnectionFactory(brokerSettings)

    val exe = Executors.newResizingThreadPool()

    val manager = new ConnectionCloseManagerEx(brokerConnection, exe)
    manager.start()

    val appManagerSettings = new ApplicationManagerSettings(userSettings, nodeSettings)
    val applicationManager = new SimpleConnectedApplicationManager(exe, manager, appManagerSettings)
    applicationManager.start()

    applicationManager.addConnectedApplication(new FepConnectedApplication(new CalculatorProtocol(), userSettings))

    waitForShutdown {
      applicationManager.stop()
      manager.stop()
      exe.terminate()
    }
  }
}
