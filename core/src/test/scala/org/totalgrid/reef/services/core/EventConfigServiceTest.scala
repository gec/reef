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

import org.totalgrid.reef.proto.Alarms._

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.messaging.mock.AMQPFixture

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import org.totalgrid.reef.models._

import org.totalgrid.reef.event._
import org.totalgrid.reef.services.SilentEventPublishers

@RunWith(classOf[JUnitRunner])
class EventConfigServiceTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll with BeforeAndAfterEach // with RunTestsInsideTransaction
{
  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }
  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  test("Get and Put") {
    import EventType._
    import EventConfig.Designation

    AMQPFixture.mock(true) { amqp =>
      val fac = new EventConfigServiceModelFactory(new SilentEventPublishers)
      val service = new EventConfigService(fac)
      amqp.bindService("test", service.respond) // listen for service requests with the echo service
      val client = amqp.getProtoServiceClient("test", 500000, EventConfig.parseFrom)

      val sent = makeEC(Scada.ControlExe, 1, Designation.ALARM)
      val created = client.putOne(sent)
      val gotten = client.getOne(makeEC(Scada.ControlExe))

      gotten should equal(created)
    }
  }

  ////////////////////////////////////////////////////////
  // Utilities

  def makeEC(event: EventType, severity: Int, designation: EventConfig.Designation) =
    EventConfig.newBuilder
      .setEventType(event)
      .setSeverity(severity)
      .setDesignation(designation)
      .build

  def makeEC(event: EventType) =
    EventConfig.newBuilder
      .setEventType(event)
      .build

}
