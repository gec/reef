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
import org.totalgrid.reef.measurementstore.MeasSink.Meas
import org.totalgrid.reef.api.proto.Measurements
import org.totalgrid.reef.api.proto.Measurements.MeasurementSnapshot

import org.totalgrid.reef.services.core.SyncServiceShims._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.japi.BadRequestException
import org.totalgrid.reef.models.DatabaseUsingTestBase

class FakeRTDatabase(map: Map[String, Meas]) extends RTDatabase {
  def get(names: Seq[String]): Map[String, Meas] = {
    map.filterKeys(k => names.exists(_ == k))
  }
}

@RunWith(classOf[JUnitRunner])
class MeasurementSnapshotServiceTest extends DatabaseUsingTestBase {

  def makeMeas(name: String, time: Int) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(0)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  def getMeas(names: String*) = {
    import scala.collection.JavaConversions._
    MeasurementSnapshot.newBuilder().addAllPointNames(names).build
  }

  test("Get Measurements from RTDB") {
    val points = Map("meas1" -> makeMeas("meas1", 0), "meas2" -> makeMeas("meas2", 0))
    val service = new MeasurementSnapshotService(new FakeRTDatabase(points))

    val getMeas1 = service.get(getMeas("meas1")).expectOne()
    getMeas1.getMeasurementsCount() should equal(1)

    val getMeas1and2 = service.get(getMeas("meas1", "meas2")).expectOne()
    getMeas1and2.getMeasurementsCount() should equal(2)

    val getAllMeas = service.get(getMeas("*")).expectOne()
    getAllMeas.getMeasurementsCount() should equal(0)
  }

  test("Bad Request for unknown points") {
    val points = Map("meas" -> makeMeas("meas1", 0))
    val service = new MeasurementSnapshotService(new FakeRTDatabase(points))

    val exception = intercept[BadRequestException] {
      service.get(getMeas("crazyName")).expectOne()
    }
    exception.getMessage should include("crazyName")
  }

  test("Bad Request for some unknown points") {
    val points = Map("meas" -> makeMeas("meas1", 0))
    val service = new MeasurementSnapshotService(new FakeRTDatabase(points))

    val exception = intercept[BadRequestException] {
      service.get(getMeas("meas", "crazyName")).expectOne()
    }
    exception.getMessage should include("crazyName")
  }

  test("Blank Request returns ok") {
    val points = Map("meas" -> makeMeas("meas1", 0))
    val service = new MeasurementSnapshotService(new FakeRTDatabase(points))

    val result = service.get(getMeas()).expectOne()
    result.getPointNamesCount should equal(0)
  }
}