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

import org.totalgrid.reef.proto.{ Measurements, SimMapping }
import java.util.Random
import org.totalgrid.reef.proto.Measurements.Measurement.Type

/////////////////////////////////////////////////
// Simple random walk simulation components
/////////////////////////////////////////////////

object RandomValues {

  private val rand = new Random

  def apply(config: SimMapping.MeasSim): RandomValue = {
    config.getType match {
      case Type.BOOL => BooleanValue(config.getInitial.toInt == 0, config.getChangeChance)
      case Type.DOUBLE => DoubleValue(config.getInitial, config.getMin, config.getMax, config.getMaxDelta, config.getChangeChance)
      case Type.INT => IntValue(config.getInitial.toInt, config.getMin.toInt, config.getMax.toInt, config.getMaxDelta.toInt, config.getChangeChance)
    }
  }

  abstract class RandomValue {

    def changeProbability: Double

    def newChangeProbablity(p: Double): RandomValue

    def next(): Option[RandomValue] = {
      if (rand.nextDouble > changeProbability) None
      else Some(generate)
    }

    def generate(): RandomValue

    def apply(meas: Measurements.Measurement.Builder)
  }

  case class DoubleValue(value: Double, min: Double, max: Double, maxChange: Double, changeProbability: Double) extends RandomValue {
    def generate() = this.copy(value = value + maxChange * 2 * ((rand.nextDouble - 0.5)).max(min).min(max))
    def apply(meas: Measurements.Measurement.Builder) = meas.setDoubleVal(value).setType(Type.DOUBLE)
    def newChangeProbablity(p: Double) = this.copy(changeProbability = p)
  }
  case class IntValue(value: Int, min: Int, max: Int, maxChange: Int, changeProbability: Double) extends RandomValue {
    def generate() = this.copy(value = (value + rand.nextInt(2 * maxChange + 1) - maxChange).max(min).min(max))
    def apply(meas: Measurements.Measurement.Builder) = meas.setIntVal(value).setType(Type.INT)
    def newChangeProbablity(p: Double) = this.copy(changeProbability = p)
  }
  case class BooleanValue(value: Boolean, changeProbability: Double) extends RandomValue {
    def generate() = this.copy(value = !value, changeProbability)
    def apply(meas: Measurements.Measurement.Builder) = meas.setBoolVal(value) setType (Type.BOOL)
    def newChangeProbablity(p: Double) = this.copy(changeProbability = p)
  }

}
