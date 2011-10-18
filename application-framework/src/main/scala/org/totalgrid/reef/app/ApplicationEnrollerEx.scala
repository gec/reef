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
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.japi.client.{ UserSettings, NodeSettings }
import org.totalgrid.reef.api.proto.ReefServicesList
import org.totalgrid.reef.messaging.{ BasicSessionPool, AmqpClientSession, SessionSource }
import org.totalgrid.reef.sapi.request.AllScadaService
import org.totalgrid.reef.api.proto.Application.ApplicationConfig
import org.totalgrid.reef.procstatus.ProcessHeartbeatActor
import org.totalgrid.reef.executor.{ ReactActorExecutor, Lifecycle, LifecycleManager }
import org.totalgrid.reef.api.sapi.client.rpc.impl.{ AllScadaServiceImpl, AllScadaServicePooled }

trait ConnectionConsumer {
  def newConnection(factory: AMQPSyncFactory): Cancelable
}

trait ClientConsumer {
  // TODO: should be main client type not AllScadaServiceImpl
  // TODO: remove factory when the client can bind serviceCalls
  def newClient(factory: AMQPSyncFactory, client: AllScadaServiceImpl): Cancelable
}

trait AppEnrollerConsumer {
  def applicationRegistered(factory: AMQPSyncFactory, client: AllScadaServiceImpl, appConfig: ApplicationConfig): Cancelable
}

class SessionSourceShim(factory: AMQPSyncFactory) extends SessionSource {
  def newSession() = new AmqpClientSession(factory, ReefServicesList, 5000)
}

class UserLogin(userSettings: UserSettings, consumer: ClientConsumer) extends ConnectionConsumer {
  def newConnection(factory: AMQPSyncFactory) = {
    val pool = new BasicSessionPool(new SessionSourceShim(factory))
    val client = new AllScadaServicePooled(pool, "")
    val token = client.createNewAuthorizationToken(userSettings.getUserName, userSettings.getUserPassword).await

    val newClient = new AllScadaServicePooled(pool, token)
    consumer.newClient(factory, newClient)
  }
}

class ApplicationEnrollerEx(nodeSettings: NodeSettings, instanceName: String, capabilities: List[String], applicationCreator: AppEnrollerConsumer) extends ClientConsumer {
  def newClient(factory: AMQPSyncFactory, client: AllScadaServiceImpl) = {
    val appConfig = client.registerApplication(nodeSettings, instanceName, capabilities).await

    val heartBeater = new ProcessHeartbeatActor(client, appConfig.getHeartbeatCfg) with ReactActorExecutor

    heartBeater.start()

    val userApp = applicationCreator.applicationRegistered(factory, client, appConfig)

    new Cancelable {
      def cancel() {
        userApp.cancel
        heartBeater.stop()
      }
    }
  }
}