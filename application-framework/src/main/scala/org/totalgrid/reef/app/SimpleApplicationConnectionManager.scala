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

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.exception.ServiceIOException
import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.client.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.client.sapi.client.rest.{ Client, Connection }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.app.subprocess.{ SimpleProcessManager, ErrorHandler, ProcessManager }
import org.totalgrid.reef.app.subprocess.{ Process, OneShotProcess, RetryableProcess }
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig

class SimpleApplicationConnectionManager(executor: Executor, provider: ConnectionProvider)
    extends ApplicationConnectionManager
    with ConnectionConsumer
    with ErrorHandler
    with Logging {

  private var currentConnection = Option.empty[Connection]
  private var currentOptions = Option.empty[ApplicationSettings]
  private var fullyConnected = false

  private val processManager = new SimpleProcessManager(executor)
  processManager.addErrorHandler(this)

  private def options = currentOptions.getOrElse(throw new ServiceIOException("ConnectionManager shutting down"))

  def newConnection(brokerConnection: BrokerConnection, exe: Executor) = {

    val conn = new DefaultConnection(brokerConnection, exe, 5000)
    conn.addServicesList(new ReefServices)

    handleConnection(conn)
  }

  def handleConnection(conn: Connection) = {
    currentConnection = Some(conn)

    startAppRegistration(conn)

    new Cancelable {
      def cancel() {
        connectionStopped()
      }
    }
  }

  def start(settings: ApplicationSettings) = this.synchronized {
    if (currentOptions.isDefined) stop()
    currentOptions = Some(settings)
    processManager.start()

    provider.addConsumer(this)
  }

  def stop() = this.synchronized {
    provider.removeConsumer(this)

    connectionStopped()
    processManager.stop()
    currentOptions = None
    onConnected(false)
  }

  def isConnected = fullyConnected

  def isShutdown = currentOptions.isEmpty

  def getConnection = if (fullyConnected) currentConnection.get else throw new ServiceIOException("No connection to broker.")

  private var listeners = Set.empty[ApplicationConnectionListener]
  def addConnectionListener(listener: ApplicationConnectionListener) = this.synchronized {
    listener.onConnectionStatusChanged(fullyConnected)
    listeners += listener
  }
  def removeConnectionListener(listener: ApplicationConnectionListener) = this.synchronized { listeners -= listener }

  private var task = Option.empty[Process]
  private def startAppRegistration(connection: Connection) = this.synchronized {
    if (task.isDefined) throw new RuntimeException("Already logging in!")
    task = Some(new LoginTask(connection, options))
    processManager.addProcess(task.get)
  }

  private def connectionStopped() = this.synchronized {
    task.foreach { processManager.removeProcess(_) }

    currentConnection = None
  }
  private def onConnected(state: Boolean) = this.synchronized {
    fullyConnected = state
    listeners.foreach { _.onConnectionStatusChanged(state) }
  }
  def onError(msg: String, ex: Option[Exception]) = this.synchronized {
    logger.warn(msg)
    listeners.foreach { _.onConnectionError(msg, ex) }
  }

  class LoginTask(connection: Connection, settings: ApplicationSettings)
      extends RetryableProcess("Logging in " + settings.userSettings.getUserName) {

    override def setupRetryDelay = settings.retryLoginDelayMs

    private var client = Option.empty[Client]
    private var childTask = Option.empty[Process]

    def setup(p: ProcessManager) {

      client = Some(connection.login(settings.userSettings).await)

      val services = client.get.getRpcInterface(classOf[AllScadaService])

      childTask = Some(new AppRegistrationTask(services, settings))
      p.addChildProcess(this, childTask.get)
    }

    def cleanup(p: ProcessManager) {
      childTask.foreach { p.removeProcess(_) }
      client.foreach { _.logout().await }
    }
  }

  class AppRegistrationTask(services: AllScadaService, settings: ApplicationSettings)
      extends OneShotProcess("Registering application: " + settings.instanceName) {

    var appConfig = Option.empty[ApplicationConfig]

    def setup(p: ProcessManager) {
      appConfig = Some(services.registerApplication(settings.nodeSettings, settings.instanceName, settings.capabilities).await)

      services.sendHeartbeat(appConfig.get).await

      onConnected(true)

      p.addChildProcess(this, new HeartbeatTask(services, appConfig.get, settings))
    }

    def cleanup(p: ProcessManager) {

      appConfig.foreach { services.sendApplicationOffline(_) }

      onConnected(false)
    }
  }

  class HeartbeatTask(services: AllScadaService, appConfig: ApplicationConfig, settings: ApplicationSettings)
      extends OneShotProcess("heartbeat " + appConfig.getInstanceName) {

    var timer = Option.empty[Timer]

    def setup(p: ProcessManager) {

      val period: Long = settings.overrideHeartbeatPeriodMs.getOrElse(appConfig.getHeartbeatCfg.getPeriodMs)
      timer = Some(executor.scheduleWithFixedOffset(0.milliseconds, period.milliseconds) {
        try {
          services.sendHeartbeat(appConfig).await
        } catch {
          case rse: Exception =>
            // need to shunt notification out of timer call, otherwise timer.cancel during cleanup will deadlock
            executor.execute {
              p.reportError(this, "Error hearbeating " + rse.getMessage, Some(rse))
              p.failProcess(this)
            }
        }
      })
    }

    def cleanup(p: ProcessManager) {
      timer.foreach { _.cancel() }
    }
  }

}