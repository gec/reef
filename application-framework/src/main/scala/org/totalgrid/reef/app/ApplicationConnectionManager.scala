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

import org.totalgrid.reef.client.settings.{ UserSettings, NodeSettings }
import org.totalgrid.reef.client.sapi.client.rest.Connection

/**
 * when the state of the ReefManager changes we will call all listeners with the
 * currently connected state and a reference to the generating manager.
 */
trait ApplicationConnectionListener {
  /**
   * called when the connection goes up or down
   */
  def onConnectionStatusChanged(isConnected: Boolean)

  /**
   * called with errors encountered during login, registration or heartbeating.
   * Useful for letting an application (or its users) know why we are not connected
   */
  def onConnectionError(msg: String, exception: Option[Exception])
}

/**
 * ApplicationConnectionManager manages the connection state to reef. This is more than just the connection to the broker,
 * it also requires that the server is responding to heartbeats from atoll. We are only "fully connected"
 * if we have a valid broker connection, were able to login the atoll level user, register the application
 * and be successfully making heartbeats. If any of those conditions are not met the rest of the system should
 * treat atoll as unconnected.
 *
 * Contract:
 *
 * If we are "fully connected" to reef, getConnection() will return a usable reef Connection.
 * If we are not "fully connected" then we will throw an exception if getConnection is called.
 * User code should use isConnected() to see if getConnection will throw (but they will still need
 * to be ready to catch a ReefConnectionClosedException that may occur from outside our control).
 * If user code needs to perform work if the state changes they should register for updates using
 * addStateChangeListener. The callbacks will come on a random Executor pool thread.
 *
 * @see org.totalgrid.atoll.reef.api.core.ReefConnection
 */
trait ApplicationConnectionManager {
  /**
   * starts connection process
   */
  def start(settings: ApplicationSettings)

  /**
   * blocks until connection manager has totally stopped
   */
  def stop()

  /**
   * whether we are "fully connected" or not
   */
  def isConnected: Boolean

  /**
   * if we are shutting down
   */
  def isShutdown: Boolean

  /**
   * gets the current connection or throws an exception if not connected
   */
  def getConnection: Connection

  /**
   * adds a listener, if manager is already connected we will immediately call onConnected
   * with current state of manager.
   */
  def addConnectionListener(listener: ApplicationConnectionListener)

  /**
   * removes a listener
   */
  def removeConnectionListener(listener: ApplicationConnectionListener)

}

/**
 * settings needed to register the application and configure the heartbeating
 */
case class ApplicationSettings(
  userSettings: UserSettings,
  nodeSettings: NodeSettings,
  instanceName: String,
  capabilities: List[String],
  overrideHeartbeatPeriodMs: Option[Long] = None,
  retryLoginDelayMs: Long = 1000)
