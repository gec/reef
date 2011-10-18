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
package org.totalgrid.reef.simulator.example

import org.osgi.framework.{ BundleActivator, BundleContext }
import com.weiglewilczek.scalamodules._
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.protocol.simulator.SimulatorPluginFactory

class Activator extends BundleActivator with Logging {

  final override def start(context: BundleContext) = {
    logger.info("Starting ExampleSimulator")
    context.createService(ExampleSimulatorFactory, interface1 = interface[SimulatorPluginFactory])
  }

  final override def stop(context: BundleContext) = {
    logger.info("Stopping ExampleSimulator")
  }

}