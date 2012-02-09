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
package org.totalgrid.reef.app.impl

import org.totalgrid.reef.client.settings.{ NodeSettings, UserSettings }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.sapi.client.rest.Connection
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.app.{ ConnectedApplication, ConnectionConsumer, ConnectedApplicationManager, ConnectionProvider }
import net.agileautomata.executor4s.{ Cancelable, Executor }
import org.totalgrid.reef.util.Lifecycle

// TODO: implement retry backoff
case class ApplicationManagerSettings(
    userSettings: UserSettings,
    nodeSettings: NodeSettings,
    overrideHeartbeatPeriodMs: Option[Long] = None,
    retryLoginInitialDelayMs: Long,
    retryLoginMaxDelayMs: Long) {

  def this(userSettings: UserSettings, nodeSettings: NodeSettings) = this(userSettings, nodeSettings, None, 1000, 60000)
}

class SimpleConnectedApplicationManager(executor: Executor, provider: ConnectionProvider, managerSettings: ApplicationManagerSettings)
    extends ConnectedApplicationManager
    with ConnectionConsumer
    with Lifecycle
    with Logging {

  private var currentConnection = Option.empty[Connection]
  private var applications = Map.empty[ConnectedApplication, Option[LoginProcessTree]]

  def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {

    val conn = new DefaultConnection(brokerConnection, exe, 5000)
    conn.addServicesList(new ReefServices)

    handleConnection(conn)
  }

  def handleConnection(conn: Connection) = {
    currentConnection = Some(conn)

    applications.keys.foreach { createLoginProcess(_) }

    new Cancelable {
      def cancel() {
        connectionStopped()
      }
    }
  }

  def start() = this.synchronized {
    provider.addConsumer(this)
  }

  def stop() = this.synchronized {
    provider.removeConsumer(this)

    connectionStopped()
  }

  def addConnectedApplication(app: ConnectedApplication) = this.synchronized {

    createLoginProcess(app)
  }

  def removeConnectedApplication(app: ConnectedApplication) = this.synchronized {
    applications.get(app) match {
      case Some(process) => process.foreach { _.stop() }
      case None =>
    }
    applications -= app
  }

  private def connectionStopped() = this.synchronized {

    currentConnection = None

    applications.foreach {
      case (app, process) =>
        process.foreach { p => p.stop() }
    }

    applications = applications.keys.map { _ -> None }.toMap
  }

  private def createLoginProcess(app: ConnectedApplication) {
    val process = if (currentConnection.isEmpty) None else Some(new LoginProcessTree(currentConnection.get, app, managerSettings, executor))

    applications += (app -> process)
  }

}