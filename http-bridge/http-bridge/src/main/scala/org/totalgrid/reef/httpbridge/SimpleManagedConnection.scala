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
package org.totalgrid.reef.httpbridge

import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.exception.ReefServiceException
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.app._

class SimpleManagedConnection(brokerOptions: AmqpSettings, executor: Executor, defaultUser: Option[UserSettings], appSettings: ApplicationSettings)
    extends ManagedConnection
    with ApplicationConnectionListener
    with Logging {

  private var defaultAuthToken = Option.empty[String]

  private val connectionManager = new ConnectionCloseManagerEx(brokerOptions, executor)
  private val appManager = new SimpleApplicationConnectionManager(executor, connectionManager)

  def getAuthenticatedClient(authToken: String) = {
    appManager.getConnection.login(authToken)
  }

  def getNewAuthToken(userName: String, userPassword: String) = {
    appManager.getConnection.login(userName, userPassword).await.getHeaders.getAuthToken()
  }

  def getSharedBridgeAuthToken() = defaultAuthToken

  def onConnectionStatusChanged(isConnected: Boolean) = {
    if (isConnected) {
      defaultUser.foreach { user =>
        try {
          defaultAuthToken = Some(getNewAuthToken(user.getUserName, user.getUserPassword))
        } catch {
          case rse: ReefServiceException =>
            logger.error("Couldn't login default user: " + user.getUserName + ". Error: " + rse, rse)
        }
      }
    } else {
      defaultAuthToken = None
    }
  }

  def onConnectionError(msg: String, exception: Option[Exception]) = {
    logger.warn(msg)
  }

  def start() {
    connectionManager.start()
    appManager.start(appSettings)
  }
  def stop() {
    appManager.stop()
    connectionManager.stop()
  }
}