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

import org.totalgrid.reef.client.service.proto.Alarms._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.models._

import org.totalgrid.reef.client.service.proto.Alarms.EventConfig.Designation
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.sapi.client.Expectations._

@RunWith(classOf[JUnitRunner])
class EventConfigServiceTest extends DatabaseUsingTestBase with SyncServicesTestHelpers {

  val service = sync(new EventConfigService(new EventConfigServiceModel))

  test("Get and Put") {

    val sent = makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.EVENT), Some("Resource"))
    val created = service.put(sent).expectOne
    val gotten = service.get(makeEc(Some("Scada.ControlExe"))).expectOne

    gotten should equal(created)
  }

  test("Incomplete event configs") {

    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), None, Some(Designation.EVENT), Some("Resource"))).expectOne
    }
    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), None, Some("Resource"))).expectOne
    }
    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.EVENT), None)).expectOne
    }
    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.EVENT), Some("Resource"), builtIn = Some(true))).expectOne
    }
  }

  test("Incomplete alarm event configs") {

    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.ALARM), Some("Resource"))).expectOne
    }
    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.ALARM), Some("Resource"), Some(Alarm.State.ACKNOWLEDGED))).expectOne
    }
    intercept[BadRequestException] {
      service.put(makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.ALARM), Some("Resource"), Some(Alarm.State.REMOVED))).expectOne
    }
  }

  test("Create large string") {

    // resources is by default 128 chars max
    val longString = "hello!" * 50

    val sent = makeEc(Some("Scada.ControlExe"), Some(1), Some(Designation.ALARM), Some(longString), Some(Alarm.State.UNACK_AUDIBLE))
    val created = service.put(sent).expectOne

    created.getResource should equal(longString)
  }

  test("Delete custom event") {
    val sent = makeEc(Some("Custom.Event"), Some(1), Some(Designation.EVENT), Some("ss"))
    val created = service.put(sent).expectOne
    created.getBuiltIn should equal(false)

    val gotten = service.get(makeEc(builtIn = Some(false))).expectOne
    gotten should equal(created)

    // can delete custom event
    service.delete(created)
  }

  test("Can't delete builtIn event") {

    EventConfigService.seed()

    val gotten = service.get(makeEc(builtIn = Some(true))).expectMany().head

    val defaultText = gotten.getResource

    val altered = service.put(gotten.toBuilder.setResource("Test Value").build).expectOne

    altered.getResource should not equal (defaultText)

    service.delete(gotten).expectOne(Envelope.Status.DELETED)

    service.get(gotten).expectOne.getResource should equal(defaultText)
  }

  ////////////////////////////////////////////////////////
  // Utilities

  private def makeEc(event: Option[String] = None,
    severity: Option[Int] = None,
    designation: Option[EventConfig.Designation] = None,
    resource: Option[String] = None,
    alarmState: Option[Alarm.State] = None,
    builtIn: Option[Boolean] = None) = {
    val b = EventConfig.newBuilder
    event.foreach(b.setEventType(_))
    severity.foreach(b.setSeverity(_))
    designation.foreach(b.setDesignation(_))
    resource.foreach(b.setResource(_))
    alarmState.foreach(b.setAlarmState(_))
    builtIn.foreach(b.setBuiltIn(_))
    b.build
  }

}
