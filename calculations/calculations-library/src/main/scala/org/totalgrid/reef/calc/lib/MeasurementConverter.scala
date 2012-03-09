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

import eval._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

object MeasurementConverter {

  def convertMeasurement(m: Measurement): OperationValue = {
    if (m.getType == Measurement.Type.DOUBLE) {
      NumericMeas(m.getDoubleVal, m.getTime)
    } else if (m.getType == Measurement.Type.INT) {
      LongMeas(m.getIntVal, m.getTime)
    } else if (m.getType == Measurement.Type.BOOL) {
      BooleanMeas(m.getBoolVal, m.getTime)
    } else {
      throw new EvalException("Cannot user measurement as input: " + m)
    }
  }

  def convertOperationValue(v: OperationValue): Measurement.Builder = {
    val b = Measurement.newBuilder

    // weird bug in case matching, see LongValue.unapply
    v match {
      case LongValue(d) =>
        b.setType(Measurement.Type.INT)
        b.setIntVal(d)
      case _ =>
        v match {
          case NumericValue(d) =>
            b.setType(Measurement.Type.DOUBLE)
            b.setDoubleVal(d)
          case BooleanConst(bv) =>
            b.setType(Measurement.Type.BOOL)
            b.setBoolVal(bv)
          case _ =>
            throw new EvalException("Cannot use value as output: " + v)
        }
    }

    b
  }
}
