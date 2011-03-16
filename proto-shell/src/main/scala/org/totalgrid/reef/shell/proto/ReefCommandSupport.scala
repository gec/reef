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

import org.apache.karaf.shell.console.OsgiCommandSupport
import org.totalgrid.reef.api.{ RequestEnv, ServiceHandlerHeaders }
import request.RequestFailure
import org.totalgrid.reef.util.Logging

abstract class ReefCommandSupport extends OsgiCommandSupport with OSGiSyncOperations with Logging {

  protected val requiresLogin = true

  override def getDefaultHeaders: RequestEnv = {
    val headers = new ServiceHandlerHeaders
    get("auth_token").foreach { x => headers.addAuthToken(x) }
    super.mergeHeaders(headers.env)
  }

  protected def getUser: Option[String] = this.get("user")

  protected def isLoggedIn = getUser.isDefined

  protected def login(user: String, auth: String) = {
    this.set("user", user)
    this.set("auth_token", auth)
  }

  protected def logout() = {
    this.unset("user")
    this.unset("auth_token")
  }

  private def get(name: String): Option[String] = {
    this.session.get(name) match {
      case null => None
      case x => Some(x.asInstanceOf[String])
    }
  }

  private def unset(name: String): Unit = set(name, null)
  private def set(name: String, value: String): Unit = this.session.put(name, value)

  override protected def doExecute(): Object = {
    println("")
    try {
      if (requiresLogin && !isLoggedIn) {
        println("You must be logged into Reef before you can this command")
        println("See help reef:login")
      } else doCommand()
    } catch {
      case RequestFailure(why) => println(why)
      case ex: Exception =>
        println("Error running command: " + ex)
        error(ex.getStackTraceString)
    }
    println("")
    null
  }

  protected def doCommand(): Unit

}
