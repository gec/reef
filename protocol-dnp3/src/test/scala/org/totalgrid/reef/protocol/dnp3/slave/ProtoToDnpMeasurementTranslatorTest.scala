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
import org.totalgrid.reef.protocol.dnp3.{ Analog, AnalogQuality }

@RunWith(classOf[JUnitRunner])
class ProtoToDnpMeasurementTranslatorTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {
  import ProtoToDnpMeasurementTranslator._
  import org.totalgrid.reef.protocol.dnp3.DNPTestHelpers._

  private val goodQuality = AnalogQuality.AQ_ONLINE.swigValue().toShort
  private val badQuality = AnalogQuality.AQ_COMM_LOST.swigValue().toShort

  private val measName = "meas"
  // we run these tests multiple times to exacerbated any weird memoty issies
  private val tries = 10

  test("convert Analog double") {
    (0 to tries).foreach { i =>
      val obj = getAnalog(makeAnalogMeas(measName, i.toDouble))
      obj.GetValue() should equal(i.toDouble)
      obj.GetQuality() should equal(goodQuality)
    }
  }

  test("convert Analog int") {
    (0 to tries).foreach { i =>
      val obj = getAnalog(makeAnalogIntMeas(measName, i))
      obj.GetValue() should equal(i)
      obj.GetQuality() should equal(goodQuality)
    }
  }

  test("convert Analog Bad meas") {
    (0 to tries).foreach { i =>
      val obj = getAnalog(makeBinaryMeas(measName, i % 2 == 0))
      obj.GetQuality() should equal(badQuality)
    }
  }

  test("convert binary ok") {
    (0 to tries).foreach { i =>
      val obj = getBinary(makeBinaryMeas(measName, i % 2 == 0))
      //obj.GetQuality() should equal(goodQuality)
      obj.GetValue() should equal(i % 2 == 0)
    }
  }

  test("convert binary bad") {
    (0 to tries).foreach { i =>
      val obj = getBinary(makeAnalogMeas(measName, i))
      obj.GetQuality() should equal(badQuality)
    }
  }
}