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
package org.totalgrid.reef.protocol.benchmark

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.executor.{ Executor, Lifecycle }
import org.totalgrid.reef.util.Timer

import java.util.Random
import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.{ SimMapping, Measurements, Commands }
import org.totalgrid.reef.util.Conversion.convertIterableToMapified

import org.totalgrid.reef.protocol.api.{ CommandHandler, Listener }
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, Measurement => Meas }
import org.totalgrid.reef.proto.Commands.CommandResponse

class Simulator(name: String, publisher: Listener[MeasurementBatch], config: SimMapping.SimulatorMapping, reactor: Executor) extends Lifecycle with CommandHandler with ControllableSimulator with Logging {

  case class MeasRecord(name: String, unit: String, currentValue: CurrentValue[_])

  private var delay: Long = config.getDelay

  private val measurements = config.getMeasurementsList.map { x => MeasRecord(x.getName, x.getUnit, getValueHolder(x)) }.toList
  private val cmdMap = config.getCommandsList.map { x => x.getName -> x.getResponseStatus }.toMap

  private val rand = new Random
  private var repeater: Option[Timer] = None

  override def afterStart() {
    reactor.execute { update(measurements, true) }
    setUpdateParams(delay)
  }
  override def beforeStop() {
    this.synchronized {
      repeater.foreach { _.cancel }
    }
  }

  def getRepeatDelay = delay

  def setUpdateParams(newDelay: Long) = {
    // if the delay is 0 we shouldn't publish any random values after
    // the initial integrity poll
    delay = newDelay
    logger.info("Updating parameters for simulator " + name + " delay = " + delay)
    this.synchronized {
      repeater.foreach(_.cancel)
      repeater = if (delay == 0) None else Some(reactor.repeat(delay) {
        update(measurements.toList)
      })
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
      publisher.onUpdate(batch.build)
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

  def issue(cr: Commands.CommandRequest, rspHandler: Listener[CommandResponse]) = cmdMap.get(cr.getName) match {
    case Some(x) =>
      logger.info("handled command: " + cr)
      val rsp = Commands.CommandResponse.newBuilder
      rsp.setCorrelationId(cr.getCorrelationId).setStatus(x)
      rspHandler.onUpdate(rsp.build)
    case None =>
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