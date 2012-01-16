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

object MeasurementUtility {

  def createStream(originals: List[Measurement], size: Int, nowMillis: Long) = {

    val prev = new scala.collection.mutable.Queue[Measurement]()
    prev ++= originals

    def add = {
      val m = prev.dequeue()

      val meas = if (m.getType == Measurement.Type.DOUBLE) {
        m.toBuilder.setDoubleVal(m.getDoubleVal + 1.0).setTime(nowMillis).build
      } else if (m.getType == Measurement.Type.BOOL || m.getType == Measurement.Type.STRING) {
        m.toBuilder.setBoolVal(!m.getBoolVal).setTime(nowMillis).build
      } else if (m.getType == Measurement.Type.INT) {
        m.toBuilder.setIntVal(m.getIntVal + 1).setTime(nowMillis).build
      } else m.toBuilder.setTime(nowMillis).build

      prev.enqueue(meas)
      meas
    }

    Stream.continually(add).take(size).toList
  }

  def printMeasurements(list: List[Measurement]) = list.map { m => m.getName + "->" + (m.doubleVal orElse m.intVal orElse m.boolVal orElse m.stringVal).getOrElse("") }
}