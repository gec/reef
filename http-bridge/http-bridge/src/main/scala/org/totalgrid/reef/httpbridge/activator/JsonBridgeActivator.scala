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
package org.totalgrid.reef.httpbridge.activator

import com.weiglewilczek.scalamodules._
import javax.servlet.Servlet
import org.osgi.framework.{ ServiceRegistration, BundleContext }
import org.totalgrid.reef.osgi.OsgiConfigReader
import net.agileautomata.executor4s._
import org.totalgrid.reef.client.service.list.ReefServices
import org.totalgrid.reef.httpbridge._
import org.totalgrid.reef.httpbridge.servlets._
import org.totalgrid.reef.httpbridge.servlets.apiproviders.AllScadaServiceApiCallLibrary
import org.totalgrid.reef.app.whiteboard.ConnectedApplicationBundleActivator
import org.totalgrid.reef.app.{ ConnectedApplicationManager, ConnectionProvider }
import org.totalgrid.reef.httpbridge.servlets.helpers._

/**
 * We use the "whiteboard" style of servlet registration and let the pax extender
 * handle hooking our servlets up to the actual HttpService.
 *
 * http://felix.apache.org/site/apache-felix-http-service.html#ApacheFelixHTTPService-UsingtheWhiteboard
 */
class JsonBridgeActivator extends ConnectedApplicationBundleActivator {

  private var registrations = List.empty[ServiceRegistration]

  override def propertyFiles = super.propertyFiles ::: List("org.totalgrid.reef.httpbridge")

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) = {
    val bridgeOptions = OsgiConfigReader.load(context, "org.totalgrid.reef.httpbridge")
    val defaultUser = DefaultUserConfiguration.getDefaultUser(bridgeOptions)

    val managedConnection = new ConnectedApplicationManagedConnection(defaultUser)

    appManager.addConnectedApplication(managedConnection)

    val builderLocator = new BuilderLocator(new ReefServices)
    val subscriptionHandler = new SimpleSubscriptionManager()
    val bridge = new RestLevelServlet(managedConnection, builderLocator)
    val login = new LoginServlet(managedConnection)
    val converter = new ConverterServlet(builderLocator)
    val apiBridge = new ApiServlet(managedConnection, new AllScadaServiceApiCallLibrary, subscriptionHandler)
    val subHandler = new SubscriptionServlet(subscriptionHandler)

    registrations ::= context.createService(bridge, List("alias" -> "/rest").toMap, interface[Servlet])
    registrations ::= context.createService(login, List("alias" -> "/login").toMap, interface[Servlet])
    registrations ::= context.createService(converter, List("alias" -> "/convert").toMap, interface[Servlet])
    registrations ::= context.createService(apiBridge, List("alias" -> "/api").toMap, interface[Servlet])
    registrations ::= context.createService(subHandler, List("alias" -> "/subscribe").toMap, interface[Servlet])
  }

  override def stopApplication() = {
    registrations.foreach { _.unregister() }
  }
}