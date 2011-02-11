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

//import org.totalgrid.reef.loader.configuration
//import org.totalgrid.reef.loader.sx

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.protoapi.scala.client.MockSyncOperations
import org.totalgrid.reef.protoapi.ServiceTypes._

@RunWith(classOf[JUnitRunner])
class MessageLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  case class Fixture(client: MockSyncOperations, config: sx.Configuration)
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    val client = new MockSyncOperations((GeneratedMessage) => MultiSuccess(List[GeneratedMessage]()))
    val config = new sx.Configuration("1.0")

    test(Fixture(client, config))
  }

  def testMessageModelMessage(fixture: Fixture) {
    import org.totalgrid.reef.loader.LoadManager
    import fixture._

    val mModel = new sx.MessageModel
    config.setMessageModel(mModel)

    mModel.add(new sx.Message("Scada.UserLogin", "EVENT", 1, "User login {status} {reason}"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    var protos = List[GeneratedMessage](toEvent("Scada.UserLogin", 1, "User login {status} {reason}"))
    client.getPutQueue should equal(protos)

    mModel.reset
    client.reset
    mModel.add(new sx.Message("Scada.OutOfNominal", "LOG", 8, "User login {status} {reason}"))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    protos = List[GeneratedMessage](toLog("Scada.OutOfNominal", 8, "User login {status} {reason}"))
    client.getPutQueue should equal(protos)

    mModel.reset
    client.reset
    mModel.add(new sx.Message("Scada.OutOfNominal", "ALARM", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE.toString))
    LoadManager.loadConfiguration(client, config, true) // T: benchmark
    protos = List[GeneratedMessage](toAlarm("Scada.OutOfNominal", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE))
    client.getPutQueue should equal(protos)

  }

  def testMessageModelMessageSet(fixture: Fixture) {
    import org.totalgrid.reef.loader.LoadManager
    import fixture._

    val mModel = new sx.MessageModel
    config.setMessageModel(mModel)

    // Get a proto stream using MessageSet defaults
    //
    var messageSet = new sx.MessageSet("Scada", "EVENT", 1, Alarm.State.UNACK_AUDIBLE.toString)
    mModel.add(messageSet)
    messageSet.add(new sx.Message("one", "", 0, "User login {status} {reason}"))
    messageSet.add(new sx.Message("two", "ALARM", 0, "User login {status} {reason}"))
    messageSet.add(new sx.Message("three", "", 2, "User login {status} {reason}"))
    messageSet.add(new sx.Message("four", "", 0, "Something else"))
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
    mModel.reset
    messageSet = new sx.MessageSet("Scada")
    messageSet.add(messageSet)
    messageSet.add(new sx.Message("one", "EVENT", 1, "User login {status} {reason}"))
    messageSet.add(new sx.Message("two", "ALARM", 1, "User login {status} {reason}", Alarm.State.UNACK_AUDIBLE.toString))
    messageSet.add(new sx.Message("three", "EVENT", 2, "User login {status} {reason}"))
    messageSet.add(new sx.Message("four", "EVENT", 1, "Something else"))
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

