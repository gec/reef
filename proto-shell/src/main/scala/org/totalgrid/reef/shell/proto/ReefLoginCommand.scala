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

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }
import java.io.{ BufferedReader, InputStreamReader }

import org.totalgrid.reef.osgi.OsgiConfigReader
import net.agileautomata.executor4s.Executors
import org.totalgrid.reef.api.sapi.client.rest.impl.DefaultConnection
import org.totalgrid.reef.client.sapi.ReefServicesList

import org.totalgrid.reef.util.Cancelable

import org.totalgrid.reef.client.rpc.impl.AllScadaServiceJavaShimWrapper
import org.totalgrid.reef.api.sapi.client.rest.Connection
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory
import org.totalgrid.reef.api.japi.settings.AmqpSettings

/**
 * base implementation for login commands, handles getting user name and password, implementors just need to
 * define setupReefSession and call the underlying setReefSession(session, "context")
 */
abstract class ReefLoginCommandBase extends ReefCommandSupport {
  override val requiresLogin = false

  @Argument(index = 0, name = "userName", description = "user name", required = true, multiValued = false)
  private var userName: String = null

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private var password: String = null

  def doCommand() {

    if (isLoggedIn) {
      System.out.println(getLoginString)
      System.out.println("\nUse \"reef:logout\" first to logout")
    } else {
      if (password == null) {
        val stdIn = new BufferedReader(new InputStreamReader(System.in))

        System.out.println("Enter Password: ")
        password = stdIn.readLine.trim
      } else {
        System.out.println("WARNING: Password will be visible in karaf command history!")
      }

      val (connection, context, cancel) = setupReefSession()

      try {
        // TODO: rework setReefSession calls
        setReefSession(null, null, null, cancel)
        val session = connection.login(userName, password).await /// TODO - should load user out of config file
        val services = new AllScadaServiceJavaShimWrapper(session)
        setReefSession(session, services, context, cancel)

        // TODO: implement session.getHeaders.getAuthToken

        login(userName, "")
      } catch {
        case x: Exception =>
          setReefSession(null, null, null, null)
          println("Couldn't login to Reef: " + x.getMessage)
          logger.error(x.getStackTraceString)
      }
    }
  }

  def setupReefSession(): (Connection, String, Cancelable)
}

@Command(scope = "reef", name = "login", description = "Authorizes a user with a remote Reef node, asks for password interactively")
class ReefLoginCommand extends ReefLoginCommandBase {

  def setupReefSession() = {

    val connectionInfo = new AmqpSettings(new OsgiConfigReader(getBundleContext, "org.totalgrid.reef.amqp").getProperties)

    val factory = new QpidBrokerConnectionFactory(connectionInfo)
    val broker = factory.connect
    val exe = Executors.newScheduledThreadPool()
    val conn = new DefaultConnection(ReefServicesList, broker, exe, 20000)

    val cancel = new Cancelable {
      def cancel() = {
        broker.disconnect()
        exe.shutdown()
      }
    }

    (conn, connectionInfo.toString, cancel)
  }
}

@Command(scope = "reef", name = "logout", description = "Logs out the current user")
class ReefLogoutCommand extends ReefCommandSupport {
  def doCommand() = {
    this.get("authToken") match {
      case Some(token) => services.deleteAuthorizationToken(token)
      case None =>
    }
    this.logout()
  }
}

