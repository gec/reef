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
import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceWrapper
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.client.sapi.ReefServicesList
import org.totalgrid.reef.api.sapi.client.rest.{ Client, Connection }
import net.agileautomata.executor4s.Executor

trait ConnectionConsumer {
  def newConnection(brokerConnection: BrokerConnection, exe: Executor): Cancelable
}

trait ClientConsumer {
  // TODO: should be main client type not AllScadaServiceImpl
  // TODO: remove factory when the client can bind serviceCalls
  def newClient(conn: Connection, client: Client): Cancelable
}

trait AppEnrollerConsumer {
  def applicationRegistered(conn: Connection, client: Client, services: AllScadaService, appConfig: ApplicationConfig): Cancelable
}

class UserLogin(userSettings: UserSettings, consumer: ClientConsumer) extends ConnectionConsumer {
  def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {
    // TODO: move defaultTimeout to userSettings file/object
    val connection = new DefaultConnection(ReefServicesList, brokerConnection, exe, 20000)
    val client = connection.login(userSettings.getUserName, userSettings.getUserPassword).await

    consumer.newClient(connection, client)
  }
}

class ApplicationEnrollerEx(nodeSettings: NodeSettings, instanceName: String, capabilities: List[String], applicationCreator: AppEnrollerConsumer) extends ClientConsumer {
  def newClient(connection: Connection, client: Client) = {

    val services = new AllScadaServiceWrapper(client)

    val appConfig = services.registerApplication(nodeSettings, instanceName, capabilities).await

    val heartBeater = new ProcessHeartbeatActor(services, appConfig.getHeartbeatCfg, client)

    heartBeater.start()

    val userApp = applicationCreator.applicationRegistered(connection, client, services, appConfig)

    new Cancelable {
      override def cancel() {
        userApp.cancel
        heartBeater.stop()
      }
    }
  }
}