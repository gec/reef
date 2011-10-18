/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.measurementstore

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.annotation._

import org.totalgrid.reef.util.Timing
import org.totalgrid.reef.api.proto.Measurements
import org.scalatest._

@RunWith(classOf[JUnitRunner])
class InMemoryMeasurementStoreTest extends MeasurementStoreTest {
  val cm = new InMemoryMeasurementStore()
}

abstract class MeasurementStoreTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  val cm: MeasurementStore

  def getMeas(name: String, time: Int, value: Int = Int.MaxValue) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  override def beforeEach() {
    cm.reset
  }

  def testDensity(points: Int, measurements: Int, name: String) = {

    val baseLine = cm.dbSize

    if (baseLine.isDefined) {
      for (batch <- 1 to points) {
        val meas = for {
          i <- 1 to measurements
        } yield getMeas(name + batch, i)
        cm.set(meas)
        cm.archive(name + batch, Long.MaxValue)
      }
      cm.numValues(name + 1) should equal(measurements)
      val after = cm.dbSize
      val bytes = after.get - baseLine.get
      println(bytes + " for " + (points) + " points w/" + measurements + " meas each " + (bytes / (points * measurements)))
      (bytes, points, measurements * points)
    } else {
      (0, 0, 0)
    }
  }

  ignore("HistorianWriteDensity") {
    val d1 = testDensity(10, 5000, "DenseMeas1")
    val d2 = testDensity(10, 1000, "DenseMeas2")
    val d3 = testDensity(50, 1000, "DenseMeas3")
    val d4 = testDensity(1, 25000, "DenseMeas4")

    val plist = cm.points
    if (plist != Nil) plist.size should equal(d1._2 + d2._2 + d3._2 + d4._2)
  }

  test("HistorianWriteReadRemove") {
    val name = "MeasWithColumns"

    cm.remove(List(name))
    cm.numValues(name) should equal(0)

    val num = 10
    val meas1 = for (i <- 1 to num) yield getMeas(name, i)

    val meas = if (cm.supportsOutOfOrderInsertion) scala.util.Random.shuffle(meas1) else meas1

    cm.set(meas)
    cm.archive(name, Long.MaxValue)
    cm.numValues(name) should equal(num)

    // make sure the newest value is allways the same as the "current value"
    cm.getNewest(name) should equal(cm.get(name))

    cm.getOldest(name).get.getTime should equal(1)
    cm.getNewest(name).get.getTime should equal(10)

    cm.getNewest(name, 2) match {
      case List(x, y) =>
        x.getTime should equal(10)
        y.getTime should equal(9)
      case _ => assert(false)
    }

    cm.getOldest(name, 2) match {
      case List(x, y) =>
        x.getTime should equal(1)
        y.getTime should equal(2)
      case _ => assert(false)
    }

    cm.getInRange(name, 3, 5, 100, true) match {
      case List(x, y, z) =>
        x.getTime should equal(3)
        y.getTime should equal(4)
        z.getTime should equal(5)
      case _ => assert(false)
    }

    cm.getInRange(name, 3, 5, 100, false) match {
      case List(x, y, z) =>
        x.getTime should equal(5)
        y.getTime should equal(4)
        z.getTime should equal(3)
      case _ => assert(false)
    }

    // cm.remove(name)
    // cm.getNewest(name) should equal(None)
  }

  test("RTDbWriteReadRemove") {
    val basename = "MeasWithRows"

    val num = 10
    val meas = for (i <- 1 to num) yield getMeas(basename + i, i)

    cm.remove(meas.map { _.getName })
    cm.set(meas)
    meas.foreach { m => cm.archive(m.getName, Long.MaxValue) }

    //Test a batch get
    val result = cm.get(meas.map { m => m.getName })

    result.size should equal(num)

    meas.foreach { m =>
      result(m.getName).getName should equal(m.getName)
      result(m.getName).getTime should equal(m.getTime)
    }

  }

  test("Multiple Values with Same Time") {
    val name = "MultipleValues"

    if (cm.supportsMultipleMeasurementsPerMillisecond) {
      cm.remove(List(name))
      cm.numValues(name) should equal(0)

      val num = 10
      // put in 10 measurements in 5 time slots with the values 0,1
      val meas = for (i <- 0 to num - 1) yield getMeas(name, i / 2, i % 2)

      cm.set(meas)
      meas.foreach { m => cm.archive(m.getName, Long.MaxValue) }

      val allOld = cm.getOldest(name, 10000)
      allOld.size should equal(num)
      allOld should equal(meas)

      val allNew = cm.getNewest(name, 10000)
      allNew.size should equal(num)
      allNew should equal(meas.reverse)

      cm.numValues(name) should equal(num)

      cm.getNewest(name).get should equal(cm.get(name).get)

      cm.getOldest(name).get.getTime should equal(0)
      cm.getNewest(name).get.getTime should equal(4)

      cm.getNewest(name, 2) match {
        case List(x, y) =>
          x.getTime should equal(4)
          x.getIntVal should equal(1)
          y.getTime should equal(4)
          y.getIntVal should equal(0)
        case _ => assert(false)
      }

      cm.getOldest(name, 2) match {
        case List(x, y) =>
          x.getTime should equal(0)
          x.getIntVal should equal(0)
          y.getTime should equal(0)
          y.getIntVal should equal(1)
        case _ => assert(false)
      }

      cm.getInRange(name, 1, 2, 100, true) match {
        case List(w, x, y, z) =>
          w.getTime should equal(1)
          w.getIntVal should equal(0)
          x.getTime should equal(1)
          x.getIntVal should equal(1)
          y.getTime should equal(2)
          y.getIntVal should equal(0)
          z.getTime should equal(2)
          z.getIntVal should equal(1)
        case _ => assert(false)
      }

      cm.getInRange(name, 1, 2, 100, false) match {
        case List(w, x, y, z) =>
          w.getTime should equal(2)
          x.getTime should equal(2)
          y.getTime should equal(1)
          z.getTime should equal(1)
        case _ => assert(false)
      }

      cm.remove(List(name))
      cm.getNewest(name) should equal(None)
    }
  }

  test("Get Partially Known Meas") {
    val basename = "KnownMeas"

    val num = 10
    val existingMeas = for (i <- 1 to num) yield getMeas(basename + i, i)

    val lookup = for (i <- 1 to 2 * num) yield basename + i

    cm.remove(lookup)

    cm.set(existingMeas)
    existingMeas.foreach { m => cm.archive(m.getName, Long.MaxValue) }

    val result = cm.get(lookup)

    result.size should equal(num)

    cm.numValues(basename + (num * 10)) should equal(0)
  }

  test("Get Unknown Meas") {
    val basename = "UnknownMeas"

    val num = 10
    val lookup = for (i <- 1 to num) yield basename + i

    cm.remove(lookup)

    val result = cm.get(lookup)

    result.size should equal(0)
  }

  test("Trim Points") {

    if (cm.supportsTrim) {

      val basename = "TrimmedPoints"

      val num = 20
      val meas = for (i <- 1 to num) yield getMeas(basename, 100 + i)

      cm.remove(meas.map { _.getName })
      cm.set(meas)

      cm.trim(5)

      cm.getOldest(basename, num).size should equal(5)

      cm.getNewest(basename).get.getTime should equal(120)
      cm.getOldest(basename).get.getTime should equal(116)
    }
  }

}