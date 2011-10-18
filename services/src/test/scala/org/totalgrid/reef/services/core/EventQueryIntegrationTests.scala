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
import org.totalgrid.reef.messaging.mock.AMQPFixture

import org.totalgrid.reef.api.proto.Model.{ Entity => EntityProto }
import org.totalgrid.reef.api.proto.Events.{ EventList => EventListProto, EventSelect }
import org.totalgrid.reef.api.proto.Alarms.{ EventConfig => EventConfigProto }
import org.totalgrid.reef.japi.BadRequestException

@RunWith(classOf[JUnitRunner])
class EventQueryIntegrationTests extends EventIntegrationTestsBase {

  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  test("Subscribe via EventQuery") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)
      fix.eventConfigs.put(makeEC("Test.Alarm", 6, EventConfigProto.Designation.ALARM))

      val (_, expected1) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("*")))
      val (_, expected2) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("Test.Alarm")))
      val (_, not1) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("Test.Event")))
      val (_, expected3) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addSeverity(6)))
      val (_, not2) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addSeverity(5)))
      val (_, expected4) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEntity(EntityProto.newBuilder.setName("SubA-DeviceA-PointA"))))
      val (_, not3) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEntity(EntityProto.newBuilder.setName("SubA-DeviceA-PointB"))))

      val (_, expected5) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("Test.Alarm").addEventType("Test.Event")))
      val (_, not4) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("Test.Alarm").addEventType("Test.Event").addSeverity(2)))
      val (_, not5) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.setSeverityOrHigher(5)))
      val (_, expected6) = fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.setSeverityOrHigher(6)))

      val expected = expected1 :: expected2 :: expected3 :: expected4 :: expected5 :: expected6 :: Nil
      val not = not1 :: not2 :: not3 :: not4 :: not5 :: Nil

      val event = fix.publishEvent(makeEvent("Test.Alarm", entityName = Some("SubA-DeviceA-PointA")))

      expected.foreach(_.pop(5000) should equal(event))
      not.foreach(_.size should equal(0))
    }
  }

  test("Illegal Subscriptions via EventQuery") {
    AMQPFixture.mock(true) { amqp =>
      val fix = new AlarmTestFixture(amqp)

      intercept[BadRequestException] {
        fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.addEventType("Test.Alarm").addEventType("Test.Event").addSeverity(2).addSeverity(1)))
      }

      intercept[BadRequestException] {
        fix.subscribeEvents(0, makeEventList(EventSelect.newBuilder.setTimeTo(0)))
      }
    }
  }

  def makeEventList(es: EventSelect.Builder) = {
    EventListProto.newBuilder.setSelect(es).build
  }
}