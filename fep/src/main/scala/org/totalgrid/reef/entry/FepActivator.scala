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
package org.totalgrid.reef.entry

import org.osgi.framework._

import org.totalgrid.reef.protocol.api.Protocol
import org.totalgrid.reef.protocol.api.ScadaProtocolAdapter

import com.weiglewilczek.scalamodules._

import org.totalgrid.reef.app._

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.app.whiteboard.ConnectedApplicationBundleActivator
import org.totalgrid.reef.protocol.api.scada.ProtocolAdapter

final class FepActivator extends ConnectedApplicationBundleActivator {

  private var map = Map.empty[Protocol, ConnectedApplication]
  private var wrapperMap = Map.empty[ProtocolAdapter, Protocol]

  def addApplication(context: BundleContext, connectionManager: ConnectionProvider, appManager: ConnectedApplicationManager, executor: Executor) = {

    context watchServices withInterface[Protocol] andHandle {
      case AddingService(p, _) => addProtocol(p, appManager)
      case ServiceRemoved(p, _) => removeProtocol(p, appManager)
    }

    /**
     * Produces a wrapper for Java protocol adapters, adding them just like the current Scala one is added
     */
    context watchServices withInterface[ProtocolAdapter] andHandle {
      case AddingService(p, _) =>
        val wrapper = new ScadaProtocolAdapter(p)
        wrapperMap += p -> wrapper
        addProtocol(wrapper, appManager)
      case ServiceRemoved(p, _) =>
        wrapperMap.get(p).foreach { x =>
          wrapperMap -= p
          removeProtocol(x, appManager)
        }

    }

  }

  private def addProtocol(p: Protocol, appManager: ConnectedApplicationManager) = map.synchronized {
    map.get(p) match {
      case Some(x) => logger.info("Protocol already added: " + p.name)
      case None =>
        val app = new FepConnectedApplication(p)

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
  }

}
