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
import org.totalgrid.reef.loader.sx.equipment._ // scala XML classes

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.api.scalaclient.{ MockSyncOperations, Success }
import org.totalgrid.reef.api.Envelope

class NullExceptionCollector extends ExceptionCollector {
  def collect[A](name: => String)(f: => Unit) { f }
}

@RunWith(classOf[JUnitRunner])
class EquipmentLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  case class Fixture(client: MockSyncOperations, loader: EquipmentLoader, model: EquipmentModel)
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    // For now, pass in a get function that always returns an empty list.
    val client = new MockSyncOperations((GeneratedMessage) => Success(Envelope.Status.OK, List[GeneratedMessage]()))
    val modelLoader = new CachingModelLoader(Some(client))
    val model = new EquipmentModel
    val ex = new NullExceptionCollector
    val loader = new EquipmentLoader(modelLoader, new LoadCache().loadCacheEqu, ex)

    test(Fixture(client, loader, model))
  }

  def testEquipmentModelProfiles(fixture: Fixture) = {
    import fixture._

    val actionModel = HashMap[String, configuration.ActionSet]()
    val breakerProfile = makeEquipmentProfile("FullBreaker")

    val profiles = new Profiles
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

    val actionModel = HashMap[String, configuration.ActionSet]()
    actionModel += ("Nominal" -> makeActionSetNominal())

    val normallyFalse = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
    val normallyTrue = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
    val breakerProfile = makeEquipmentProfile("FullBreaker", Some(normallyTrue))

    /*
    val pointEntity = toEntityType(name, List("Point"))
    val point = toPoint(name, pointEntity)
    val triggerSet = toTriggerSet(point)
    */

    val profiles = new Profiles
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
    equipmentProfile: Option[EquipmentProfile] = None): Equipment = {

    val breaker = new Equipment("Brk1")
    equipmentProfile match {
      case Some(profile) =>
        breaker.add(profile)
      case None =>
        val status = new Status("Bkr", "status")

        if (isTrigger) {
          status.add(new Unexpected(false, "Nominal"))
          status.add(new Transform("raw", "status", new ValueMap("false", "CLOSED"), new ValueMap("true", "OPEN")))
        }
        breaker
          .add(new Type("Breaker"))
          .add(new Type("Equipment"))
          .add(new Control("trip"))
          .add(new Control("close"))
          .add(status)
    }

    new Equipment(substationName)
      .add(new Type("Substation"))
      .add(new Type("EquipmentGroup"))
      .add(breaker)
  }

  def makeEquipmentProfile(profileName: String, pointProfile: Option[PointProfile] = None): EquipmentProfile = {

    new EquipmentProfile(profileName)
      .add(new Type("Breaker"))
      .add(new Type("Equipment"))
      .add(new Control("trip"))
      .add(new Control("close"))
      .add(new Status("Bkr", "status", pointProfile))
  }

  def makePointProfile(profileName: String, value: Boolean, actionSet: String): PointProfile = {
    new PointProfile(profileName)
      .add(new Unexpected(value, actionSet))
      .add(new Transform("raw", "status", new ValueMap("false", "CLOSED"), new ValueMap("true", "OPEN")))
  }

  def makeActionSetNominal(): sx.ActionSet = {
    val as = new sx.ActionSet("Nominal")
    val rising = new configuration.Rising()
    rising.getMessage.add(new sx.Message("Scada.OutOfNominal"))
    val high = new configuration.High
    high.setSetAbnormal(new Object)
    as.setRising(rising)
    as.setHigh(high)
    as
  }

}

