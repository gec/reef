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
package org.totalgrid.reef.shell.proto

import org.apache.karaf.shell.console.OsgiCommandSupport
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.service.AllScadaService
import org.apache.felix.service.command.CommandSession
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.client.sapi.client.rest.{ Connection, Client }
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.client.sapi.ReefServices
import org.totalgrid.reef.osgi.OsgiConfigReader
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import org.totalgrid.reef.client.ConnectionCloseListener
import org.totalgrid.reef.client.factory.ReefFactory

object ReefCommandSupport extends Logging {
  def setSessionVariables(session: CommandSession, client: Client, service: AllScadaService, context: String, cancelable: Cancelable, userName: String, authToken: String) = {
    session.put("context", context)
    session.put("client", client)
    session.put("reefSession", service)
    session.put("user", userName)
    session.put("authToken", authToken)
    session.get("cancelable") match {
      case null => // nothing to close
      case x => x.asInstanceOf[Cancelable].cancel
    }
    session.put("cancelable", cancelable)
  }

  def getAuthenticatedClient(session: CommandSession, connection: Connection, context: String, cancelable: Cancelable, userSettings: UserSettings) {
    try {
      val client = connection.login(userSettings.getUserName, userSettings.getUserPassword).await
      val services = client.getRpcInterface(classOf[AllScadaService])

      println("Logged into " + context + " as user: " + userSettings.getUserName + "\n\n")

      setSessionVariables(session, client, services, context, cancelable, userSettings.getUserName, client.getHeaders.getAuthToken)
    } catch {
      case x: Exception =>
        cancelable.cancel()
        println("Couldn't login to Reef: " + x.getMessage)
        logger.error(x.getStackTraceString)
    }
  }

  def attemptLogin(session: CommandSession, amqpSettings: AmqpSettings, userSettings: UserSettings, unexpectedDisconnectCallback: () => Unit) = {

    val factory = new ReefFactory(amqpSettings, ReefServices)
    val conn = factory.connect

    val cancel = new Cancelable {
      def cancel() = {
        // TODO: should be exe.shutdown, deadlocks when onDisconnect occurs
        factory.terminate()
      }
    }
    conn.addConnectionListener(new ConnectionCloseListener {
      def onConnectionClosed(expected: Boolean) {
        if (!expected) unexpectedDisconnectCallback()
      }
    })

    getAuthenticatedClient(session, conn, amqpSettings.toString, cancel, userSettings)
  }
}

abstract class ReefCommandSupport extends OsgiCommandSupport with Logging {

  protected val requiresLogin = true

  /**
   * session to use to interact with services
   *
   * would like this to be called session but OsgiCommandSupport already defines session
   */
  protected def services: AllScadaService = {
    this.session.get("reefSession") match {
      case null => throw new Exception("No session configured!")
      case x => x.asInstanceOf[AllScadaService]
    }
  }

  protected def reefClient: Client = {
    this.session.get("client") match {
      case null => throw new Exception("No client configured!")
      case x => x.asInstanceOf[Client]
    }
  }

  protected def getLoginString = isLoggedIn match {
    case true => "Logged in as User: " + this.get("user").get + " on Reef Node: " + this.get("context").get
    case false => "Not logged in to a Reef Node."
  }

  protected def isLoggedIn = this.session.get("user") match {
    case null => false
    case x => true
  }
  protected def rethrow = this.session.get("rethrow") match {
    case null => false
    case x => true
  }

  def login(client: Client, services: AllScadaService, context: String, cancelable: Cancelable, userName: String, authToken: String) {
    ReefCommandSupport.setSessionVariables(this.session, client, services, context, cancelable, userName, authToken)
  }

  protected def logout() {
    login(null, null, null, null, null, null)
  }

  protected def get(name: String): Option[String] = {
    this.session.get(name) match {
      case null => None
      case x => Some(x.asInstanceOf[String])
    }
  }

  protected def handleDisconnect() {
    println("Connection to broker lost! You have been logged out.")
    logout()
  }

  override protected def doExecute(): Object = {
    println("")
    try {
      if (requiresLogin && !isLoggedIn) {
        println("You must be logged into Reef before you can run this command.")
        try {
          val userSettings = new UserSettings(new OsgiConfigReader(getBundleContext, "org.totalgrid.reef.user").getProperties)
          val connectionInfo = new AmqpSettings(new OsgiConfigReader(getBundleContext, "org.totalgrid.reef.amqp").getProperties)
          println("Attempting login with user specified in etc/org.totalgrid.reef.user.cfg file: " + userSettings.getUserName)
          ReefCommandSupport.attemptLogin(this.session, connectionInfo, userSettings, handleDisconnect)
          doCommand()
        } catch {
          case _ =>
            println("Cannot auto-login see \"reef:login --help\"")
        }
      } else doCommand()
    } catch {
      case ex: Exception =>
        println("Error running command: " + ex)
        logger.error("Error running command " + this.getClass.getSimpleName + " with error: " + ex.getMessage, ex)
        if (rethrow) throw ex
    }
    println("")
    null
  }

  protected def doCommand(): Unit

}
