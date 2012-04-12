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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.sapi.rpc.impl.builders.MeasurementRequestBuilders
import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class MeasurementBatchTest extends ServiceClientSuite {

  def putMeas(m: Measurement) = client.publishMeasurements(m :: Nil)
  def putAll(m: List[Measurement]) = client.publishMeasurements(m)

  test("Simple puts") {
    val pointName = "StaticSubstation.Line02.Current"
    // read the current value so we can edit it
    val original = client.getMeasurementByName(pointName)

    // double the value and post it
    val updated = original.toBuilder.setDoubleVal(original.getDoubleVal * 2).setTime(System.currentTimeMillis).build
    putMeas(updated)

    putMeas(original.toBuilder.setTime(System.currentTimeMillis).build)
  }

  test("Multi put") {
    val names = List("StaticSubstation.Line02.Current", "StaticSubstation.Breaker02.Bkr", "StaticSubstation.Breaker02.Tripped")
    val originals = client.getMeasurementsByNames(names)

    val updated = updateMeasurements(originals.toList, System.currentTimeMillis())

    putAll(updated)

    val reverted = originals.map { m => m.toBuilder.setTime(System.currentTimeMillis).build }.toList
    putAll(reverted)
  }

  def updateMeasurements(originals: List[Measurement], nowMillis: Long) = {
    originals.map { m =>
      if (m.getType == Measurement.Type.DOUBLE)
        m.toBuilder.setDoubleVal(m.getDoubleVal + 1.0).setTime(nowMillis).build
      else if (m.getType == Measurement.Type.BOOL)
        m.toBuilder.setBoolVal(!m.getBoolVal).setTime(nowMillis).build
      else if (m.getType == Measurement.Type.INT)
        m.toBuilder.setIntVal(m.getIntVal + 1).setTime(nowMillis).build
      else m.toBuilder.setTime(nowMillis).build
    }
  }

  test("Fails with bad name") {
    val pName = "StaticSubstation.Line02.UnknownPoint"

    val ex = intercept[BadRequestException] {
      putMeas(MeasurementRequestBuilders.makeIntMeasurement(pName, 22, 1000))
    }
    ex.getMessage should include("StaticSubstation.Line02.UnknownPoint")
  }

  test("Fails with some good, some bad") {
    val pointNames = List("StaticSubstation.Line02.UnknownPoint", "StaticSubstation.Line02.Current")

    val ex = intercept[BadRequestException] {
      putAll(pointNames.map { pName => MeasurementRequestBuilders.makeIntMeasurement(pName, 22, 1000) })
    }
    ex.getMessage should include("StaticSubstation.Line02.UnknownPoint")

    val ex2 = intercept[BadRequestException] {
      putAll(pointNames.reverse.map { pName => MeasurementRequestBuilders.makeIntMeasurement(pName, 22, 1000) })
    }
    ex2.getMessage should include("StaticSubstation.Line02.UnknownPoint")
  }

}