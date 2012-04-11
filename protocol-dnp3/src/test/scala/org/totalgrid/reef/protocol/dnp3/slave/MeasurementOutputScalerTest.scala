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
package org.totalgrid.reef.protocol.dnp3.slave

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import org.totalgrid.reef.protocol.dnp3.DNPTestHelpers._
import org.totalgrid.reef.client.service.proto.Mapping.{ DataType, MeasMap }

@RunWith(classOf[JUnitRunner])
class MeasurementOutputScalerTest extends FunSuite with ShouldMatchers {

  private def makeMap(name: String, scaling: Option[Double] = None) = {
    val b = MeasMap.newBuilder.setIndex(0).setUnit("raw").setPointName(name).setType(DataType.ANALOG)
    scaling.foreach(b.setScaling(_))
    b.build
  }

  test("No Scaling") {

    val mappings = List(makeMap("test"))
    val scaler = new MeasurementOutputScaler(mappings)

    val analog = makeAnalogMeas("test", 99.8)
    scaler.scaleMeasurement(analog) should equal(analog)

    val analogInt = makeAnalogIntMeas("test", 22)
    scaler.scaleMeasurement(analogInt) should equal(analogInt)

    val unknown = makeAnalogMeas("unknown", 99.8)
    scaler.scaleMeasurement(unknown) should equal(unknown)
  }

  test("Scale by 100.0") {

    val mappings = List(makeMap("test", Some(100.0)))
    val scaler = new MeasurementOutputScaler(mappings)

    val analog = makeAnalogMeas("test", 99.8)
    scaler.scaleMeasurement(analog) should equal(makeAnalogMeas("test", 9980))

    val analogInt = makeAnalogIntMeas("test", 22)
    scaler.scaleMeasurement(analogInt) should equal(makeAnalogIntMeas("test", 2200))

    val unknown = makeAnalogMeas("unknown", 99.8)
    scaler.scaleMeasurement(unknown) should equal(unknown)
  }
}