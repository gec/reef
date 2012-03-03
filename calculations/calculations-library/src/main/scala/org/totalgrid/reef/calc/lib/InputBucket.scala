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
package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Calculations.CalculationInput
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import scala.collection.mutable
import org.totalgrid.reef.client.service.proto.Calculations.SingleMeasurement.MeasurementStrategy

sealed trait MeasRequest
case object SingleLatest extends MeasRequest
case class MultiSince(from: Long, limit: Int) extends MeasRequest
case class MultiLimit(count: Int) extends MeasRequest

trait MeasBucket {
  def onReceived(m: Measurement)
}

trait InputBucket extends MeasBucket {
  def variable: String

  def getSnapshot: Option[List[Measurement]]

  def getMeasRequest: MeasRequest
}

case class InputConfig(point: String, bucket: InputBucket)

object InputBucket {

  def build(calc: CalculationInput): InputConfig = {

    val pointName = calc.point.name.getOrElse { throw new Exception("Must have input point name") }

    val variable = calc.variableName.getOrElse { throw new Exception("Must have input variable name") }

    // hold the last 100 measurements unless configured otherwise
    val limit = calc.range.limit.getOrElse(100)

    if (limit > 10000) throw new Exception("Limit is larger than 10000 max: " + limit)

    val bucket = calc.single.strategy.map {
      case MeasurementStrategy.MOST_RECENT => new SingleLatestBucket(variable)
      case x => throw new Exception("Uknown single measurement strategy: " + x)
    } orElse {
      calc.range.fromMs.map(from => new FromRangeBucket(SystemTimeSource, variable, from, limit))
    } orElse {
      calc.range.limit.map(lim => new LimitRangeBucket(variable, limit))
    } getOrElse {
      throw new Exception("Cannot build input from configuration: " + pointName + " " + variable + " config: " + calc)
    }

    InputConfig(pointName, bucket)
  }

  class FromRangeBucket(timeSource: TimeSource, val variable: String, from: Long, limit: Int, minimum: Int = 1) extends InputBucket {

    def getMeasRequest = MultiSince(from, limit)

    private val queue = new mutable.Queue[Measurement]()

    protected def prune() {
      val horizon = timeSource.now + from
      while (queue.headOption.map { _.getTime <= horizon }.getOrElse(false)) {
        queue.dequeue()
      }

      while (queue.size > limit) {
        queue.dequeue()
      }
    }

    def onReceived(m: Measurement) = {
      queue.enqueue(m)
      prune()
    }

    def getSnapshot = {
      // we may need to throw out old measurements before returning snapshot
      prune()
      if (queue.size >= minimum) Some(queue.toList)
      else None
    }
  }

  class LimitRangeBucket(val variable: String, limit: Int, minimum: Int = 1) extends InputBucket {

    def getMeasRequest = MultiLimit(limit)

    private val queue = new mutable.Queue[Measurement]()

    def onReceived(m: Measurement) {
      queue.enqueue(m)
      while (queue.size > limit) {
        queue.dequeue()
      }
    }

    def getSnapshot = if (queue.size >= minimum) Some(queue.toList) else None
  }

  class SingleLatestBucket(val variable: String) extends InputBucket {

    def getMeasRequest = SingleLatest

    protected var meas: Option[Measurement] = None

    def onReceived(m: Measurement) {
      meas = Some(m)
    }

    def getSnapshot = if (meas.isDefined) Some(meas.toList) else None
  }
}

