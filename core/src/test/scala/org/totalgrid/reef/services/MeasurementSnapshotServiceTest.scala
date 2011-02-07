/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services

import org.totalgrid.reef.measurementstore.RTDatabase
import org.totalgrid.reef.measurementstore.MeasSink.Meas
import org.totalgrid.reef.proto.Measurements
import org.totalgrid.reef.proto.Measurements.MeasurementSnapshot

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.messaging.mock.AMQPFixture

//import org.totalgrid.reef.protoapi.ProtoServiceTypes

class FakeRTDatabase(map: Map[String, Meas]) extends RTDatabase {
  def get(names: Seq[String]): Map[String, Meas] = {
    map.filterKeys(k => names.exists(_ == k))
  }
}

@RunWith(classOf[JUnitRunner])
class MeasurementSnapshotServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll // with RunTestsInsideTransaction
{

  def getMeas(name: String, time: Int) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(0)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  test("Get Measurements from RTDB") {
    AMQPFixture.mock(true) { amqp =>
      val points = Map("meas1" -> getMeas("meas1", 0), "meas2" -> getMeas("meas2", 0))
      val service = new MeasurementSnapshotService(new FakeRTDatabase(points), new SilentServiceSubscriptionHandler {})
      amqp.bindService("test", service.respond)

      val client = amqp.getProtoServiceClient("test", 500000, MeasurementSnapshot.parseFrom)

      val getMeas1 = client.getOneThrow(MeasurementSnapshot.newBuilder().addPointNames("meas1").build)
      getMeas1.getMeasurementsCount() should equal(1)

      val getMeas1and2 = client.getOneThrow(MeasurementSnapshot.newBuilder().addPointNames("meas1").addPointNames("meas2").build)
      getMeas1and2.getMeasurementsCount() should equal(2)

      val getAllMeas = client.getOneThrow(MeasurementSnapshot.newBuilder().addPointNames("*").build)
      getAllMeas.getMeasurementsCount() should equal(0)
    }
  }
}