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

import scala.collection.mutable.HashMap
import org.totalgrid.reef.loader.configuration._
import org.totalgrid.reef.loader.equipment._

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.protoapi.client.MockSyncOperations
import org.totalgrid.reef.protoapi.ProtoServiceTypes._

@RunWith(classOf[JUnitRunner])
class EquipmentLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  case class Fixture(client: MockSyncOperations, loader: EquipmentLoader, model: XmlEquipmentModel)
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    // For now, pass in a get function that always returns an empty list.
    val client = new MockSyncOperations((GeneratedMessage) => MultiSuccess(List[GeneratedMessage]()))
    val model = new XmlEquipmentModel
    val loader = new EquipmentLoader(client)

    test(Fixture(client, loader, model))
  }

  def testEquipmentModelProfiles(fixture: Fixture) = {
    import fixture._

    val actionModel = HashMap[String, ActionSet]()
    val breakerProfile = makeEquipmentProfile("FullBreaker")

    val profiles = new XmlProfiles
    model.setProfiles(profiles)

    profiles.add(breakerProfile)
    model.add(makeEquipment("ChapelHill", false, Some(breakerProfile))) // F: no triggers
    loader.load(model, actionModel)
    val protos = client.getPutQueue.clone
    protos.length should equal(12)

    client.reset // reset putQueue, etc.
    loader.reset // Clear profiles, etc.
    model.reset // Clear profiles and equipment

    model.add(makeEquipment("ChapelHill", false)) // No trigger, No profile
    loader.load(model, actionModel)

    protos should equal(client.getPutQueue)
  }

  def testEquipmentModelProfilesWithTriggers(fixture: Fixture) = {
    import fixture._

    val actionModel = HashMap[String, ActionSet]()
    actionModel += ("Nominal" -> makeActionSetNominal())

    val normallyFalse = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
    val normallyTrue = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
    val breakerProfile = makeEquipmentProfile("FullBreaker", Some(normallyTrue))

    /*
    val pointEntity = toEntityType(name, List("Point"))
    val point = toPoint(name, pointEntity)
    val triggerSet = toTriggerSet(point)
    */

    val profiles = new XmlProfiles
    model.setProfiles(profiles)

    profiles.add(breakerProfile)
    profiles.add(normallyFalse)
    profiles.add(normallyTrue)

    model.add(makeEquipment("ChapelHill", true, Some(breakerProfile)))

    loader.load(model, actionModel)
    val protos = client.getPutQueue.clone
    protos.length should equal(13)

    client.reset // reset putQueue, etc.
    loader.reset // Clear profiles, etc.
    model.reset // Clear profiles and equipment

    model.add(makeEquipment("ChapelHill", true)) // No profile
    loader.load(model, actionModel)

    //println( "protos2.length = " + client.getPutQueue.length)
    protos should equal(client.getPutQueue)
  }

  def makeEquipment(
    substationName: String,
    isTrigger: Boolean,
    equipmentProfile: Option[XmlEquipmentProfile] = None): XmlEquipment = {

    val breaker = new XmlEquipment("Brk1")
    equipmentProfile match {
      case Some(profile) =>
        breaker.add(profile)
      case None =>
        val status = new XmlStatus("Bkr", "status")
        if (isTrigger)
          status.add(new XmlUnexpected(false, "Nominal"))
        breaker
          .add(new XmlType("Breaker"))
          .add(new XmlType("Equipment"))
          .add(new XmlControl("trip"))
          .add(new XmlControl("close"))
          .add(status)
    }

    new XmlEquipment(substationName)
      .add(new XmlType("Substation"))
      .add(new XmlType("EquipmentGroup"))
      .add(breaker)
  }

  def makeEquipmentProfile(profileName: String, pointProfile: Option[XmlPointProfile] = None): XmlEquipmentProfile = {

    new XmlEquipmentProfile(profileName)
      .add(new XmlType("Breaker"))
      .add(new XmlType("Equipment"))
      .add(new XmlControl("trip"))
      .add(new XmlControl("close"))
      .add(new XmlStatus("Bkr", "status", pointProfile))
  }

  def makePointProfile(profileName: String, value: Boolean, actionSet: String): XmlPointProfile = {
    new XmlPointProfile(profileName)
      .add(new XmlUnexpected(value, actionSet))
  }

  def makeActionSetNominal(): XmlActionSet = {
    val as = new XmlActionSet("Nominal")
    val rising = new configuration.Rising()
    rising.getMessage.add(new XmlMessage("Scada.OutOfNominal"))
    val high = new configuration.High
    high.setSetAbnormal(new Object)
    as.setRising(rising)
    as.setHigh(high)
    as
  }

}

