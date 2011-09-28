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
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.totalgrid.reef.proto.Mapping.{ DataType, MeasMap }
import org.totalgrid.reef.protocol.dnp3.mock.MockDataObserver
import org.totalgrid.reef.proto.Measurements.{ Quality, Measurement }
import org.totalgrid.reef.protocol.dnp3.{ DNPTestHelpers, AnalogQuality }

@RunWith(classOf[JUnitRunner])
class DataObserverPublisherTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {

  import DNPTestHelpers._

  // ignored until we can fix the c++ memory weirdness
  test("Translates measurements") {
    val map = makeMappingProto(2, 2, 2, 2, 2, 0, 0)

    val mock = new MockDataObserver

    val observer = new DataObserverPublisher(map, mock)

    observer.publishMeasurements(
      makeAnalogMeas("analog0", 100.0) ::
        makeBinaryMeas("binary1", false) ::
        makeAnalogIntMeas("counter0", 50) ::
        makeBinaryMeas("controlStatus1", true) ::
        makeAnalogIntMeas("setpointStatus0", 75) :: Nil)

    val goodQuality = AnalogQuality.AQ_ONLINE.swigValue().toShort

    mock.analogs.head._2 should equal(0)
    mock.analogs.head._1.GetValue() should equal(100.0)
    mock.analogs.head._1.GetQuality() should equal(goodQuality)

    mock.counters.head._2 should equal(0)
    mock.counters.head._1.GetValue() should equal(50)
    mock.counters.head._1.GetQuality() should equal(goodQuality)

    mock.controlStatus.head._2 should equal(1)
    mock.controlStatus.head._1.GetValue() should equal(true)
    mock.controlStatus.head._1.GetQuality() should equal(goodQuality + 128) // the value is in the quality field (highest bit)

    mock.setpointStatus.head._2 should equal(0)
    mock.setpointStatus.head._1.GetValue() should equal(75)
    mock.setpointStatus.head._1.GetQuality() should equal(goodQuality)

    mock.binaries.head._2 should equal(1)
    mock.binaries.head._1.GetValue() should equal(false)
    mock.binaries.head._1.GetQuality() should equal(goodQuality)
  }

}