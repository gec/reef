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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.mock.AMQPFixture

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.api.Envelope.Event

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.api.Envelope

@RunWith(classOf[JUnitRunner])
class CommunicationStreamCoordinationTest extends EndpointRelatedTestBase {

  test("Add Order: Device, FEP, Meas") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val device = coord.addDnp3Device("dev1")

      coord.pointsInDatabase should equal(1)

      coord.checkAssignments(1, None, None)

      val fep = coord.addFep("fep")
      coord.checkAssignments(1, Some(fep), None)
      val fepEvents = coord.subscribeFepAssignements(1, fep)

      val meas = coord.addMeasProc("meas")
      coord.checkAssignments(1, Some(fep), Some(meas))

      // we should have sent an update to the fep to tell it the new routing address
      fepEvents.pop(5000).event should equal(Event.MODIFIED)
    }
  }

  test("Add Order: Device, Meas, FEP") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val device = coord.addDnp3Device("dev1")

      coord.pointsInDatabase should equal(1)

      val meas = coord.addMeasProc("meas")
      coord.checkAssignments(1, None, Some(meas))

      val fep = coord.addFep("fep")
      coord.checkAssignments(1, Some(fep), Some(meas))
    }
  }

  test("Add Order: Meas, FEP, Device") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val fep = coord.addFep("fep")
      val meas = coord.addMeasProc("meas")

      val device = coord.addDnp3Device("dev1")
      coord.pointsInDatabase should equal(1)
      coord.checkAssignments(1, Some(fep), Some(meas))
    }
  }

  test("Add Order: Combo(Meas, FEP), Device") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val bothApp = coord.addApp("both", List("FEP", "Processing"))
      val fepPart = coord.addProtocols(bothApp)

      val device = coord.addDnp3Device("dev1")
      coord.pointsInDatabase should equal(1)
      coord.checkAssignments(1, Some(fepPart), Some(bothApp))
    }
  }

  test("Many devices, one handler") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      for (i <- 1 to 3) yield coord.addDevice("dev" + i)
      for (i <- 4 to 6) yield coord.addDnp3Device("dev" + i)

      coord.pointsInDatabase should equal(6)

      val fep = coord.addFep("fep")
      val meas = coord.addMeasProc("meas")

      coord.checkAssignments(6, Some(fep), Some(meas))
    }
  }

  test("Many devices, many balanced handlers") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val feps = for (i <- 1 to 3) yield coord.addFep("fep" + i)
      val procs = for (i <- 1 to 3) yield coord.addMeasProc("meas" + i)

      for (i <- 1 to 9) yield coord.addDevice("dev" + i)

      coord.pointsInDatabase should equal(9)

      feps.foreach { fepUid =>
        val fassign = many(3, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepUid).build))
        coord.checkFeps(fassign, false, Some(fepUid), true)
      }
      procs.foreach { measUid =>
        val massign = many(3, coord.measProcConnection.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measUid).build))
        coord.checkMeasProcs(massign, Some(measUid), true)
      }
    }
  }

  test("FEPS have correct ports") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val meas = coord.addMeasProc("meas")
      val fepNetALocA = coord.addProtocols(coord.addApp("fepNetALocA", List("FEP"), "netA", "locA"), List("dnp3", "benchmark"))
      val fepNetBLocA = coord.addProtocols(coord.addApp("fepNetBLocA", List("FEP"), "netB", "locA"), List("dnp3", "benchmark"))
      val fepNetBLocB = coord.addProtocols(coord.addApp("fepNetBLocB", List("FEP"), "netB", "locB"), List("dnp3", "benchmark"))

      val serialLocA1 = coord.addDnp3Device("serialLocA1", None, Some("locA"))
      val serialLocA2 = coord.addDnp3Device("serialLocA2", None, Some("locA"))
      val serialLocB1 = coord.addDnp3Device("serialLocB1", None, Some("locB"))
      val serialLocB2 = coord.addDnp3Device("serialLocB2", None, Some("locB"))

      coord.checkFeps(many(1, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetALocA).build)), false, Some(fepNetALocA), true)
      coord.checkFeps(many(1, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocA).build)), false, Some(fepNetBLocA), true)
      coord.checkFeps(many(2, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocB).build)), false, Some(fepNetBLocB), true)

      val ipNetA1 = coord.addDnp3Device("ipNetA1", Some("netA"), None)
      val ipNetA2 = coord.addDnp3Device("ipNetA2", Some("netA"), None)
      val ipNetB1 = coord.addDnp3Device("ipNetB1", Some("netB"), None)
      val ipNetB2 = coord.addDnp3Device("ipNetB2", Some("netB"), None)

      coord.checkFeps(many(1 + 2, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetALocA).build)), false, Some(fepNetALocA), true)
      coord.checkFeps(many(1 + 2, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocA).build)), false, Some(fepNetBLocA), true)
      coord.checkFeps(many(2, coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocB).build)), false, Some(fepNetBLocB), true)

    }
  }

}