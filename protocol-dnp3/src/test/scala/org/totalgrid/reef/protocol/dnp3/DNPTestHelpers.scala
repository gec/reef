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

    (1 to numBinary).foreach(i => add(i, "binary", DataType.BINARY))
    (1 to numAnalog).foreach(i => add(i, "analog", DataType.ANALOG))
    (1 to numCounter).foreach(i => add(i, "counter", DataType.COUNTER))
    (1 to numControlStatus).foreach(i => add(i, "contolStatus", DataType.CONTROL_STATUS))
    (1 to numSetpointStatus).foreach(i => add(i, "setpointStatus", DataType.SETPOINT_STATUS))

    (1 to numControl).foreach(i => addC(i, "control", CommandType.LATCH_ON))
    (1 to numSetpoint).foreach(i => addC(i, "setpoint", CommandType.SETPOINT))

    index.build
  }

}