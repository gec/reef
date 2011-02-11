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

import org.totalgrid.reef.loader.sx.communications._
import java.io.File
import collection.mutable.{ Queue, HashMap }

// scala XML classes

import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.protoapi.scala.client.MockSyncOperations
import org.totalgrid.reef.protoapi.ServiceTypes._

@RunWith(classOf[JUnitRunner])
class CommunicationsLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {
  import org.totalgrid.reef.services.ServiceResponseTestingHelpers._

  case class Fixture(client: MockSyncOperations, loader: CommunicationsLoader, model: CommunicationsModel) {
    def reset = {
      client.reset // reset putQueue, etc.
      loader.reset // Clear profiles, etc.
      model.reset // Clear profiles and equipment
    }
  }
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    // For now, pass in a get function that always returns an empty list.
    val client = new MockSyncOperations((GeneratedMessage) => MultiSuccess(List[GeneratedMessage]()))
    val model = new CommunicationsModel
    val loader = new CommunicationsLoader(client)

    test(Fixture(client, loader, model))
  }

  def testInterfaces(fixture: Fixture) = {
    import fixture._

    println("default path = " + new java.io.File(".").getAbsolutePath)
    val path = new java.io.File("../karaf-common/samples/two_substations")
    val equipmentPointUnits = HashMap[String, String]()

    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
      .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    protos.length should equal(3)

    reset
    model.add(
      new Interface("i1", "192.168.100.30", 8003))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
      .set(new Interface("i1")))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)

    reset
    model.add(
      new Interface("i1", "192.168.100.30", 0))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
      .set(new Interface("i1", 8003)))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)

    reset
    model.add(
      new Interface("i1", "192.168.100.0", 0))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
      .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)
  }

  def testCommunicationsPointProfiles(fixture: Fixture) = {
    import fixture._

    val path = new java.io.File("../karaf-common/samples/two_substations")
    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    // No profiles
    //
    model.add(
      new Endpoint("Endpoint1")
      .set(new Interface("i1", "192.168.100.30", 8003))
      .add(new Equipment("ChapelHill")
        .add(new Equipment("BigBkr")
          .add(new Control("trip", 1)
            .set(new OptionsDnp3("PULSE_CLOSE", 1000, 1000, 1)))
          .add(new Analog("Mw", 3)
            .set(new Scale(-50.0, 100.0, 100.0, 200.0, "Mw"))))))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    //println( "no profiles protos.length = "+protos.length); protos.foreach( protoPrintln); println(" \n ");
    protos.length should equal(3)

    //  With PointProfile and ControlProfile
    //
    reset
    val linePowerProfile = new PointProfile("LinePower").set(new Scale(-50.0, 100.0, 100.0, 200.0, "Mw"))
    val controlProfile = new ControlProfile("Breaker").set(new OptionsDnp3("PULSE_CLOSE", 1000, 1000, 1))
    model.set(
      new Profiles()
      .add(linePowerProfile)
      .add(controlProfile))
    model.add(
      new Endpoint("Endpoint1")
      .set(new Interface("i1", "192.168.100.30", 8003))
      .add(new Equipment("ChapelHill")
        .add(new Equipment("BigBkr")
          .add(new Control("trip", 1, controlProfile))
          .add(new Analog("Mw", 3, linePowerProfile)))))
    loader.load(model, path, equipmentPointUnits, true) // T: benchmark
    //val protos2 = client.getPutQueue
    //println( "\nWith profiles protos.length = "+protos2.length); protos2.foreach( protoPrintln); println(" \n ");
    protos should equal(client.getPutQueue)
  }

  def protoPrintln(proto: com.google.protobuf.GeneratedMessage) = println("\nPROTO: " + className(proto.getClass.toString) + "\n" + proto.toString + "\n")
  def className(c: String) = c.substring(c.lastIndexOf('.') + 1, c.length - 1).replace('$', '.')

  def makeEquipment(
    substationName: String,
    isTrigger: Boolean,
    equipmentProfile: Option[EquipmentProfile] = None): Equipment = {

    val breaker = new Equipment("Brk1")
    equipmentProfile match {
      case Some(profile) =>
        breaker.add(profile)
      case None =>
        val status = new Status("Bkr", 1, "status")
        //if (isTrigger)
        //  status.add(new Unexpected(false, "Nominal"))
        breaker
          .add(new Control("trip", 2))
          .add(new Control("close", 3))
          .add(status)
    }

    new Equipment(substationName)
      .add(breaker)
  }

}

