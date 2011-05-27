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
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.japi.request.impl.{ SingleSessionClientSource, AllScadaServiceImpl }
import org.totalgrid.reef.sapi.client.{ ClientSession }

abstract class ReefCommandSupport extends OsgiCommandSupport with Logging {

  protected val requiresLogin = true

  /**
   * session to use to interact with services
   *
   * would like this to be called session but OsgiCommandSupport already defines session
   */
  protected def reefSession: ClientSession = {
    this.session.get("reefSession") match {
      case null => throw new Exception("No session configured!")
      case x => x.asInstanceOf[ClientSession]
    }
  }

  def setReefSession(session: ClientSession, context: String) = {
    this.session.put("context", context)
    this.session.get("reefSession") match {
      case null => // nothing to close
      case x => x.asInstanceOf[ClientSession].close
    }
    this.session.put("reefSession", session)
    session
  }
  /**
   * "all services" wrapper around the reefSession
   */
  protected lazy val services = new AllScadaServiceImpl with SingleSessionClientSource {
    def session = reefSession
  }

  protected def getLoginString = isLoggedIn match {
    case true => "Logged in as User: " + this.get("user").get + " on Reef Node: " + this.get("context").get
    case false => "Not logged in to a Reef Node."
  }

  protected def isLoggedIn = this.session.get("user") match {
    case null => false
    case x => true
  }

  protected def login(user: String, auth: String) = {
    this.set("user", user)
    reefSession.getDefaultHeaders.setAuthToken(auth)
  }

  protected def logout() = {
    this.unset("user")
    setReefSession(null, null)
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
        println("You must be logged into Reef before you can run this command.")
        println("See help reef:login")
      } else doCommand()
    } catch {
      case RequestFailure(why) => println(why)
      case ex: Exception =>
        println("Error running command: " + ex)
        logger.error(ex.getStackTraceString)
    }
    println("")
    null
  }

  protected def doCommand(): Unit

}
