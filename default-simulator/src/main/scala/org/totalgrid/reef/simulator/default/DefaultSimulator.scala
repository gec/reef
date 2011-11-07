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
package org.totalgrid.reef.simulator.default

import com.weiglewilczek.slf4s.Logging

import net.agileautomata.executor4s._

import java.util.Random
import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.{ SimMapping, Measurements, Commands }

import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, Measurement => Meas }
import org.totalgrid.reef.api.protocol.api.Publisher
import org.totalgrid.reef.api.protocol.simulator.{ ControllableSimulator, SimulatorPluginFactory, SimulatorPlugin }

class DefaultSimulator(name: String, publisher: Publisher[MeasurementBatch], config: SimMapping.SimulatorMapping, exe: Executor, parent: SimulatorPluginFactory)
    extends SimulatorPlugin with ControllableSimulator with Logging {

  case class MeasRecord(name: String, unit: String, currentValue: CurrentValue[_])

  val strand = Strand(exe)

  private val measurements = config.getMeasurementsList.map { x => MeasRecord(x.getName, x.getUnit, getValueHolder(x)) }.toList
  private val cmdMap = config.getCommandsList.map { x => x.getName -> x.getResponseStatus }.toMap
  private val rand = new Random
  private var delay: Long = config.getDelay

  private var timer: Option[Timer] = None

  strand.execute(start)

  private def start() = {
    //Todo - place startup code here
  }

  def shutdown() = strand.terminate(timer.foreach(_.cancel()))

  private def repeat() = update(measurements)

  def getRepeatDelay = delay

  def setUpdateParams(newDelay: Long) = {
    // if the delay is 0 we shouldn't publish any random values after
    // the initial integrity poll
    delay = newDelay
    logger.info("Updating parameters for simulator: " + name + ", delay = " + delay)
    this.synchronized {
      timer.foreach(_.cancel)
      if (delay > 0) timer = Some(exe.scheduleWithFixedOffset(delay.milliseconds) { repeat }) else None
    }
  }

  def update(meases: List[MeasRecord], force: Boolean = false): Unit = {
    val batch = Measurements.MeasurementBatch.newBuilder.setWallTime(System.currentTimeMillis)
    meases.foreach { meas =>
      if (meas.currentValue.next(force)) {
        batch.addMeas(getMeas(meas))
      }
    }
    if (batch.getMeasCount > 0) {
      logger.debug(name + " publishing batch of size: " + batch.getMeasCount)
      publisher.publish(batch.build)
      logger.debug(name + " published batch")
    }
  }

  /** Generate a random measurement */
  private def getMeas(meas: MeasRecord): Meas = {
    val point = Measurements.Measurement.newBuilder.setName(meas.name)
      .setQuality(Measurements.Quality.newBuilder.build)
      .setUnit(meas.unit)

    meas.currentValue.apply(point)
    point.build
  }

  final override def factory = parent
  final override def simLevel: Int = 0

  final override def issue(cr: Commands.CommandRequest): Commands.CommandStatus = cmdMap.get(cr.getName) match {
    case Some(status) => status
    case None =>
      logger.warn("Response for command not found, returning default")
      Commands.CommandStatus.NOT_SUPPORTED
  }

  /////////////////////////////////////////////////
  // Simple random walk simulation components
  /////////////////////////////////////////////////

  def getValueHolder(config: SimMapping.MeasSim): CurrentValue[_] = {
    config.getType match {
      case Meas.Type.BOOL => BooleanValue(config.getInitial.toInt == 0, config.getChangeChance)
      case Meas.Type.DOUBLE => DoubleValue(config.getInitial, config.getMin, config.getMax, config.getMaxDelta, config.getChangeChance)
      case Meas.Type.INT => IntValue(config.getInitial.toInt, config.getMin.toInt, config.getMax.toInt, config.getMaxDelta.toInt, config.getChangeChance)
    }
  }

  abstract class CurrentValue[A](var value: A, val changeChance: Double) {

    def next(force: Boolean): Boolean = {
      if (force) return true
      if (rand.nextDouble > changeChance) return false
      val original = value
      _next
      original != value
    }

    def _next()
    def apply(meas: Measurements.Measurement.Builder)
  }
  case class DoubleValue(initial: Double, min: Double, max: Double, maxChange: Double, cc: Double) extends CurrentValue[Double](initial, cc) {
    def _next() = value = (value + maxChange * 2 * ((rand.nextDouble - 0.5))).max(min).min(max)
    def apply(meas: Measurements.Measurement.Builder) = meas.setDoubleVal(value).setType(Meas.Type.DOUBLE)
  }
  case class IntValue(initial: Int, min: Int, max: Int, maxChange: Int, cc: Double) extends CurrentValue[Int](initial, cc) {
    def _next() = value = (value + rand.nextInt(2 * maxChange + 1) - maxChange).max(min).min(max)
    def apply(meas: Measurements.Measurement.Builder) = meas.setIntVal(value).setType(Meas.Type.INT)
  }
  case class BooleanValue(initial: Boolean, cc: Double) extends CurrentValue[Boolean](initial, cc) {
    def _next() = value = !value
    def apply(meas: Measurements.Measurement.Builder) = meas.setBoolVal(value) setType (Meas.Type.BOOL)
  }

}