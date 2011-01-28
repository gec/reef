/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.protocol.benchmark

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.reactor.{ Reactable, Lifecycle, DelayHandler }

import java.util.Random
import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.{ Mapping, Measurements, Commands }
import Measurements.{ Measurement => Meas }

import org.totalgrid.reef.util.Conversion.convertIterableToMapified

import org.totalgrid.reef.protocol.api.IProtocol

class Simulator(name: String, publish: IProtocol.Publish, respondFun: IProtocol.Respond, config: Mapping.IndexMapping, reactor: Reactable) extends Lifecycle with Logging {

  case class MeasRecord(name: String, dataType: Measurements.Measurement.Type, currentValue: CurrentValue)

  private var delay = 500
  private var batchSize = 10
  private val list = config.getMeasmapList.map { x => MeasRecord(x.getPointName, getType(x.getType), getValueHolder(getType(x.getType))) }
  private val cmdMap = config.getCommandmapList.map { x => x.getCommandName }.mapify { x => x }
  private val rand = new Random
  private var repeater: Option[DelayHandler] = None

  override def afterStart() {
    setUpdateParams(delay, batchSize)
  }
  override def beforeStop() {
    repeater.foreach { _.cancel }
  }

  def setUpdateParams(newDelay: Int, newBatchSize: Int) = reactor.execute {
    delay = newDelay
    batchSize = newBatchSize
    info { "Updating parameters for " + name + ": delay = " + delay + " batch_size = " + batchSize }
    repeater.foreach(_.cancel)
    repeater = Some(reactor.repeat(delay) { update })
  }

  /** Pick batchSize random values to update from the map */
  def update: Unit = {
    val batch = Measurements.MeasurementBatch.newBuilder.setWallTime(System.currentTimeMillis)
    for (i <- 1 to batchSize) {
      val meas = list(rand.nextInt(list.size))
      batch.addMeas(getMeas(meas))
    }
    debug { "publishing batch of size: " + batch.getMeasCount }
    publish(batch.build)
  }

  /** Generate a random measurement */
  private def getMeas(meas: MeasRecord): Meas = {
    val point = Measurements.Measurement.newBuilder.setName(meas.name)
      .setType(meas.dataType)
      .setQuality(Measurements.Quality.newBuilder.build)
      .setUnit("raw")

    meas.currentValue.next()
    meas.currentValue.apply(point)

    point.build
  }

  def issue(cr: Commands.CommandRequest): Unit = reactor.execute {
    cmdMap.get(cr.getName) match {
      case Some(x) =>
        info { "handled command:" + cr }
        val rsp = Commands.CommandResponse.newBuilder
        rsp.setCorrelationId(cr.getCorrelationId).setStatus(Commands.CommandStatus.SUCCESS)
        respondFun(rsp.build)
      case None =>
    }
  }

  /////////////////////////////////////////////////
  // Simple random walk simulation components
  /////////////////////////////////////////////////

  abstract class CurrentValue {
    def next()
    def apply(meas: Measurements.Measurement.Builder)
  }
  case class DoubleValue(var value: Double, min: Double, max: Double, maxChange: Double) extends CurrentValue {
    def next() = value = (value + maxChange * 2 * ((rand.nextDouble - 0.5))).max(min).min(max)
    def apply(meas: Measurements.Measurement.Builder) = meas.setDoubleVal(value)
  }
  case class IntValue(var value: Int, min: Int, max: Int, maxChange: Int) extends CurrentValue {
    def next() = value = (value + rand.nextInt(2 * maxChange) - maxChange).max(min).min(max)
    def apply(meas: Measurements.Measurement.Builder) = meas.setIntVal(value)
  }
  case class BooleanValue(var value: Boolean, chanceOfNotChanging: Double) extends CurrentValue {
    def next() = value = if (rand.nextDouble < chanceOfNotChanging) value else !value
    def apply(meas: Measurements.Measurement.Builder) = meas.setBoolVal(value)
  }

  def getType(dt: Mapping.DataType): Measurements.Measurement.Type = {
    dt match {
      case Mapping.DataType.ANALOG => Meas.Type.DOUBLE
      case Mapping.DataType.BINARY => Meas.Type.BOOL
      case Mapping.DataType.COUNTER => Meas.Type.INT
      case Mapping.DataType.CONTROL_STATUS => Meas.Type.BOOL
      case Mapping.DataType.SETPOINT_STATUS => Meas.Type.DOUBLE
    }
  }

  def getValueHolder(dt: Measurements.Measurement.Type): CurrentValue = {
    dt match {
      case Meas.Type.DOUBLE => DoubleValue(0, -50, 50, 2)
      case Meas.Type.BOOL => BooleanValue(false, 0.99)
      case Meas.Type.INT => IntValue(0, 0, 100, 2)
    }
  }
}