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
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.service.AllScadaService
import org.apache.felix.service.command.CommandSession
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.{ Connection, Client }
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }
import org.totalgrid.reef.client.{ SubscriptionEventAcceptor, SubscriptionEvent, Subscription, ConnectionCloseListener }
import org.totalgrid.reef.client.factory.ReefConnectionFactory

object ReefCommandSupport extends Logging {
  def setSessionVariables(session: CommandSession, connection: Connection, client: Client, service: AllScadaService, context: String, cancelable: Cancelable, userName: String, authToken: String) = {
    session.put("context", context)
    session.put("connection", connection)
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
      val client = connection.login(userSettings)
      val services = client.getService(classOf[AllScadaService])

      println("Logged into " + context + " as user: " + userSettings.getUserName + "\n\n")

      setSessionVariables(session, connection, client, services, context, cancelable, userSettings.getUserName, client.getHeaders.getAuthToken)
    } catch {
      case x: Exception =>
        cancelable.cancel()
        println("Couldn't login to Reef: " + x.getMessage)
        logger.error(x.getStackTraceString)
    }
  }

  def attemptLogin(session: CommandSession, amqpSettings: AmqpSettings, userSettings: UserSettings, unexpectedDisconnectCallback: () => Unit) = {

    val factory = ReefConnectionFactory.buildFactory(amqpSettings, new ReefServices)
    val conn = factory.connect()

    val cancel = new Cancelable {
      def cancel() = {
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

  /**
   * holds an objects in the command session to maintain state between command
   * invocations
   */
  class SessionHeldObject[A](name: String, session: => CommandSession, default: A) {

    def get(): A = {
      session.get(name) match {
        case null => default
        case o: Object => o.asInstanceOf[A]
      }
    }
    def set(obj: A) = session.put(name, obj)
    def clear() = session.put(name, null)
  }

  /**
   * wrapper around the object holder that adds some list manipulation functions
   */
  class SessionHeldList[A](name: String, session: => CommandSession) extends SessionHeldObject[List[A]](name, session, Nil: List[A]) {

    def add(key: A) = set(key :: get)
    def remove(key: A) = set(get.filterNot(_ == key))
  }
}

abstract class ReefCommandSupport extends OsgiCommandSupport with Logging {
  import ReefCommandSupport.{ SessionHeldObject, SessionHeldList }

  def list[A](key: String) = new SessionHeldList[A](key, { this.session })
  def obj[A](key: String, default: A) = new SessionHeldObject[A](key, { this.session }, default)

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

  protected def connection: Connection = {
    this.session.get("connection") match {
      case null => throw new Exception("No connection configured!")
      case x => x.asInstanceOf[Connection]
    }
  }

  protected def reefClient: Client = {
    this.session.get("client") match {
      case null => throw new Exception("No client configured!")
      case x => x.asInstanceOf[Client]
    }
  }

  protected def getLoginString = isLoggedIn match {
    case true => "Logged in as user: " + this.get("user").get + " on server: " + this.get("context").get
    case false => "Not logged in."
  }

  protected def isLoggedIn = this.session.get("user") match {
    case null => false
    case x => true
  }
  protected def rethrow = this.session.get("rethrow") match {
    case null => false
    case x => true
  }

  def login(connection: Connection, client: Client, services: AllScadaService, context: String, cancelable: Cancelable, userName: String, authToken: String) {
    ReefCommandSupport.setSessionVariables(this.session, connection, client, services, context, cancelable, userName, authToken)
  }

  protected def logout() {
    login(null, null, null, null, null, null, null)
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

          val properties = OsgiConfigReader.load(getBundleContext, List("org.totalgrid.reef.user", "org.totalgrid.reef.amqp", "org.totalgrid.reef.cli"))
          val userSettings = new UserSettings(properties)
          val connectionInfo = new AmqpSettings(properties)
          println("Attempting login with user specified in etc/org.totalgrid.reef.cli.cfg file: " + userSettings.getUserName)
          ReefCommandSupport.attemptLogin(this.session, connectionInfo, userSettings, handleDisconnect)
          doCommand()
        } catch {
          case _: Throwable =>
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

  /**
   * starts a subscription, calling back the function with each event and waits until users presses control-c
   * and then cancels the subscription for us
   */
  def runSubscription[A](subscription: Subscription[A])(fun: SubscriptionEvent[A] => Unit) {
    subscription.start(new SubscriptionEventAcceptor[A] {
      def onEvent(event: SubscriptionEvent[A]) {
        fun(event)
      }
    })
    try {
      // sleep foreverish
      Thread.sleep(1000000)
    } catch {
      case i: InterruptedException =>
    }
    subscription.cancel()
  }
}
