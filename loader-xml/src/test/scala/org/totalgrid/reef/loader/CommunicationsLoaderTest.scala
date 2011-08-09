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
package org.totalgrid.reef.loader

import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.fixture.FixtureSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.loader.sx.communications._

import collection.mutable.HashMap
import org.totalgrid.reef.util.BuildEnv
import org.totalgrid.reef.loader.helpers.CachingModelLoader

// scala XML classes

import org.totalgrid.reef.sapi.client.{ MockSyncOperations, Success }
import org.totalgrid.reef.japi.Envelope

@RunWith(classOf[JUnitRunner])
class CommunicationsLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers {

  val samplesPath = "assemblies/assembly-common/filtered-resources/samples/"

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
    val client = new MockSyncOperations((AnyRef) => Success(Envelope.Status.OK, List[AnyRef]()))
    val modelLoader = new CachingModelLoader(Some(client))
    val model = new CommunicationsModel
    val ex = new NullExceptionCollector
    val commonLoader = new CommonLoader(modelLoader, ex, new java.io.File(BuildEnv.configPath + samplesPath + "two_substations"))
    val loader = new CommunicationsLoader(modelLoader, new LoadCache().loadCacheCom, ex, commonLoader)

    test(Fixture(client, loader, model))
  }

  def testInterfaces(fixture: Fixture) = {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()

    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
        .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    protos.length should equal(3)

    reset
    model.add(
      new Interface("i1", "192.168.100.30", 8003))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
        .set(new Interface("i1")))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)

    reset
    model.add(
      new Interface("i1", "192.168.100.30", 0))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
        .set(new Interface("i1", 8003)))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)

    reset
    model.add(
      new Interface("i1", "192.168.100.0", 0))
    model.add(
      new Endpoint("myEndpoint", Some("dnp3"), Some("comms/sel_351_endpoint1.xml"))
        .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    protos should equal(client.getPutQueue)
  }

  def testCommunicationsPointProfiles(fixture: Fixture) = {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    // No profiles
    //
    model.add(
      new Endpoint("Endpoint1")
        .set(new Interface("i1", "192.168.100.30", 8003))
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Control("trip", Some(1))
              .set(new OptionsDnp3("PULSE_CLOSE", 1000, 1000, 1)))
            .add(new Analog("Mw", Some(3))
              .set(new Scale(-50.0, 100.0, 100.0, 200.0, "Mw"))))))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
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
            .add(new Control("trip", Some(1), controlProfile))
            .add(new Analog("Mw", Some(3), linePowerProfile)))))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    //val protos2 = client.getPutQueue
    //println( "\nWith profiles protos.length = "+protos2.length); protos2.foreach( protoPrintln); println(" \n ");
    protos should equal(client.getPutQueue)
  }

  def testCommunicationsEndpointProfiles(fixture: Fixture) = {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    // No profiles
    //
    model.add(
      new Endpoint("Endpoint1")
        .set(new Interface("i1", "192.168.100.30", 8003))
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Control("trip", Some(1))
              .set(new OptionsDnp3("PULSE_CLOSE", 1000, 1000, 1)))
            .add(new Analog("Mw", Some(3))
              .set(new Scale(-50.0, 100.0, 100.0, 200.0, "Mw"))))))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    println(protos)
    //println( "no profiles protos.length = "+protos.length); protos.foreach( protoPrintln); println(" \n ");
    protos.length should equal(3)

    //  With EndpointProfile, PointProfile, and ControlProfile
    //
    reset
    val linePowerProfile = new PointProfile("LinePower").set(new Scale(-50.0, 100.0, 100.0, 200.0, "Mw"))
    val controlProfile = new ControlProfile("Breaker").set(new OptionsDnp3("PULSE_CLOSE", 1000, 1000, 1))
    model.set(
      new Profiles()
        .add(linePowerProfile)
        .add(controlProfile)
        .add(new EndpointProfile("EndpointProfile1")
          .set(new Interface("i1", "192.168.100.30", 8003))
          .add(new Equipment("ChapelHill")
            .add(new Equipment("BigBkr")
              .add(new Control("trip", Some(1), controlProfile))
              .add(new Analog("Mw", Some(3), linePowerProfile))))))
    model.add(
      new Endpoint("Endpoint1")
        .add(new EndpointProfile("EndpointProfile1")))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    //val protos2 = client.getPutQueue
    //println( "\nWith profiles protos.length = "+protos2.length); protos2.foreach( protoPrintln); println(" \n ");
    protos should equal(client.getPutQueue)

    //  With control in EndpointProfile, analog not.
    //
    reset
    model.set(
      new Profiles()
        .add(linePowerProfile)
        .add(controlProfile)
        .add(new EndpointProfile("EndpointProfile1")
          .set(new Interface("i1", "192.168.100.30", 8003))
          .add(new Equipment("ChapelHill")
            .add(new Equipment("BigBkr")
              .add(new Control("trip", Some(1), controlProfile))))))
    model.add(
      new Endpoint("Endpoint1")
        .add(new EndpointProfile("EndpointProfile1"))
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Analog("Mw", Some(3), linePowerProfile)))))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    //val protos2 = client.getPutQueue
    //println( "\nWith profiles protos.length = "+protos2.length); protos2.foreach( protoPrintln); println(" \n ");
    protos should equal(client.getPutQueue)
  }

  /**
   * Indexes are not required for benchmark.
   */
  def testCommunicationsBenchmarkWithoutIndexes(fixture: Fixture) = {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    model.add(
      new Endpoint("Simulated") // default protocol is benchmark
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Analog("Mw")))))
    loader.load(model, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    protos.length should equal(2)

  }

  /**
   * Indexes are not required for benchmark.
   */
  def testCommunicationsDnp3WithoutIndexes(fixture: Fixture) = {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    model.add(
      new Endpoint("Simulated", Some(CommunicationsLoader.DNP3))
        .set(new Interface("interface1", "127.0.0.1", 8001))
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Analog("Mw")))))

    intercept[Exception] {
      loader.load(model, equipmentPointUnits, false) // T: benchmark
    }

    loader.load(model, equipmentPointUnits, true) // T: benchmark
    val protos = client.getPutQueue.clone
    protos.length should equal(3)

  }

  def protoPrintln(value: AnyRef) = println("\nPROTO: " + className(value.getClass.toString) + "\n" + value.toString + "\n")
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
        val status = new Status("Bkr", Some(1), "status")
        //if (isTrigger)
        //  status.add(new Unexpected(false, "Nominal"))
        breaker
          .add(new Control("trip", Some(2)))
          .add(new Control("close", Some(3)))
          .add(status)
    }

    new Equipment(substationName)
      .add(breaker)
  }

}

