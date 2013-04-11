package org.totalgrid.reef.frontend.entry

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

import org.osgi.framework._

import org.totalgrid.reef.osgi.Helpers._

import org.totalgrid.reef.app._

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.app.whiteboard.ConnectedApplicationBundleActivator
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.client.settings.UserSettings
import org.totalgrid.reef.protocol.api.{ ProtocolManager, Protocol }
import org.totalgrid.reef.frontend.{ ProtocolTraitToManagerShim, FepConnectedApplication }

final class FepActivator extends ConnectedApplicationBundleActivator {

  private var map = Map.empty[String, (ProtocolManager, ConnectedApplication)]

  override def propertyFiles = super.propertyFiles ::: List("org.totalgrid.reef.fep")

  private def getProtocolName(props: Map[String, Any]): Option[String] = {
    props.get("protocol") match {
      case None => logger.warn("Protocol bundle service does not have name property defined"); None
      case Some(name: String) => Some(name)
      case Some(_) => logger.warn("Protocol bundle service name is not a string"); None
    }
  }

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) = {

    context.watchServices(classOf[Protocol]) {
      case ServiceAdded(p, _) => addProtocol(context, p.name, new ProtocolTraitToManagerShim(p), appManager)
      case ServiceRemoved(p, _) => removeProtocol(p.name, appManager)
    }

    context.watchServices(classOf[ProtocolManager]) {
      case ServiceAdded(p, props) => getProtocolName(props).foreach(name => addProtocol(context, name, p, appManager))
      case ServiceRemoved(p, props) => getProtocolName(props).foreach(name => removeProtocol(name, appManager))
    }
  }

  private def getSettings(context: BundleContext, protocolName: String): UserSettings = {
    // load up the protocol specific user (if configured)
    val userProperties = OsgiConfigReader.load(context, propertyFiles ::: List("org.totalgrid.reef.protocol." + protocolName))
    new UserSettings(userProperties)
  }

  private def addProtocol(context: BundleContext, protocolName: String, p: ProtocolManager, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(protocolName) match {
      case Some(_) => logger.info("Protocol already added: " + protocolName)
      case None =>
        val userSettings = getSettings(context, protocolName)

        val app = new FepConnectedApplication(protocolName, p, userSettings)
        appManager.addConnectedApplication(app)

        map += (protocolName -> (p, app))
    }
  }

  private def removeProtocol(protocolName: String, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(protocolName) match {
      case None => logger.warn("Protocol not found: " + protocolName)
      case Some((_, app)) =>
        map -= protocolName
        appManager.removeConnectedApplication(app)
    }
  }

}
