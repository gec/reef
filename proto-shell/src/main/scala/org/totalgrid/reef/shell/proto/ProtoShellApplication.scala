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

import org.apache.karaf.shell.console.Main
import org.apache.karaf.shell.console.jline.Console
import org.apache.felix.gogo.runtime.CommandProcessorImpl

import jline.Terminal
import java.io.{ PrintStream, InputStream }
import org.totalgrid.reef.client.settings.{ AmqpSettings, UserSettings }
import org.totalgrid.reef.client.service.AllScadaService
import org.totalgrid.reef.client.{ Client, Connection }
import org.totalgrid.reef.client.settings.util.PropertyReader
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.service.list.ReefServices

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.factory.ReefConnectionFactory

object ProtoShellApplication {
  def main(args: Array[String]) = {
    System.setProperty("jline.terminal", "jline.UnsupportedTerminal")

    val properties = PropertyReader.readFromFiles(List("target.cfg"))

    val userSettings = new UserSettings(properties)
    val connectionInfo = new AmqpSettings(properties)

    val factory = new ReefConnectionFactory(connectionInfo, new ReefServices)

    val connection = factory.connect()
    val cancel = new Cancelable {
      def cancel() = factory.terminate()
    }

    runTerminal(connection, userSettings, connectionInfo.toString, cancel)
    factory.terminate()
  }

  def runTerminal(connection: Connection, userSettings: UserSettings, context: String, cancelable: Cancelable) {
    try {
      val client = connection.login(userSettings)
      val services = client.getService(classOf[AllScadaService])

      val app = new ProtoShellApplication(connection, client, services, cancelable, userSettings.getUserName, context, client.getHeaders.getAuthToken)
      app.run(Array[String]())
    } catch {
      case e: Exception =>
        cancelable.cancel()
        throw e
    }
  }
}

class ProtoShellApplication(connection: Connection, client: Client, services: AllScadaService, cancelable: Cancelable, userName: String, context: String, authToken: String) extends Main {

  setUser(userName)
  setApplication(context)

  override def getDiscoveryResource = "OSGI-INF/blueprint/commands.index"

  protected override def createConsole(commandProcessor: CommandProcessorImpl, in: InputStream, out: PrintStream, err: PrintStream, terminal: Terminal) = {
    new Console(commandProcessor, in, out, err, terminal, null) {
      protected override def setSessionProperties = {
        ReefCommandSupport.setSessionVariables(this.session, connection, client, services, context, cancelable, userName, authToken)
      }
    }
  }
}