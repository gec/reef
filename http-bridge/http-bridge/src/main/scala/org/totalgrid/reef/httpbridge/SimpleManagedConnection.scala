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

import org.totalgrid.reef.client.sapi.client.rest.Connection
import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import org.totalgrid.reef.app.{ ConnectionConsumer, ConnectionCloseManagerEx }
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import net.agileautomata.executor4s.{ Cancelable, Executor }
import org.totalgrid.reef.client.exception.{ ReefServiceException, ServiceIOException }
import com.weiglewilczek.slf4s.Logging

class SimpleManagedConnection(brokerOptions: AmqpSettings, executor: Executor, defaultUser: Option[UserSettings]) extends ManagedConnection with ConnectionConsumer with Logging {
  private var currentConnection = Option.empty[Connection]
  private var defaultAuthToken = Option.empty[String]

  def connection = currentConnection.getOrElse(throw new ServiceIOException("No connection to broker."))

  private val manager = new ConnectionCloseManagerEx(brokerOptions, executor)

  manager.addConsumer(this)

  def getAuthenticatedClient(authToken: String) = {
    connection.login(authToken)
  }

  def getNewAuthToken(userName: String, userPassword: String) = {
    connection.login(userName, userPassword).await.getHeaders.getAuthToken()
  }

  def getSharedBridgeAuthToken() = defaultAuthToken

  def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {

    val conn = new DefaultConnection(brokerConnection, exe, 5000)
    conn.addServicesList(new ReefServices)

    currentConnection = Some(conn)

    defaultUser.foreach { user =>
      try {
        defaultAuthToken = Some(getNewAuthToken(user.getUserName, user.getUserPassword))
      } catch {
        case rse: ReefServiceException =>
          logger.error("Couldn't login default user: " + user.getUserName + ". Error: " + rse, rse)
      }
    }

    new Cancelable {
      def cancel() {
        currentConnection = None
        defaultAuthToken = None
      }
    }
  }

  def start() = manager.start()
  def stop() = manager.stop()
}