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

import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.app._

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.app.whiteboard.ConnectedApplicationBundleActivator
import org.totalgrid.reef.protocol.api.scada.ProtocolAdapter
import org.totalgrid.reef.osgi.OsgiConfigReader
import org.totalgrid.reef.client.settings.UserSettings
import org.totalgrid.reef.protocol.api.{ ProtocolManager, Protocol, ScadaProtocolAdapter }
import org.totalgrid.reef.frontend.{ FepConnectedApplication, ProtocolInterface }

final class FepActivator extends ConnectedApplicationBundleActivator {

  import ProtocolInterface._

  private var map = Map.empty[String, (ProtocolInterface, ConnectedApplication)]

  override def propertyFiles = super.propertyFiles ::: List("org.totalgrid.reef.fep")

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) = {

    context watchServices withInterface[Protocol] andHandle {
      case AddingService(p, _) => addProtocol(context, p.name, TraitInterface(p), appManager)
      case ServiceRemoved(p, _) => removeProtocol(p.name, TraitInterface(p), appManager)
    }

    context watchServices withInterface[ProtocolManager] andHandle {
      case AddingService(p, props) =>
        props.get("protocol") match {
          case None => logger.warn("Protocol bundle service does not have name property defined")
          case Some(name: String) => addProtocol(context, name, ManagerInterface(p), appManager)
          case Some(_) => logger.warn("Protocol bundle service name is not a string")
        }
      case ServiceRemoved(p, props) =>

    }
  }

  private def getSettings(context: BundleContext, protocolName: String): UserSettings = {
    // load up the protocol specific user (if configured)
    val userProperties = OsgiConfigReader.load(context, propertyFiles ::: List("org.totalgrid.reef.protocol." + protocolName))
    new UserSettings(userProperties)
  }

  private def addProtocol(context: BundleContext, protocolName: String, p: ProtocolInterface, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(protocolName) match {
      case Some(_) => logger.info("Protocol already added: " + protocolName)
      case None =>
        val userSettings = getSettings(context, protocolName)

        val app = p match {
          case TraitInterface(pt) => new FepConnectedApplication(pt, userSettings)
          case ManagerInterface(pm) => new FepConnectedApplication(protocolName, pm, userSettings)
        }

        appManager.addConnectedApplication(app)

        map += (protocolName -> (p, app))
    }
  }

  private def removeProtocol(protocolName: String, p: ProtocolInterface, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(protocolName) match {
      case None => logger.warn("Protocol not found: " + protocolName)
      case Some((p, app)) =>
        map -= protocolName
        appManager.removeConnectedApplication(app)
    }
  }

  /*
  private var map = Map.empty[Protocol, ConnectedApplication]
  //private var wrapperMap = Map.empty[ProtocolAdapter, Protocol]
  private var mgrMap = Map.empty[String, (ProtocolManager, ConnectedApplication)]

  override def propertyFiles = super.propertyFiles ::: List("org.totalgrid.reef.fep")

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) = {

    context watchServices withInterface[Protocol] andHandle {
      case AddingService(p, _) => addProtocol(context, p, appManager)
      case ServiceRemoved(p, _) => removeProtocol(p, appManager)
    }

    /**
     * Produces a wrapper for Java protocol adapters, adding them just like the current Scala one is added
     */
    /*context watchServices withInterface[ProtocolAdapter] andHandle {
      case AddingService(p, _) =>
        val wrapper = new ScadaProtocolAdapter(p)
        wrapperMap += p -> wrapper
        addProtocol(context, wrapper, appManager)
      case ServiceRemoved(p, _) =>
        wrapperMap.get(p).foreach { x =>
          wrapperMap -= p
          removeProtocol(x, appManager)
        }

    }*/

  }

  private def addProtocolManager(context: BundleContext, name: String, mgr: ProtocolManager, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(p) match {
      case Some(x) => logger.info("Protocol already added: " + p.name)
      case None =>

        // load up the protocol specific user (if configured)
        val userProperties = OsgiConfigReader.load(context, propertyFiles ::: List("org.totalgrid.reef.protocol." + p.name))
        val userSettings = new UserSettings(userProperties)

        val app = new FepConnectedApplication(p, userSettings)

        appManager.addConnectedApplication(app)

        map = map + (p -> app)
    }
  }

  private def addProtocol(context: BundleContext, p: Protocol, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(p) match {
      case Some(x) => logger.info("Protocol already added: " + p.name)
      case None =>

        // load up the protocol specific user (if configured)
        val userProperties = OsgiConfigReader.load(context, propertyFiles ::: List("org.totalgrid.reef.protocol." + p.name))
        val userSettings = new UserSettings(userProperties)

        val app = new FepConnectedApplication(p, userSettings)

        appManager.addConnectedApplication(app)

        map = map + (p -> app)
    }
  }

  private def removeProtocol(p: Protocol, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(p) match {
      case Some(app) =>
        map = map - p
        appManager.removeConnectedApplication(app)

      case None => logger.warn("Protocol not found: " + p.name)
    }
  }*/

}
