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
package org.totalgrid.reef.app

import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.api.japi.settings.{ NodeSettings, UserSettings }
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.client.sapi.ReefServices
import org.totalgrid.reef.api.sapi.client.rest.Client
import net.agileautomata.executor4s.Executor

trait ConnectionConsumer {
  def newConnection(brokerConnection: BrokerConnection, exe: Executor): Cancelable
}

trait ClientConsumer {
  def newClient(client: Client): Cancelable
}

trait AppEnrollerConsumer {
  def applicationRegistered(client: Client, services: AllScadaService, appConfig: ApplicationConfig): Cancelable
}

class UserLogin(userSettings: UserSettings, consumer: ClientConsumer) extends ConnectionConsumer {
  def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {
    // TODO: move defaultTimeout to userSettings file/object
    val connection = ReefServices(brokerConnection, exe)
    val client = connection.login(userSettings.getUserName, userSettings.getUserPassword).await

    consumer.newClient(client)
  }
}

class ApplicationEnrollerEx(nodeSettings: NodeSettings, instanceName: String, capabilities: List[String], applicationCreator: AppEnrollerConsumer) extends ClientConsumer {
  def newClient(client: Client) = {

    val services = client.getRpcInterface(classOf[AllScadaService])

    val appConfig = services.registerApplication(nodeSettings, instanceName, capabilities).await

    val heartBeater = new ProcessHeartbeatActor(services, appConfig.getHeartbeatCfg, client)

    heartBeater.start()

    val userApp = applicationCreator.applicationRegistered(client, services, appConfig)

    new Cancelable {
      override def cancel() {
        userApp.cancel
        heartBeater.stop()
      }
    }
  }
}