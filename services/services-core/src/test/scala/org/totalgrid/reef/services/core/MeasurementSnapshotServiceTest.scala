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

import org.totalgrid.reef.measurementstore.RTDatabase
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement => Meas }
import org.totalgrid.reef.client.service.proto.Measurements
import org.totalgrid.reef.client.service.proto.Measurements.MeasurementSnapshot

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.client.Expectations._

import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.models.DatabaseUsingTestBase
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point }

class FakeRTDatabase(map: Map[String, Meas]) extends RTDatabase {
  def get(names: Seq[String]): Map[String, Meas] = {
    map.filterKeys(k => names.exists(_ == k))
  }
}

@RunWith(classOf[JUnitRunner])
class MeasurementSnapshotServiceTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {

  class Fixture {
    val factories = new ModelFactories(new ServiceDependenciesDefaults(dbConnection))
    val pointService = sync(new PointService(factories.points))
    val points = Map("meas1" -> makeMeas("meas1", 0, "type1"), "meas2" -> makeMeas("meas2", 0, "type2"))
    val service = sync(new MeasurementSnapshotService(new FakeRTDatabase(points)))

    private def makeMeas(name: String, time: Int, unit: String) = {

      val basicPoint = Point.newBuilder.setName(name).setType(PointType.ANALOG).setUnit(unit).build
      pointService.put(basicPoint).expectOne()

      val meas = Measurements.Measurement.newBuilder
      meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(0)
      meas.setQuality(Measurements.Quality.newBuilder.build)
      meas.setTime(time)
      meas.build
    }
  }

  def getMeas(names: String*) = {
    import scala.collection.JavaConversions._
    MeasurementSnapshot.newBuilder().addAllPointNames(names).build
  }

  test("Get Measurements from RTDB") {
    val f = new Fixture
    val getMeas1 = f.service.get(getMeas("meas1")).expectOne()
    getMeas1.getMeasurementsCount() should equal(1)

    val getMeas1and2 = f.service.get(getMeas("meas1", "meas2")).expectOne()
    getMeas1and2.getMeasurementsCount() should equal(2)

    val getAllMeas = f.service.get(getMeas("*")).expectOne()
    getAllMeas.getMeasurementsCount() should equal(0)
  }

  test("Bad Request for unknown points") {
    val f = new Fixture
    val exception = intercept[BadRequestException] {
      f.service.get(getMeas("crazyName")).expectOne()
    }
    exception.getMessage should include("crazyName")
  }

  test("Bad Request for some unknown points") {
    val f = new Fixture
    val exception = intercept[BadRequestException] {
      f.service.get(getMeas("meas", "crazyName")).expectOne()
    }
    exception.getMessage should include("crazyName")
  }

  test("Blank Request returns ok") {
    val f = new Fixture
    val result = f.service.get(getMeas()).expectOne()
    result.getPointNamesCount should equal(0)
  }

  test("Request measurements using Point objects") {
    val f = new Fixture
    val point = f.pointService.get(Point.newBuilder.setName("meas1").build).expectOne()

    def sameMeas(name: String, point: Point) {
      sameMeases(List(name), List(point))
    }

    def sameMeases(names: List[String], pointProtos: List[Point]) {
      val result = f.service.get(MeasurementSnapshot.newBuilder.addAllPoint(pointProtos).build).expectOne()

      result.getMeasurementsList.toList should equal(names.map { f.points(_) })
    }

    sameMeas("meas1", point)
    sameMeas("meas1", Point.newBuilder.setName("meas1").build)
    sameMeas("meas1", Point.newBuilder.setUuid(point.getUuid).build)

    sameMeas("meas1", Point.newBuilder.setUnit("type1").build)
    sameMeas("meas2", Point.newBuilder.setUnit("type2").build)

    sameMeases(List("meas1", "meas2"), List(Point.newBuilder.setName("meas1").build, Point.newBuilder.setUnit("type2").build))
    sameMeases(List("meas2", "meas2"), List(Point.newBuilder.setUnit("type2").build, Point.newBuilder.setUnit("type2").build))
    sameMeases(List("meas1", "meas2"), List(Point.newBuilder.setType(PointType.ANALOG).build))

    sameMeases(List("meas1", "meas2"), List(Point.newBuilder.setName("*").build))
  }
}