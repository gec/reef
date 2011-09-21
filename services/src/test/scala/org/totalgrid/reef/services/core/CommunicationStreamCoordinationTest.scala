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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.mock.AMQPFixture

import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.japi.Envelope.Event

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.event.EventType

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
        val fassign = coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepUid).build).expectMany(3)
        coord.checkFeps(fassign, false, Some(fepUid), true)
      }
      procs.foreach { measUid =>
        val massign = coord.measProcConnection.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measUid).build).expectMany(3)
        coord.checkMeasProcs(massign, Some(measUid), true)
      }
    }
  }

  test("Search for Endpoints by channel") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val serialLocA1 = coord.addDnp3Device("serialLocA1", None, Some("locA"), Some("SerialA"))
      val serialLocA2 = coord.addDnp3Device("serialLocA2", None, Some("locA"), Some("SerialA"))
      serialLocA1.getChannel should equal(serialLocA2.getChannel)

      val serialLocB1 = coord.addDnp3Device("serialLocB1", None, Some("locB"), Some("SerialB"))
      val serialLocB2 = coord.addDnp3Device("serialLocB2", None, Some("locB"), Some("SerialB"))
      serialLocB1.getChannel should equal(serialLocB2.getChannel)

      val ipNetA1 = coord.addDnp3Device("ipNetA1", Some("netA"), None, Some("IPA"))
      val ipNetA2 = coord.addDnp3Device("ipNetA2", Some("netA"), None, Some("IPA"))
      val ipNetA3 = coord.addDnp3Device("ipNetA3", Some("netA"), None, Some("IPA"))
      val ipNetB = coord.addDnp3Device("ipNetB", Some("netB"), None, Some("IPB"))
      ipNetA1.getChannel should equal(ipNetA2.getChannel)
      ipNetA1.getChannel should equal(ipNetA3.getChannel)

      ipNetA1.getChannel should not equal (ipNetB.getChannel)
      ipNetA1.getChannel should not equal (serialLocB1.getChannel)
      serialLocB1.getChannel should not equal (serialLocA1.getChannel)

      coord.commEndpointService.get(CommEndpointConfig.newBuilder.setChannel(serialLocA1.getChannel).build).expectMany(2)
      coord.commEndpointService.get(CommEndpointConfig.newBuilder.setChannel(serialLocB1.getChannel).build).expectMany(2)
      coord.commEndpointService.get(CommEndpointConfig.newBuilder.setChannel(ipNetA1.getChannel).build).expectMany(3)
      coord.commEndpointService.get(CommEndpointConfig.newBuilder.setChannel(ipNetB.getChannel).build).expectMany(1)
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

      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetALocA).build).expectOne(), false, Some(fepNetALocA), true)
      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocA).build).expectOne(), false, Some(fepNetBLocA), true)
      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocB).build).expectMany(2), false, Some(fepNetBLocB), true)

      val ipNetA1 = coord.addDnp3Device("ipNetA1", Some("netA"), None)
      val ipNetA2 = coord.addDnp3Device("ipNetA2", Some("netA"), None)
      val ipNetB1 = coord.addDnp3Device("ipNetB1", Some("netB"), None)
      val ipNetB2 = coord.addDnp3Device("ipNetB2", Some("netB"), None)

      // fepNetALocA should have got both netA devices and the fepNetB* feps will split the netB additions
      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetALocA).build).expectMany(2 + 1), false, Some(fepNetALocA), true)
      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocA).build).expectMany(2 + 1), false, Some(fepNetBLocA), true)
      coord.checkFeps(coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setFrontEnd(fepNetBLocB).build).expectMany(1 + 1), false, Some(fepNetBLocB), true)

    }
  }

  test("Disable/Enable Endpoint") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val fep = coord.addFep("fep")
      val meas = coord.addMeasProc("meas")

      val device = coord.addDnp3Device("dev1")
      coord.pointsInDatabase should equal(1)
      coord.checkAssignments(1, Some(fep), Some(meas))

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointDisabled) should equal(0)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointEnabled) should equal(0)

      val connection = coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setEnabled(true).build).expectOne()
      coord.setEndpointEnabled(connection, false)

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointDisabled) should equal(1)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointEnabled) should equal(0)

      // check that we have unassigned the fep
      coord.checkAssignments(1, None, Some(meas))

      // we can also now search by enabled == false
      coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setEnabled(false).build).expectOne()
      coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setEnabled(true).build).expectNone()

      coord.setEndpointEnabled(connection, true)

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointDisabled) should equal(1)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointEnabled) should equal(1)

      coord.checkAssignments(1, Some(fep), Some(meas))
    }
  }

  test("Search connections by state") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val coord = new CoordinatorFixture(amqp)

      val fep = coord.addFep("fep")
      val meas = coord.addMeasProc("meas")

      coord.addDnp3Device("dev1")
      coord.addDnp3Device("dev2")

      coord.pointsInDatabase should equal(2)
      coord.checkAssignments(2, Some(fep), Some(meas))

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOffline) should equal(0)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOnline) should equal(0)

      val connections = coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setState(CommEndpointConnection.State.COMMS_DOWN).build).expectMany(2)
      coord.setEndpointState(connections.head, CommEndpointConnection.State.COMMS_UP)

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOffline) should equal(0)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOnline) should equal(1)

      coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setState(CommEndpointConnection.State.COMMS_DOWN).build).expectOne()
      coord.frontEndConnection.get(CommEndpointConnection.newBuilder.setState(CommEndpointConnection.State.COMMS_UP).build).expectOne()

      coord.setEndpointState(connections.head, CommEndpointConnection.State.COMMS_DOWN)

      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOffline) should equal(1)
      coord.eventSink.getEventCount(EventType.Scada.CommEndpointOnline) should equal(1)
    }
  }

}