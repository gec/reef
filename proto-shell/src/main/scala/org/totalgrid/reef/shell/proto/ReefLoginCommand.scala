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

import org.apache.felix.gogo.commands.{ Command, Argument }
import org.totalgrid.reef.proto.Auth.{ Agent, AuthToken }

@Command(scope = "reef", name = "login", description = "Logs a user into the system")
class ReefLoginCommand extends ReefCommandSupport {
  override val requiresLogin = false

  @Argument(index = 0, name = "user name", description = "user name", required = true, multiValued = false)
  private var username: String = null

  @Argument(index = 1, name = "password", description = "password", required = true, multiValued = false)
  private var password: String = null

  def doCommand() = {
    val request = AuthToken.newBuilder.setAgent(Agent.newBuilder.setName(username).setPassword(password)).build
    val response = putOne(request)
    this.login(username, response.getToken)
  }
}

@Command(scope = "reef", name = "logout", description = "Logs out the current user")
class ReefLogoutCommand extends ReefCommandSupport {
  def doCommand() = this.logout()
}

