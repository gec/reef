/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.shell.proto

import org.apache.felix.gogo.commands.{ Command, Argument, Option => GogoOption }
import org.totalgrid.reef.proto.Auth.{ Agent, AuthToken }
import java.io.{ BufferedReader, InputStreamReader }

@Command(scope = "reef", name = "login", description = "Logs a user into the system, asks for password interactively")
class ReefLoginCommand extends ReefCommandSupport {
  override val requiresLogin = false

  @Argument(index = 0, name = "userName", description = "user name", required = true, multiValued = false)
  private var userName: String = null

  @GogoOption(name = "-p", description = "password for non-interactive scripting. WARNING password will be visible in command history")
  private var password: String = null

  def doCommand() = {
    if (password == null) {
      val stdIn = new BufferedReader(new InputStreamReader(System.in))

      System.out.println("Enter Password: ")
      password = stdIn.readLine.trim
    } else {
      System.out.println("WARNING: Password will be visible in karaf command history!")
    }

    this.login(userName, services.createNewAuthorizationToken(userName, password))
  }
}

@Command(scope = "reef", name = "logout", description = "Logs out the current user")
class ReefLogoutCommand extends ReefCommandSupport {
  def doCommand() = this.logout()
}

