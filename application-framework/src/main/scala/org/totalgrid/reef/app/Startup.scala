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
package org.totalgrid.reef.app

import org.totalgrid.reef.broker.BrokerConnection
import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.sapi.ReefServices
import org.totalgrid.reef.clientapi.sapi.client.Promise
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.clientapi.settings.{ NodeSettings, UserSettings }
import org.totalgrid.reef.proto.Application

object Startup {

  def login(broker: BrokerConnection, exe: Executor, settings: UserSettings): Promise[Client] = {
    println("logging in!")
    val conn = ReefServices(broker, exe)
    val p = conn.login(settings.getUserName, settings.getUserPassword)
    p.listen(r => println("Client: " + r.extract))
    println("done logging in!")
    p
  }

  def enroll(client: Client, nodeSettings: NodeSettings, instanceName: String, capabilities: List[String]): Promise[Application.ApplicationConfig] = {
    println("enrolling!")
    val services = client.getRpcInterface(classOf[AllScadaService])
    val p = services.registerApplication(nodeSettings, instanceName, capabilities)
    println("done enrolling")
    p
  }

}