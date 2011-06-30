package org.totalgrid.reef.protocol.simulator

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
import org.totalgrid.reef.protocol.api.Publisher
import org.totalgrid.reef.proto.{ Measurements, SimMapping, Commands }
import org.totalgrid.reef.executor.Executor

trait SimulatorPluginFactory {
  def getSimLevel(endpointName: String, config: SimMapping.SimulatorMapping): Int
  def createSimulator(endpointName: String, executor: Executor, publisher: Publisher[Measurements.MeasurementBatch], config: SimMapping.SimulatorMapping): SimulatorPlugin
  def destroySimulator(plugin: SimulatorPlugin): Unit
}

trait SimulatorPlugin {
  def shutdown() = factory.destroySimulator(this)
  def factory: SimulatorPluginFactory
  def simLevel: Int
  def issue(cr: Commands.CommandRequest): Commands.CommandStatus
}