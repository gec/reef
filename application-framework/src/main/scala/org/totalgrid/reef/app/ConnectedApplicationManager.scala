/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.app

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }

/**
 * ConnectedApplicationManager manages the connection state to reef. This is more than just the connection to the broker,
 * it also requires that the server is responding to heartbeats from atoll. We are only "fully connected"
 * if we have a valid broker connection, were able to login the app level user, register the application
 * and be successfully making heartbeats. If any of those conditions are not met the rest of the system should
 * treat the application as unconnected.
 */
trait ConnectedApplicationManager {
  /**
   * starts connection process
   */
  def start()

  /**
   * blocks until connection manager has totally stopped
   */
  def stop()

  /**
   * adds a listener, if manager is already connected we will immediately call onConnected
   * with current state of manager.
   */
  def addConnectedApplication(app: ConnectedApplication)

  /**
   * removes a listener
   */
  def removeConnectedApplication(app: ConnectedApplication)

}

case class ApplicationSettings(instanceName: String, capabilites: List[String]) {
  def this(instanceName: String, capability: String) = this(instanceName, List(capability))
}

/**
 * when the state of the ConnectedApplication changes we will call all listeners with the
 * currently connected state and a reference to the generating manager.
 */
trait ConnectedApplication {

  def getApplicationSettings: ApplicationSettings

  /**
   * called when the connection goes up or down
   */
  def onApplicationStartup(appConfig: ApplicationConfig, connection: Connection, appLevelClient: Client)

  def onApplicationShutdown()

  /**
   * called with errors encountered during login, registration or heartbeating.
   * Useful for letting an application (or its users) know why we are not connected
   */
  def onConnectionError(msg: String)
}