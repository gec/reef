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
import org.totalgrid.reef.client.factory.ReefFactory
import org.totalgrid.reef.client.service.AllScadaService
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.settings.util.PropertyReader
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.client.service.list.ReefServices

object ProtoShellApplication {
  def main(args: Array[String]) = {
    System.setProperty("jline.terminal", "jline.UnsupportedTerminal")

    val userSettings = new UserSettings(PropertyReader.readFromFile("org.totalgrid.reef.user.cfg"))
    val connectionInfo = new AmqpSettings(PropertyReader.readFromFile("org.totalgrid.reef.amqp.cfg"))

    val factory = new ReefFactory(connectionInfo, ReefServices)

    val connection = factory.connect()

    val client = connection.login(userSettings.getUserName, userSettings.getUserPassword).await
    val services = client.getRpcInterface(classOf[AllScadaService])

    val cancel = new Cancelable {
      def cancel() = factory.terminate()
    }

    val app = new ProtoShellApplication(client, services, cancel, userSettings.getUserName, connectionInfo.toString, client.getHeaders.getAuthToken)
    app.run(Array[String]())
  }
}

class ProtoShellApplication(client: Client, services: AllScadaService, cancelable: Cancelable, userName: String, context: String, authToken: String) extends Main {

  setUser(userName)
  setApplication(context)

  override def getDiscoveryResource = "OSGI-INF/blueprint/commands.index"

  protected override def createConsole(commandProcessor: CommandProcessorImpl, in: InputStream, out: PrintStream, err: PrintStream, terminal: Terminal) = {
    new Console(commandProcessor, in, out, err, terminal, null) {
      protected override def isPrintStackTraces = false
      protected override def welcome = {
        session.getConsole().println(">")
      }
      protected override def setSessionProperties = {
        ReefCommandSupport.setSessionVariables(this.session, client, services, context, cancelable, userName, authToken)
      }
    }
  }
}