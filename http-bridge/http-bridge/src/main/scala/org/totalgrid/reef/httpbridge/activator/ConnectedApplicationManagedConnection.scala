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
package org.totalgrid.reef.httpbridge.activator

import org.totalgrid.reef.httpbridge.ManagedConnection
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.app.{ ApplicationSettings, ConnectedApplication }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }
import org.totalgrid.reef.client.exception.{ ReefServiceException, ServiceIOException }
import org.totalgrid.reef.client.settings.UserSettings

class ConnectedApplicationManagedConnection(defaultUser: Option[UserSettings])
    extends ManagedConnection
    with ConnectedApplication
    with Logging {

  private var defaultAuthToken = Option.empty[String]
  private var currentConnection = Option.empty[Connection]

  def connection = currentConnection.getOrElse(throw new ServiceIOException("Connection not available"))

  def getAuthenticatedClient(authToken: String) = {
    connection.login(authToken)
  }

  def getNewAuthToken(userName: String, userPassword: String) = {
    connection.login(userName, userPassword).await.getHeaders.getAuthToken()
  }

  def getSharedBridgeAuthToken() = defaultAuthToken

  def getApplicationSettings = new ApplicationSettings("http-brige", "Bridge")

  def onApplicationStartup(appConfig: ApplicationConfig, newConnection: Connection, appLevelClient: Client) = {
    currentConnection = Some(newConnection)
    defaultUser.foreach { user =>
      try {
        defaultAuthToken = Some(getNewAuthToken(user.getUserName, user.getUserPassword))
      } catch {
        case rse: ReefServiceException =>
          logger.error("Couldn't login default user: " + user.getUserName + ". Error: " + rse, rse)
      }
    }
  }

  def onApplicationShutdown() = {
    currentConnection = None
    defaultAuthToken = None
  }

  def onConnectionError(msg: String) = {
    logger.warn("Http-bridge error logging in: " + msg)
  }
}
