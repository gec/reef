/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.benchmarks.measurements

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Measurements

object MeasurementUtility {

  def createStream(originals: Seq[Measurement], size: Int, nowMillis: Long) = {

    val prev = new scala.collection.mutable.Queue[Measurement]()
    prev ++= originals

    def add = {
      val m = prev.dequeue()

      val builder = m.toBuilder
      if (m.getType == Measurement.Type.DOUBLE) {
        builder.setDoubleVal(m.getDoubleVal + 1.0)
      } else if (m.getType == Measurement.Type.BOOL || m.getType == Measurement.Type.STRING) {
        builder.setBoolVal(!m.getBoolVal)
        builder.setType(Measurement.Type.BOOL)
      } else if (m.getType == Measurement.Type.INT) {
        builder.setIntVal(m.getIntVal + 1)
      } else {
        throw new RuntimeException("Measurement has no initial type")
      }

      val meas = builder.setTime(nowMillis).build

      prev.enqueue(meas)
      meas
    }

    Stream.continually(add).take(size).toList
  }

  def printMeasurements(list: List[Measurement]) = list.map { m =>
    m.getName + "->" + (m.doubleVal orElse m.intVal orElse m.boolVal orElse m.stringVal).getOrElse("Blank")
  }

  var measValue = -1000

  def makeMeasurements(pointNames: List[String], size: Int) = {
    // lazily prepare all of the measurements, wraps around point names if more measurements than points

    def nextValue: Int = {
      measValue = (measValue + 1)
      if (measValue >= 1000) measValue = -1000
      measValue
    }

    def makeMeas(name: String, value: Int, time: Long) = {
      val meas = Measurements.Measurement.newBuilder
      meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
      meas.setQuality(Measurements.Quality.newBuilder.build)
      meas.setTime(time)
      meas.setSystemTime(time)
      meas.build
    }

    val now = System.currentTimeMillis()
    val names = Stream.continually(pointNames).flatten.take(size)
    names.map { makeMeas(_, nextValue, now) }
  }
}