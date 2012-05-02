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

import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.client.settings.{ UserSettings, AmqpSettings }

@Command(scope = "reef", name = "login", description = "Authorizes a user with a remote Reef node, asks for password interactively")
class ReefLoginCommand extends ReefCommandSupport {

  override val requiresLogin = false

  @Argument(index = 0, name = "userName", description = "User name, if not specified we try looking for user settings in etc directory.", required = false, multiValued = false)
  private var userName: String = null

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private var password: String = null

  def doCommand() {

    if (isLoggedIn) {
      println(getLoginString)
      println("\nUse \"reef:logout\" first to logout")
    } else {
      val userSettings = if (userName == null) {
        val user = new UserSettings(OsgiConfigReader.load(getBundleContext, List("org.totalgrid.reef.user", "org.totalgrid.reef.cli")))
        println("Attempting login with user specified in etc/org.totalgrid.reef.cli.cfg file.")
        user
      } else {
        if (password == null) {
          val stdIn = new BufferedReader(new InputStreamReader(System.in))

          println("Enter Password: ")
          password = stdIn.readLine.trim
        } else {
          println("WARNING: Password will be visible in karaf command history!")
        }
        new UserSettings(userName, password)
      }

      val connectionInfo = new AmqpSettings(OsgiConfigReader.load(getBundleContext, "org.totalgrid.reef.amqp"))

      ReefCommandSupport.attemptLogin(this.session, connectionInfo, userSettings, handleDisconnect)
    }
  }
}

@Command(scope = "reef", name = "whoami", description = "Prints out the currently logged in user details.")
class ReefWhoAmICommand extends ReefCommandSupport {

  override val requiresLogin = false

  def doCommand() {
    println(getLoginString)
  }
}

@Command(scope = "reef", name = "logout", description = "Logs out the current user")
class ReefLogoutCommand extends ReefCommandSupport {
  def doCommand() = {
    try {
      this.get("authToken") match {
        case Some(token) =>
          reefClient.logout()
        case None =>
      }
    } catch {
      case ex: ReefServiceException =>
        val errorMsg = "Error logging out: " + ex.getMessage
        println(errorMsg)
        logger.warn(errorMsg, ex)
    }
    this.logout()

    println("Logged out")
  }
}

@Command(scope = "reef", name = "headers", description = "Displays and allows updating of the client headers.")
class ReefHeadersCommand extends ReefCommandSupport {

  @GogoOption(name = "-l", description = "Change the result limit, (must be greater than 0)")
  var resultLimit: Int = -1

  @GogoOption(name = "-t", description = "Change the timeout (must be greater than 0, in milliseconds)")
  var timeout: Long = -1

  @GogoOption(name = "-r", description = "Reset headers to default state.")
  var reset: Boolean = false

  def doCommand() {
    var headers = reefClient.getHeaders

    if (reset) headers = headers.clearResultLimit().clearTimeout()
    if (resultLimit > 0) headers = headers.setResultLimit(resultLimit)
    if (timeout > 0) headers = headers.setTimeout(timeout)

    reefClient.setHeaders(headers)

    import presentation.ClientHeadersView
    ClientHeadersView.displayHeaders(headers)
  }
}

