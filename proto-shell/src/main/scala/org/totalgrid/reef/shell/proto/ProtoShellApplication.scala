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

import org.totalgrid.reef.util.FileConfigReader
import org.totalgrid.reef.sapi.client.ClientSession
import org.totalgrid.reef.japi.client.UserSettings
import org.totalgrid.reef.broker.api.{ BrokerConnectionInfo, BrokerProperties }
import org.totalgrid.reef.api.sapi.client.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.sapi.request.framework.SingleSessionClientSource

object ProtoShellApplication {
  def main(args: Array[String]) = {
    //System.setProperty("jline.terminal", "jline.UnsupportedTerminal")

    val userSettings = new UserSettings(new FileConfigReader("org.totalgrid.reef.user.cfg").props)
    val connectionInfo = BrokerProperties.get(new FileConfigReader("org.totalgrid.reef.amqp.cfg"))

    val client = connect(connectionInfo, userSettings)

    val app = new ProtoShellApplication(client, userSettings.getUserName, connectionInfo.toString)
    app.run(Array[String]())
  }

  def connect(connectionInfo: BrokerConnectionInfo, userSettings: UserSettings) = {
    import org.totalgrid.reef.executor.ReactActorExecutor
    import org.totalgrid.reef.broker.qpid.QpidBrokerConnection
    import org.totalgrid.reef.messaging.{ AmqpClientSession, AMQPProtoFactory }
    import org.totalgrid.reef.api.proto.ReefServicesList

    val amqp = new AMQPProtoFactory with ReactActorExecutor {
      val broker = new QpidBrokerConnection(connectionInfo)
    }

    amqp.connect(5000)
    val client = new AmqpClientSession(amqp, ReefServicesList, 5000) {
      override def close() {
        super.close()
        amqp.disconnect(5000)
      }
    }

    val services = new AllScadaServiceImpl with SingleSessionClientSource {
      def session = client
    }

    val token = services.createNewAuthorizationToken(userSettings.getUserName, userSettings.getUserPassword).await()
    client.modifyHeaders(_.setAuthToken(token))

    client
  }

}

class ProtoShellApplication(clientSession: ClientSession, userName: String, context: String) extends Main {

  setUser(userName)
  setApplication(context)

  override def getDiscoveryResource = "OSGI-INF/blueprint/commands.index"

  protected override def createConsole(commandProcessor: CommandProcessorImpl, in: InputStream, out: PrintStream, err: PrintStream, terminal: Terminal) = {
    new Console(commandProcessor, in, out, err, terminal, null) {
      protected override def isPrintStackTraces = false
      protected override def welcome = {
        session.getConsole().println("hi")
      }
      protected override def setSessionProperties = {
        session.put("reefSession", clientSession)
        session.put("context", context)
        session.put("user", userName)
        session.put("authToken", userName)
      }
    }
  }
}