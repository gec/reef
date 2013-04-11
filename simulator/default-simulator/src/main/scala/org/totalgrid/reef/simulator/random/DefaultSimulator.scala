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

import net.agileautomata.executor4s._

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.{ SimMapping, Measurements, Commands }

import org.totalgrid.reef.protocol.api.Publisher
import org.totalgrid.reef.simulator.random.RandomValues.RandomValue
import org.totalgrid.reef.client.service.proto.Commands.CommandStatus
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement, MeasurementBatch }
import org.totalgrid.reef.protocol.simulator.{ ControllableSimulator, SimulatorPlugin }

final class DefaultSimulator(simName: String, publisher: Publisher[MeasurementBatch], config: SimMapping.SimulatorMapping, exe: Executor, parent: DefaultSimulatorFactory)
    extends SimulatorPlugin with ControllableSimulator with Logging {

  val strand = Strand(exe)

  private var timer = Option.empty[Timer]
  private var delayMs: Long = config.getDelay

  private var state: State = {
    val measurements = config.getMeasurementsList.map(x => x.getName -> MeasRecord(x.getName, x.getUnit, RandomValues(x))).toMap
    val commands = config.getCommandsList.map { x => x.getName -> x.getResponseStatus }.toMap
    State(measurements, commands)
  }

  // do an integrity poll on all values at startup
  strand.execute {
    mutate { state =>
      publish(state.measurements.values.map(_.measurement))
      state
    }
    startTimer()
  }

  def shutdown() = {
    strand.terminate {
      parent.remove(this)
      cancelTimer()
    }
  }

  private case class MeasRecord(name: String, unit: String, value: RandomValue) {
    def measurement: Measurements.Measurement = {
      val point = Measurements.Measurement.newBuilder.setName(name)
        .setQuality(Measurements.Quality.newBuilder.build)
        .setUnit(unit)

      value.apply(point)
      point.build
    }
  }

  private case class State(measurements: Map[String, MeasRecord], commands: Map[String, CommandStatus]) {

    def randomizeMeasurements(): State = {
      val meas = measurements.values.foldLeft(measurements) { (map, record) =>
        record.value.next() match {
          case Some(x) => map + (record.name -> record.copy(value = x))
          case None => map
        }
      }
      this.copy(measurements = meas)
    }
  }

  def update() = mutate { state =>
    val s2 = state.randomizeMeasurements()
    val changes = s2.measurements.values.filterNot(r => r.value == state.measurements(r.name).value)

    publish(changes.map(_.measurement))
    s2
  }

  private def mutate(fun: State => State) = synchronized(state = fun(state))

  private def publish(list: Iterable[Measurement]): Unit = if (list.nonEmpty) {
    val batch = Measurements.MeasurementBatch.newBuilder.setWallTime(System.currentTimeMillis)
    list.foreach(batch.addMeas)
    logger.debug(name + " publishing batch of size: " + batch.getMeasCount)
    publisher.publish(batch.build)
    logger.debug(name + " published batch")
  }

  /* Implement Simulator plugin */

  override def name: String = simName

  override def factory = parent
  override def simLevel: Int = 0

  override def issue(cr: Commands.CommandRequest): Commands.CommandStatus = state.commands.get(cr.getCommand.getName) match {
    case Some(status) => status
    case None =>
      logger.warn("Response for command not found, returning default")
      Commands.CommandStatus.NOT_SUPPORTED
  }

  /* Implement ControllableSimulator */

  override def setChangeProbability(prob: Double) = mutate { state =>
    val meas = state.measurements.values.foldLeft(Map.empty[String, MeasRecord]) { (sum, rec) =>
      sum + (rec.name -> rec.copy(value = rec.value.newChangeProbablity(prob)))
    }
    state.copy(measurements = meas)
  }

  private def cancelTimer() {
    timer.foreach(_.cancel())
    timer = None
  }

  private def startTimer() {
    timer = if (delayMs > 0) Some(strand.scheduleWithFixedOffset(delayMs.milliseconds)(update()))
    else None
  }

  override def getRepeatDelay = delayMs
  override def setUpdateDelay(newDelay: Long) {
    logger.info("Updating parameters for simulator: " + name + ", delay = " + newDelay)
    cancelTimer()
    delayMs = newDelay
    startTimer()
  }
}