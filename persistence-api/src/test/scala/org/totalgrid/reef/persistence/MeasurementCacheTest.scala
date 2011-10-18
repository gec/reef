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
package org.totalgrid.reef.persistence

import org.totalgrid.reef.api.proto.Measurements.Measurement

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.api.proto.Measurements

@RunWith(classOf[JUnitRunner])
class InMemoryMeasurementCacheTest extends MeasurementCacheTest {

  val cm = new InMemoryObjectCache[Measurement]()
}

abstract class MeasurementCacheTest extends FunSuite with ShouldMatchers {

  val cm: ObjectCache[Measurement]

  test("Put and Gets work") {

    val original1 = makeAnalog("test", 1, 1)
    val original2 = makeAnalog("test", 2, 2)
    val original3 = makeAnalog("test", 3, 3)
    cm.put("test", original1)
    cm.put("test", original2)
    cm.put("test", original3)

    val retrieved = cm.get("test")

    retrieved.get should equal(original3)
  }

  def makeAnalog(name: String, value: Double, time: Long = System.currentTimeMillis, unit: String = "raw"): Measurements.Measurement = {
    val m = Measurements.Measurement.newBuilder
    m.setTime(time)
    m.setName(name)
    m.setType(Measurements.Measurement.Type.DOUBLE)
    m.setDoubleVal(value)
    m.setQuality(Measurements.Quality.newBuilder.setDetailQual(Measurements.DetailQual.newBuilder))
    m.setUnit(unit)
    m.build
  }
}