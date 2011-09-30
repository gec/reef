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
package org.totalgrid.reef.protocol.dnp3

import org.totalgrid.reef.proto.Measurements.{ Quality, Measurement }

object DNPTestHelpers {
  def makeMappingProto(numBinary: Int, numAnalog: Int, numCounter: Int, numControlStatus: Int, numSetpointStatus: Int, numControl: Int, numSetpoint: Int) = {
    import org.totalgrid.reef.proto.Mapping._

    val index = IndexMapping.newBuilder

    def add(i: Int, n: String, t: DataType) {
      index.addMeasmap(MeasMap.newBuilder.setIndex(i).setPointName(n + i).setType(t).setUnit("raw"))
    }
    def addC(i: Int, n: String, t: CommandType) {
      index.addCommandmap(CommandMap.newBuilder.setCommandName(n + i).setType(t).setIndex(i))
    }

    (0 until numBinary).foreach(i => add(i, "binary", DataType.BINARY))
    (0 until numAnalog).foreach(i => add(i, "analog", DataType.ANALOG))
    (0 until numCounter).foreach(i => add(i, "counter", DataType.COUNTER))
    (0 until numControlStatus).foreach(i => add(i, "controlStatus", DataType.CONTROL_STATUS))
    (0 until numSetpointStatus).foreach(i => add(i, "setpointStatus", DataType.SETPOINT_STATUS))

    (0 until numControl).foreach(i => addC(i, "control", CommandType.LATCH_ON))
    (0 until numSetpoint).foreach(i => addC(i, "setpoint", CommandType.SETPOINT))

    index.build
  }

  def makeAnalogMeas(name: String, value: Double, time: Long = 9999) = {
    Measurement.newBuilder.setName(name).setDoubleVal(value).setTime(time)
      .setType(Measurement.Type.DOUBLE).setQuality(Quality.newBuilder).build
  }
  def makeAnalogIntMeas(name: String, value: Int, time: Long = 9999) = {
    Measurement.newBuilder.setName(name).setIntVal(value).setTime(time)
      .setType(Measurement.Type.INT).setQuality(Quality.newBuilder).build
  }
  def makeBinaryMeas(name: String, value: Boolean, time: Long = 9999) = {
    Measurement.newBuilder.setName(name).setBoolVal(value).setTime(time)
      .setType(Measurement.Type.BOOL).setQuality(Quality.newBuilder).build
  }
}