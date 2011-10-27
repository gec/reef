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

import org.totalgrid.reef.services.ConnectionFixture
import org.totalgrid.reef.clientapi.sapi.client.rest.Connection
import org.totalgrid.reef.clientapi.exceptions.ReefServiceException
import org.totalgrid.reef.clientapi.sapi.client.Response
import org.totalgrid.reef.util.AsyncValue

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MeasurementBatchServiceTest extends EndpointRelatedTestBase {
  import org.totalgrid.reef.measproc.ProtoHelper._
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  import org.totalgrid.reef.proto.Measurements.MeasurementBatch

  class BatchFixture(amqp: Connection) extends CoordinatorFixture(amqp) {

    val batchService = new MeasurementBatchService()

    def addFepAndMeasProc() {
      addFep("fep", List("benchmark"))
      addMeasProc("meas")
    }

    def publishMeas(meas: MeasurementBatch): MeasurementBatch = {
      val result = new AsyncValue[Response[MeasurementBatch]]
      batchService.putAsync(contextSource, meas)(result.set)
      result.await().expectOne()
    }
  }

  test("Putting Batch when no FEP Fails") {
    ConnectionFixture.mock() { amqp =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")

      intercept[ReefServiceException] {
        coord.publishMeas(makeBatch(makeInt("dev1.test_point", 10)))
      }
    }
  }

  test("Putting A Batch succeeds") {
    ConnectionFixture.mock() { amqp =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")
      coord.addFepAndMeasProc()

      coord.pointsInDatabase should equal(1)
      coord.pointsWithBadQuality should equal(1)

      val mb = coord.listenForMeasurements("meas")

      coord.publishMeas(makeBatch(makeInt("dev1.test_point", 10)))

      mb.waitFor({ _.size == 1 }, 1000)

      val (device, meas) = mb.current.head

      device should equal("dev1")

      meas.getMeasCount should equal(1)
      meas.getMeas(0).getName should equal("dev1.test_point")

      coord.pointsInDatabase should equal(1)
      coord.pointsWithBadQuality should equal(0)
    }
  }

  test("Multiple Batchs are routed correctly") {
    ConnectionFixture.mock() { amqp =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")
      coord.addDevice("dev2")
      coord.addFepAndMeasProc()

      coord.pointsInDatabase should equal(2)
      coord.pointsWithBadQuality should equal(2)

      val mb = coord.listenForMeasurements("meas")

      val batch = makeBatch(makeInt("dev1.test_point", 10) :: makeInt("dev2.test_point", 10) :: Nil)

      coord.publishMeas(batch)

      mb.waitFor({ _.size == 2 }, 1000)

      val meases = mb.current

      meases.map { _._1 }.sorted should equal(List("dev1", "dev2"))
      meases.map { _._2.getMeasCount } should equal(List(1, 1))

      coord.pointsInDatabase should equal(2)
      coord.pointsWithBadQuality should equal(0)
    }
  }

}