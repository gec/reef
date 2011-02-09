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
package org.totalgrid.reef.loader

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.fixture.FixtureSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import java.util.{ Date, Calendar }

import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.equipment.{ EquipmentModel => EQ }
import org.totalgrid.reef.loader.communications.{ CommunicationsModel => CM }

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.protoapi.client.MockSyncOperations
import org.totalgrid.reef.protoapi.ProtoServiceTypes._

@RunWith(classOf[JUnitRunner])
class MessageLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  case class Fixture(client: MockSyncOperations, config: Configuration)
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    val client = new MockSyncOperations((GeneratedMessage) => MultiSuccess(List[GeneratedMessage]()))
    val config = new Configuration
    config.setVersion("1.0")

    test(Fixture(client, config))
  }

  def testMessageModelMessage(fixture: Fixture) {
    import org.totalgrid.reef.loader.LoadManager
    import fixture._

    val mModel = new MessageModel
    config.setMessageModel(mModel)

    val messages = mModel.getMessage
    messages.add(new XmlMessage("Scada.UserLogin", "EVENT", 1, "User login {status} {reason}"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    var protos = List[GeneratedMessage](toEvent("Scada.UserLogin", 1, "User login {status} {reason}"))
    client.getPutQueue should equal(protos)

    messages.clear
    client.reset
    messages.add(new XmlMessage("Scada.OutOfNominal", "LOG", 8, "User login {status} {reason}"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    protos = List[GeneratedMessage](toLog("Scada.OutOfNominal", 8, "User login {status} {reason}"))
    client.getPutQueue should equal(protos)

    messages.clear
    client.reset
    messages.add(new XmlMessage("Scada.OutOfNominal", "ALARM", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE.toString))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    protos = List[GeneratedMessage](toAlarm("Scada.OutOfNominal", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE))
    client.getPutQueue should equal(protos)

  }

  def testMessageModelMessageSet(fixture: Fixture) {
    import org.totalgrid.reef.loader.LoadManager
    import fixture._

    val mModel = new MessageModel
    config.setMessageModel(mModel)

    // Get a proto stream using MessageSet defaults
    //
    val messageSets = mModel.getMessageSet
    var messageSet = new XmlMessageSet("Scada", "EVENT", 1, Alarm.State.UNACK_AUDIBLE.toString)
    messageSets.add(messageSet)
    val messages = messageSet.getMessage
    messages.add(new XmlMessage("one", "", 0, "User login {status} {reason}"))
    messages.add(new XmlMessage("two", "ALARM", 0, "User login {status} {reason}"))
    messages.add(new XmlMessage("three", "", 2, "User login {status} {reason}"))
    messages.add(new XmlMessage("four", "", 0, "Something else"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    val puts1 = client.getPutQueue.clone
    val ec0 = puts1.head.asInstanceOf[EventConfig]
    ec0.getEventType should equal("Scada.one")
    ec0.getDesignation should equal(EventConfig.Designation.EVENT)
    ec0.getSeverity should equal(1)
    ec0.getResource should equal("User login {status} {reason}")
    ec0.hasAlarmState should equal(false)
    val ec1 = puts1(1).asInstanceOf[EventConfig]
    ec1.hasAlarmState should equal(true)
    ec1.getAlarmState should equal(Alarm.State.UNACK_AUDIBLE)

    // Get a proto stream using NO MessageSet defaults
    //
    messageSets.clear
    messages.clear
    messageSet = new XmlMessageSet("Scada")
    messageSets.add(messageSet)
    messages.add(new XmlMessage("one", "EVENT", 1, "User login {status} {reason}"))
    messages.add(new XmlMessage("two", "ALARM", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE.toString))
    messages.add(new XmlMessage("three", "EVENT", 2, "User login {status} {reason}"))
    messages.add(new XmlMessage("four", "EVENT", 1, "Something else"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    val puts2 = client.getPutQueue

    // Both streams should match
    puts1 should equal(puts2)
  }

  def toEvent(name: String, severity: Int, resource: String) = {
    EventConfig.newBuilder
      .setEventType(name)
      .setSeverity(severity)
      .setDesignation(EventConfig.Designation.EVENT)
      .setResource(resource)
      .build
  }
  def toLog(name: String, severity: Int, resource: String) = {
    EventConfig.newBuilder
      .setEventType(name)
      .setSeverity(severity)
      .setDesignation(EventConfig.Designation.LOG)
      .setResource(resource)
      .build
  }
  def toAlarm(name: String, severity: Int, resource: String, state: Alarm.State) = {
    EventConfig.newBuilder
      .setEventType(name)
      .setSeverity(severity)
      .setDesignation(EventConfig.Designation.ALARM)
      .setAlarmState(state)
      .setResource(resource)
      .build
  }

  /*
  def xmlMessage( name: String, typ: String, severity: Int, text: String, state: Option[Alarm.State]=None) = {
    val message = new Message
    message.setName( name)
    message.setType(typ)
    message.setSeverity(severity)
    message.setValue(text)
    if( state.isDefined)
      message.setState(state.get.toString)
    message
  }
  */

}

