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

import org.totalgrid.reef.services.ConnectionFixture

import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType

@RunWith(classOf[JUnitRunner])
class FEPAssignmentLifecycleTest extends EndpointRelatedTestBase {

  test("One device, dead apps noticed, point taken offline") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      val device = coord.addDevice("dev1")
      val fep = coord.addFep("fep99")
      val meas = coord.addMeasProc("meas99")

      coord.checkPoints(1, 1)
      coord.checkAssignments(1, Some(fep), Some(meas))

      // simulate a measproc shoving a measurement into the rtDatabase
      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)

      // since there is no heartbeat process running for our fake feps, checking for timeouts along time in the future
      // will mean that both apps will have timedout
      coord.heartbeatCoordinator.checkTimeouts(coord.startTime + 1000000)

      coord.checkAssignments(1, None, None)

      // check that the point is now marked as bad in the rtdb
      coord.checkPoints(1, 1)

    }
  }

  test("Switch FEP when protocol changes") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      val meas = coord.addMeasProc("meas")
      val dnp = coord.addFep("dnp3", List("dnp3"))
      val dnpEvents = coord.subscribeFepAssignements(0, dnp)
      val benchmark = coord.addFep("benchmark", List("benchmark"))
      val benchmarkEvents = coord.subscribeFepAssignements(0, benchmark)

      val device1 = coord.addDnp3Device("dev1")

      dnpEvents.pop(5000).event should equal(SubscriptionEventType.MODIFIED)

      coord.checkAssignments(1, Some(dnp), Some(meas))
      coord.checkPoints(1, 1)
      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)

      val device2 = coord.addDevice("dev1")

      dnpEvents.pop(5000).event should equal(SubscriptionEventType.REMOVED)
      benchmarkEvents.pop(5000).event should equal(SubscriptionEventType.ADDED)

      coord.checkAssignments(1, Some(benchmark), Some(meas))
      coord.checkPoints(1, 1)
      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)
    }
  }

  test("Reassign FEP when protocol changes") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      val meas = coord.addMeasProc("meas")
      val fep = coord.addFep("fep")
      val fepEvents = coord.subscribeFepAssignements(0, fep)
      val device1 = coord.addDnp3Device("dev1")

      fepEvents.pop(5000).event should equal(SubscriptionEventType.MODIFIED)

      coord.checkAssignments(1, Some(fep), Some(meas))
      coord.checkPoints(1, 1)
      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)

      val device2 = coord.addDevice("dev1")

      fepEvents.pop(5000).event should equal(SubscriptionEventType.REMOVED)
      fepEvents.pop(5000).event should equal(SubscriptionEventType.ADDED)

      coord.checkAssignments(1, Some(fep), Some(meas))
      coord.checkPoints(1, 1)
      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)
    }
  }

  test("Self Assigned Device, claiming and then unclaiming fep") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      val device = coord.addDevice("dev1", autoAssigned = false)
      val FEP_NAME = "fep"
      val fep = coord.addFep(FEP_NAME)
      val meas = coord.addMeasProc("meas99")

      coord.checkAssignments(1, None, Some(meas))

      val connection = coord.claimEndpoint(device.getName, Some(FEP_NAME))

      connection.getFrontEnd.getAppConfig.getInstanceName should equal(FEP_NAME)

      coord.checkAssignments(1, Some(fep), Some(meas))

      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)

      coord.claimEndpoint(device.getName, None)

      coord.checkAssignments(1, None, Some(meas))

      // check that the point is now marked as bad in the rtdb
      coord.checkPoints(1, 1)
    }
  }

  test("Self Assigned Device, heartbeat out app causes COMMS_DOWN") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      val device = coord.addDevice("dev1", autoAssigned = false)
      val FEP_NAME = "fep"
      val fep = coord.addFep(FEP_NAME)
      val meas = coord.addMeasProc("meas99")

      coord.checkAssignments(1, None, Some(meas))

      val connection = coord.claimEndpoint(device.getName, Some(FEP_NAME))

      connection.getFrontEnd.getAppConfig.getInstanceName should equal(FEP_NAME)

      coord.checkAssignments(1, Some(fep), Some(meas))

      coord.updatePoint("dev1.test_point")
      coord.checkPoints(1, 0)

      coord.timeoutApplication(fep.getAppConfig)

      coord.checkAssignments(1, None, Some(meas))

      // check that the point is now marked as bad in the rtdb
      coord.checkPoints(1, 1)
    }
  }

  test("Switching to autoAssigned") {
    ConnectionFixture.mock() { amqp =>
      val coord = new CoordinatorFixture(amqp)

      coord.addDevice("dev1", autoAssigned = false)
      val fep = coord.addFep("fep")
      val meas = coord.addMeasProc("meas99")

      coord.checkAssignments(1, None, Some(meas))

      coord.addDevice("dev1", autoAssigned = true)

      coord.checkAssignments(1, Some(fep), Some(meas))

      coord.addDevice("dev1", autoAssigned = false)

      coord.checkAssignments(1, None, Some(meas))
    }
  }

}