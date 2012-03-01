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

trait MeasBucket {
  def onReceived(m: Measurement)
}

trait InputBucket extends MeasBucket {
  def variable: String

  def getSnapshot: List[Measurement]

  def hasSufficient: Boolean
}

object InputBucket {

  case class InputConfig(point: String, variable: String, request: MeasRequest, bucket: InputBucket)

  sealed trait MeasRequest
  case object SingleLatest extends MeasRequest
  case class MultiSince(from: Long) extends MeasRequest
  case class MultiLimit(count: Int) extends MeasRequest

  def build(calc: CalculationInput): InputConfig = {

    val pointName = calc.point.name.getOrElse { throw new Exception("Must have input point name") }

    val variable = calc.variableName.getOrElse { throw new Exception("Must have input variable name") }

    val (request: MeasRequest, bucket: InputBucket) = calc.single.strategy.map {
      case MeasurementStrategy.MOST_RECENT => (SingleLatest, new SingleLatestBucket(variable))
      case x => throw new Exception("Uknown single measurement strategy: " + x)
    } orElse {
      calc.range.limit.map(lim => (MultiLimit(lim), new LimitRangeBucket(variable, lim)))
    } orElse {
      calc.range.fromMs.map(from => (MultiSince(from), new FromRangeBucket(variable, from)))
    } getOrElse {
      throw new Exception("Cannot build input from configuration: " + pointName + " " + variable)
    }

    InputConfig(pointName, variable, request, bucket)
  }

  class FromRangeBucket(val variable: String, from: Long) extends InputBucket {
    private val queue = new mutable.Queue[Measurement]()

    protected def prune() {
      val horizon = System.currentTimeMillis() + from
      while (queue.head.getTime < horizon) {
        queue.dequeue()
      }
    }
    def onReceived(m: Measurement) = {
      queue.enqueue(m)
      prune()
    }
    def getSnapshot: List[Measurement] = {
      prune()
      queue.toList
    }
    def hasSufficient: Boolean = {
      prune()
      queue.size > 0
    }
  }

  class LimitRangeBucket(val variable: String, limit: Int) extends InputBucket {
    private val queue = new mutable.Queue[Measurement]()

    def onReceived(m: Measurement) {
      queue.enqueue(m)
      while (queue.size > limit) {
        queue.dequeue()
      }
    }

    def getSnapshot: List[Measurement] = queue.toList

    def hasSufficient: Boolean = { queue.size == limit }
  }

  class SingleLatestBucket(val variable: String) extends InputBucket {

    protected var meas: Option[Measurement] = None

    def onReceived(m: Measurement) {
      meas = Some(m)
    }

    def getSnapshot: List[Measurement] = meas.toList

    def hasSufficient: Boolean = meas.isDefined
  }
}

