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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.measurementstore.{ InMemoryMeasurementStore, Historian }
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas }
import org.totalgrid.reef.client.service.proto.Measurements
import org.totalgrid.reef.client.service.proto.Measurements.MeasurementHistory

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point }
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.sapi.client.Expectations._

class FakeHistorian(measStore: Historian) extends Historian {
  var begin: Long = -1
  var end: Long = -1
  var max: Long = -1
  var ascending = false
  def getInRange(name: String, b: Long, e: Long, m: Int, a: Boolean): Seq[Meas] = {
    begin = b; end = e; max = m; ascending = a
    measStore.getInRange(name, b, e, m, a)
  }

  def numValues(name: String): Int = { throw new Exception }
  def remove(names: Seq[String]): Unit = { throw new Exception }
}

@RunWith(classOf[JUnitRunner])
class MeasurementHistoryServiceTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {

  class Fixture {
    val factories = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))
    val pointService = sync(new PointService(factories.points))
    val measStore = new InMemoryMeasurementStore(false)
    measStore.set(List(
      makeMeas("meas1", 0, 1), makeMeas("meas1", 1, 1),
      makeMeas("meas2", 100, 1), makeMeas("meas2", 200, 2), makeMeas("meas2", 300, 3),
      makeMeas("meas3", 1, 88), makeMeas("meas3", 6, 99), makeMeas("meas3", 15, 111)))
    val historian = new FakeHistorian(measStore)
    val service = sync(new MeasurementHistoryService(historian))

    private def makeMeas(name: String, time: Int, value: Int) = {

      val basicPoint = Point.newBuilder.setName(name).setType(PointType.ANALOG).setUnit("raw").build
      pointService.put(basicPoint).expectOne()

      val meas = Measurements.Measurement.newBuilder.setUnit("raw")
      meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
      meas.setQuality(Measurements.Quality.newBuilder.build)
      meas.setTime(time)
      meas.build
    }

    def validateHistorian(begin: Long, end: Long, max: Int, ascending: Boolean) = {
      historian.begin should equal(begin)
      historian.end should equal(end)
      historian.max should equal(max)
      historian.ascending should equal(ascending)
    }
  }

  test("History Service defaults are sensible") {
    val f = new Fixture

    val getMeas1 = f.service.get(MeasurementHistory.newBuilder.setPointName("meas1").build).expectOne()
    getMeas1.getMeasurementsCount() should equal(2)

    f.validateHistorian(0, Long.MaxValue, 10000, false)

    f.service.get(MeasurementHistory.newBuilder().setPointName("meas1").setKeepNewest(false).setEndTime(1000).build).expectOne()
    f.validateHistorian(0, 1000, 10000, true)

    f.service.get(MeasurementHistory.newBuilder().setPointName("meas1").setLimit(99).build).expectOne()
    f.validateHistorian(0, Long.MaxValue, 99, false)
  }

  test("History Service request by pointName") {
    val f = new Fixture

    val getMeas1 = f.service.get(MeasurementHistory.newBuilder.setPointName("meas1").build).expectOne()
    getMeas1.getMeasurementsCount() should equal(2)

    val getMeas2 = f.service.get(MeasurementHistory.newBuilder.setPointName("meas2").build).expectOne()
    getMeas2.getMeasurementsCount() should equal(3)
  }

  test("History Service request by uuid") {
    val f = new Fixture

    val point = f.pointService.get(Point.newBuilder.setName("meas1").build).expectOne()

    val getMeas1 = f.service.get(MeasurementHistory.newBuilder.setPoint(point).build).expectOne()
    getMeas1.getMeasurementsCount() should equal(2)

    val getMeas2 = f.service.get(MeasurementHistory.newBuilder.setPoint(Point.newBuilder.setUuid(point.getUuid)).build).expectOne()
    getMeas2.getMeasurementsCount() should equal(2)
  }

  test("History Service unknown Point") {
    val f = new Fixture

    intercept[BadRequestException] {
      f.service.get(MeasurementHistory.newBuilder.setPointName("meas44").build).expectOne()
    }

    intercept[BadRequestException] {
      f.service.get(MeasurementHistory.newBuilder.setPoint(Point.newBuilder.setName("asdasd")).build).expectOne()
    }
  }

  test("History Service overerly specified") {
    val f = new Fixture

    intercept[BadRequestException] {
      f.service.get(MeasurementHistory.newBuilder.setPointName("meas1").setPoint(Point.newBuilder.setName("meas1")).build).expectOne()
    }
  }

  test("History Service history add previous point if we didn't hit limit") {
    val f = new Fixture

    val getMeas1 = f.service.get(MeasurementHistory.newBuilder.setPointName("meas1").setStartTime(4).build).expectOne()
    getMeas1.getMeasurementsCount() should equal(1)
    getMeas1.getMeasurements(0).getTime should equal(1)
    getMeas1.getMeasurements(0).getIntVal should equal(1)

  }

  test("History Service history dont add previous point if we hit limit") {
    val f = new Fixture

    val getMeas1 = f.service.get(MeasurementHistory.newBuilder.setPointName("meas3").setStartTime(10).setLimit(1).build).expectOne()
    getMeas1.getMeasurementsCount() should equal(1)
    getMeas1.getMeasurements(0).getTime should equal(15)
    getMeas1.getMeasurements(0).getIntVal should equal(111)

  }
}