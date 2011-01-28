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
package org.totalgrid.reef.metrics

import org.totalgrid.reef.proto.Measurements._

trait NonOperationalDataSink {
  def addMeasurement(meas: Measurement.Builder)

  def nonOp(source: String, variable: String, value: String): Unit = nonOp(source + "." + variable, value)
  def nonOp(source: String, variable: String, value: Int): Unit = nonOp(source + "." + variable, value)
  def nonOp(source: String, variable: String, value: Double): Unit = nonOp(source + "." + variable, value)

  def nonOp(name: String, value: String): Unit = {
    nonOp(name) { b =>
      b.setType(Measurement.Type.STRING)
      b.setStringVal(value)
    }
  }

  def nonOp(name: String, value: Int): Unit = {
    nonOp(name) { b =>
      b.setType(Measurement.Type.INT)
      b.setIntVal(value)
    }
  }

  def nonOp(name: String, value: Double): Unit = {
    nonOp(name) { b =>
      b.setType(Measurement.Type.DOUBLE)
      b.setDoubleVal(value)
    }
  }

  private def nonOp(name: String)(buildfun: Measurement.Builder => Unit) {
    val meas = Measurement.newBuilder
      .setName(name)
      .setQuality(Quality.newBuilder)
    buildfun(meas)

    addMeasurement(meas)
  }
}

/// Turns non operational values into single measurement batches and publishes them
class NonOperationalDataPublisher(publish: MeasurementBatch => Unit) extends NonOperationalDataSink {

  def addMeasurement(meas: Measurement.Builder) {
    val batch = MeasurementBatch.newBuilder
    batch.setWallTime(System.currentTimeMillis)
    batch.addMeas(meas)
    publish(batch.build)
  }

}