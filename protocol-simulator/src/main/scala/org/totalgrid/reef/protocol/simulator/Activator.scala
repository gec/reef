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
package org.totalgrid.reef.api.protocol.simulator

import org.osgi.framework.{ BundleActivator, BundleContext }
import org.totalgrid.reef.api.protocol.api.{ ChannelAlwaysOnline, EndpointAlwaysOnline, Protocol }

import com.weiglewilczek.scalamodules._
import com.weiglewilczek.slf4s.Logging

import net.agileautomata.executor4s.Executors

class Activator extends BundleActivator with Logging {

  val exe = Executors.newScheduledSingleThread()
  val protocol = new SimulatedProtocol(exe) with EndpointAlwaysOnline with ChannelAlwaysOnline

  final override def start(context: BundleContext) {
    context.createService(protocol, "protocol" -> protocol.name, interface[Protocol])
    context.createService(protocol, "protocol" -> protocol.name, interface[SimulatorManagement])

    context watchServices withInterface[SimulatorPluginFactory] andHandle {
      case AddingService(plugin, properties) =>
        logger.info("Adding a new SimulatorPlugin: " + plugin.getClass.getName)
        protocol.addPluginFactory(plugin)
      case ServiceRemoved(plugin, properties) =>
        logger.info("Removing a SimulatorPlugin: " + plugin.getClass.getName)
        protocol.removePluginFactory(plugin)
    }
  }

  final override def stop(context: BundleContext) = exe.terminate()

}