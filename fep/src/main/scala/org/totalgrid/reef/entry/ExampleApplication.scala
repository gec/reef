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

import org.totalgrid.reef.util.ShutdownHook
import org.totalgrid.reef.clientapi.settings.util.PropertyReader
import org.totalgrid.reef.clientapi.settings.{ NodeSettings, UserSettings, AmqpSettings }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.app.Startup
import net.agileautomata.executor4s.{ Executors }

object ExampleApplication extends ShutdownHook with App {

  val exe = Executors.newScheduledThreadPool()

  try {
    startup()
    waitForShutdown()
  } finally {
    exe.terminate()
  }

  def startup() {
    val brokerSettings = new AmqpSettings(PropertyReader.readFromFile("org.totalgrid.reef.amqp.cfg"))
    val userSettings = new UserSettings(PropertyReader.readFromFile("org.totalgrid.reef.user.cfg"))
    val nodeSettings = new NodeSettings(PropertyReader.readFromFile("org.totalgrid.reef.node.cfg"))

    val broker = new QpidBrokerConnectionFactory(brokerSettings).connect

    val appConfig = for {
      client <- Startup.login(broker, exe, userSettings)
      config <- Startup.enroll(client, nodeSettings, "myName", List("stuff"))
    } yield config

    appConfig.await
    println("done awaiting")
  }

}