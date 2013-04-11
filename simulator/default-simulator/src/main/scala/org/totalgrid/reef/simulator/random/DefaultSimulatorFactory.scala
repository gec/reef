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
package org.totalgrid.reef.simulator.random

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.protocol.api.Publisher
import org.totalgrid.reef.client.service.proto.{ Measurements, SimMapping }
import org.totalgrid.reef.protocol.simulator.SimulatorPluginFactory
import net.agileautomata.executor4s.{ Cancelable, Executor }

class DefaultSimulatorFactory(register: DefaultSimulator => Cancelable) extends SimulatorPluginFactory with Logging {

  def name = "benchmark"

  val map = collection.mutable.Map.empty[DefaultSimulator, Cancelable]

  def getSimLevel(endpointName: String, config: SimMapping.SimulatorMapping): Int = 0

  def create(endpointName: String, executor: Executor, publisher: Publisher[Measurements.MeasurementBatch], config: SimMapping.SimulatorMapping): DefaultSimulator = map.synchronized {
    val sim = new DefaultSimulator(endpointName, publisher, config, executor, this)
    val cancelable = register(sim)
    map += sim -> cancelable
    sim
  }

  def remove(sim: DefaultSimulator) = map.synchronized {
    map.remove(sim).foreach(_.cancel())
  }

}