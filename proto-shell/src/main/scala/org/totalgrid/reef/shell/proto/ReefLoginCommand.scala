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
import org.totalgrid.reef.sapi.client.ClientSession
import org.totalgrid.reef.broker.api.BrokerProperties
import org.totalgrid.reef.osgi.OsgiConfigReader

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

      val (client, context) = setupReefSession()

      try {
        setReefSession(client, context)
        this.login(userName, services.createNewAuthorizationToken(userName, password))
      } catch {
        case x: Exception =>
          setReefSession(null, null)
          println("Couldn't login to Reef: " + x.getMessage)
          logger.error(x.getStackTraceString)
      }
    }
  }

  def setupReefSession(): (ClientSession, String)
}

@Command(scope = "reef", name = "login", description = "Authorizes a user with a remote Reef node, asks for password interactively")
class ReefLoginCommand extends ReefLoginCommandBase {

  def setupReefSession() = {

    import org.totalgrid.reef.executor.ReactActorExecutor
    import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
    import org.totalgrid.reef.messaging.{ AmqpClientSession }
    import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
    import org.totalgrid.reef.proto.ReefServicesList

    val connectionInfo = BrokerProperties.get(new OsgiConfigReader(getBundleContext, "org.totalgrid.reef.amqp"))

    val amqp = new AMQPSyncFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(connectionInfo)
    }

    amqp.connect(5000)
    val client = new AmqpClientSession(amqp, ReefServicesList, 5000) {
      override def close() {
        super.close()
        amqp.disconnect(5000)
      }
    }

    (client, connectionInfo.toString)
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

