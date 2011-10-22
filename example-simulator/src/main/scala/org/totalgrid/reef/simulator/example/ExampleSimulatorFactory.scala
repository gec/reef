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

import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.api.protocol.api.Publisher
import org.totalgrid.reef.api.protocol.simulator.{ SimulatorPlugin, SimulatorPluginFactory }
import org.totalgrid.reef.proto.{ Commands, Measurements, SimMapping }
import com.weiglewilczek.slf4s.Logging
import scala.collection.JavaConversions._

object ExampleSimulatorFactory extends SimulatorPluginFactory with Logging {

  def name = "Breaker Sim"

  case class SimNames(status: String,
    trip: String,
    close: String,
    kwA: String,
    kwB: String,
    kwC: String)

  def getSimNames(config: SimMapping.SimulatorMapping): Option[SimNames] = {

    val status = config.getMeasurementsList.find(m => m.getName.endsWith(".Status")).map(_.getName)
    val kwA = config.getMeasurementsList.find(m => m.getName.endsWith(".kW_a")).map(_.getName)
    val kwB = config.getMeasurementsList.find(m => m.getName.endsWith(".kW_b")).map(_.getName)
    val kwC = config.getMeasurementsList.find(m => m.getName.endsWith(".kW_c")).map(_.getName)

    val trip = config.getCommandsList.find(m => m.getName.endsWith(".Trip")).map(_.getName)
    val close = config.getCommandsList.find(m => m.getName.endsWith(".Close")).map(_.getName)

    List(status, trip, close, kwA, kwB, kwC).flatten match {
      case List(a, b, c, d, e, f) => Some(SimNames(a, b, c, d, e, f))
      case _ => None
    }
  }

  def getSimLevel(endpointName: String, config: SimMapping.SimulatorMapping): Int = getSimNames(config) match {
    case Some(x) => 1
    case None => -1
  }

  def createSimulator(endpointName: String, executor: Executor, publisher: Publisher[Measurements.MeasurementBatch], config: SimMapping.SimulatorMapping): SimulatorPlugin = {
    val names = getSimNames(config).get
    logger.info("Binding new ExampleSimulator with config: " + names.toString)
    new ExampleBreakerSimulator(executor, publisher, names)
  }

  def destroySimulator(plugin: SimulatorPlugin): Unit = {}
}

class ExampleBreakerSimulator(executor: Executor, publisher: Publisher[Measurements.MeasurementBatch], names: ExampleSimulatorFactory.SimNames) extends SimulatorPlugin with Logging {

  def factory: SimulatorPluginFactory = ExampleSimulatorFactory
  def simLevel: Int = 1

  executor.execute(publisher.publish(createBreakerBatch(false)))

  def createAnalog(name: String, unit: String, value: Double) = {
    Measurements.Measurement.newBuilder
      .setName(name)
      .setQuality(Measurements.Quality.newBuilder)
      .setUnit(unit)
      .setType(Measurements.Measurement.Type.DOUBLE)
      .setDoubleVal(value).build
  }

  def createStatus(name: String, unit: String, value: Boolean) = {
    Measurements.Measurement.newBuilder
      .setName(name)
      .setQuality(Measurements.Quality.newBuilder)
      .setUnit(unit)
      .setType(Measurements.Measurement.Type.BOOL)
      .setBoolVal(value).build
  }

  def createBreakerBatch(tripped: Boolean): Measurements.MeasurementBatch = {
    val power = if (tripped) 0.0 else 20.0
    val status = !tripped

    Measurements.MeasurementBatch.newBuilder
      .setWallTime(System.currentTimeMillis)
      .addMeas(createAnalog(names.kwA, "kW", power))
      .addMeas(createAnalog(names.kwB, "kW", power))
      .addMeas(createAnalog(names.kwC, "kW", power))
      .addMeas(createStatus(names.status, "status", status)).build
  }

  def issue(cr: Commands.CommandRequest): Commands.CommandStatus = cr.getName match {
    case names.trip =>
      executor.execute(publisher.publish(createBreakerBatch(true)))
      Commands.CommandStatus.SUCCESS
    case names.close =>
      executor.execute(publisher.publish(createBreakerBatch(false)))
      Commands.CommandStatus.SUCCESS
    case _ =>
      logger.error("Unknown command for example simulator: " + cr)
      Commands.CommandStatus.NOT_SUPPORTED
  }

}