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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.messaging.{ AMQPProtoFactory, ProtoServiceException }
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MeasurementBatchServiceTest extends EndpointRelatedTestBase {
  import org.totalgrid.reef.measproc.ProtoHelper._
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementBatch }

  class BatchFixture(amqp: AMQPProtoFactory) extends CoordinatorFixture(amqp) {
    val batchService = new MeasurementBatchService(amqp)

    def addFepAndMeasProc() {
      addProtocols(addApp("both", List("FEP", "Processing")))
    }

  }

  test("Putting Batch when no FEP Fails") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")

      intercept[ProtoServiceException] {
        coord.batchService.put(makeBatch(makeInt("dev1.test_point", 10)))
      }
    }
  }

  test("Putting A Batch succeeds") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")
      coord.addFepAndMeasProc()

      coord.pointsInDatabase should equal(1)
      coord.pointsWithBadQuality should equal(1)

      val measProcAssign = one(coord.measProcConnection.get(MeasurementProcessingConnection.newBuilder.setUid("*").build))

      var mb = coord.listenForMeasurements(measProcAssign)

      one(coord.batchService.put(makeBatch(makeInt("dev1.test_point", 10))))

      mb.waitFor({ _.size == 1 }, 1000)

      mb.current.head.getMeasCount should equal(1)
      mb.current.head.getMeas(0).getName should equal("dev1.test_point")

      coord.pointsInDatabase should equal(1)
      coord.pointsWithBadQuality should equal(0)
    }
  }

  test("Multiple Batchs are routed correctly") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new BatchFixture(amqp)

      coord.addDevice("dev1")
      coord.addDevice("dev2")
      coord.addFepAndMeasProc()

      coord.pointsInDatabase should equal(2)
      coord.pointsWithBadQuality should equal(2)

      val measProcAssign = many(2, coord.measProcConnection.get(MeasurementProcessingConnection.newBuilder.setUid("*").build))

      measProcAssign.head.getRouting.getServiceRoutingKey should (not be ==(measProcAssign.tail.head.getRouting.getServiceRoutingKey))

      var mb = coord.listenForMeasurements(measProcAssign.head)
      var mb2 = coord.listenForMeasurements(measProcAssign.tail.head)

      many(2, coord.batchService.put(makeBatch(makeInt("dev1.test_point", 10) :: makeInt("dev2.test_point", 10) :: Nil)))

      mb.waitFor({ _.size == 1 }, 1000)
      mb2.waitFor({ _.size == 1 }, 1000)

      mb.current.head.getMeasCount should equal(1)
      mb2.current.head.getMeasCount should equal(1)

      coord.pointsInDatabase should equal(2)
      coord.pointsWithBadQuality should equal(0)
    }
  }

}