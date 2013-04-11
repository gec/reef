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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.fixture.FixtureSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.loader.sx.communications._

import com.typesafe.scalalogging.slf4j.Logging
import org.scalatest.{ Assertions, BeforeAndAfterAll }
import collection.mutable.{ HashMap }

import collection.Seq
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint => EndpointProto, CommChannel }
import org.totalgrid.reef.client.service.proto.Model.ConfigFile
import org.totalgrid.reef.loader.helpers.CachingModelLoader

@RunWith(classOf[JUnitRunner])
class CommunicationsLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers with Assertions with Logging {

  val samplesPath = "assemblies/assembly-common/filtered-resources/samples/"

  case class Fixture(modelLoader: CachingModelLoader, exceptionCollector: ExceptionCollector, loader: CommunicationsLoader,
      model: CommunicationsModel) {
    def reset {
      loader.reset // Clear profiles, etc.
      modelLoader.reset()
      exceptionCollector.reset()
      model.reset // Clear profiles and equipment
    }
  }
  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) = {

    // For now, pass in a get function that always returns an empty list.
    val modelLoader = new CachingModelLoader(None)
    val model = new CommunicationsModel
    val exceptionCollector = new LoadingExceptionCollector
    val commonLoader = new CommonLoader(modelLoader, exceptionCollector, new java.io.File("../" + samplesPath + "two_substations"))
    val loader = new CommunicationsLoader(modelLoader, new LoadCache().loadCacheCommunication, exceptionCollector, commonLoader)

    test(Fixture(modelLoader, exceptionCollector, loader, model))
  }

  def testInterfaces(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()

    val endpointXmlFileName: String = "comms/sel_351_endpoint1.xml"
    val endpointName: String = "myEndpoint"
    model.add(
      new Endpoint(endpointName, Some("dnp3"), Some(endpointXmlFileName))
        .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, equipmentPointUnits, true)
    verifyNoLoadExceptions(loader)
    val protos = modelLoader.allProtos
    protos.length should equal(4)

    reset

    model.add(new Interface("i1", "192.168.100.30", 8003))
    model.add(
      new Endpoint(endpointName, Some("dnp3"), Some(endpointXmlFileName))
        .set(new Interface("i1")))
    loader.load(model, equipmentPointUnits, true)

    protos should equal(modelLoader.allProtos)

    reset

    model.add(new Interface("i1", "192.168.100.30", 0))
    model.add(
      new Endpoint(endpointName, Some("dnp3"), Some(endpointXmlFileName))
        .set(new Interface("i1", 8003)))
    loader.load(model, equipmentPointUnits, true)
    protos should equal(modelLoader.allProtos)

    reset

    model.add(new Interface("i1", "192.168.100.0", 0))
    model.add(
      new Endpoint(endpointName, Some("dnp3"), Some(endpointXmlFileName))
        .set(new Interface("i1", "192.168.100.30", 8003)))
    loader.load(model, equipmentPointUnits, true)
    protos should equal(modelLoader.allProtos)

    verifyNoLoadExceptions(loader)
  }

  def testCommunicationsPointProfiles(fixture: Fixture) {
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
    loader.load(model, equipmentPointUnits, true)
    val protos = modelLoader.allProtos
    protos.length should equal(4)

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

    protos should equal(modelLoader.allProtos)

    verifyNoLoadExceptions(loader)
  }

  def testCommunicationsEndpointProfiles(fixture: Fixture) {
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

    val protos = modelLoader.allProtos
    protos.length should equal(4)

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
    protos should equal(modelLoader.allProtos)

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
    protos should equal(modelLoader.allProtos)

    verifyNoLoadExceptions(loader)
  }

  // Indexes are not required for benchmark.
  def testCommunicationsBenchmarkWithoutIndexes(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    // default protocol is benchmark
    model.add(
      new Endpoint("Simulated")
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Analog("Mw")))))
    loader.load(model, equipmentPointUnits, true)
    verifyNoLoadExceptions(loader)

    modelLoader.size should equal(2)
  }

  def testCommunicationsDnp3WithoutIndexesFails(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    val endpoint: Endpoint = createSimpleEndpoint(false)
    model.add(endpoint)

    loader.load(model, equipmentPointUnits)

    logCollectedExceptions(loader)

    expect(2)(loader.getExceptionCollector.getErrors.length)
    val matchedError: List[String] = loader.getExceptionCollector.getErrors.filter(error =>
      {
        error.contains("does not specify an index") || error.contains("has no index specified")
      })
    expect(2)(matchedError.length)
  }

  def testCommunicationsDnp3BadControlCode(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()

    // No profiles
    //
    model.add(
      new Endpoint("Endpoint1", Some("dnp3"))
        .set(new Interface("i1", "192.168.100.30", 8003))
        .add(new Equipment("ChapelHill")
          .add(new Equipment("BigBkr")
            .add(new Control("trip", Some(1))
              .set(new OptionsDnp3("PULSE_OPEN", 1000, 1000, 1))))))
    loader.load(model, equipmentPointUnits)

    logCollectedExceptions(loader)

    expect(1)(loader.getExceptionCollector.getErrors.length)
    val matchedError: List[String] = loader.getExceptionCollector.getErrors.filter(error =>
      {
        error.contains("not one of the legal values")
      })
    expect(1)(matchedError.length)
  }

  def testCommunicationsDnp3WithIndexesSucceeds(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    val endpoint: Endpoint = createSimpleEndpoint(true)
    model.add(endpoint)

    loader.load(model, equipmentPointUnits)

    verifyNoLoadExceptions(loader)
    val protos: Seq[AnyRef] = modelLoader.allProtos.toSeq
    protos.length should equal(3)

    val commChannelList: Seq[AnyRef] = protos.filter(proto => proto.isInstanceOf[CommChannel])
    commChannelList.length should equal(1)
    commChannelList.apply(0).asInstanceOf[CommChannel].getName should equal("tcp://127.0.0.1:7799@any")

    val endPointConfigList: Seq[AnyRef] = protos.filter(proto => proto.isInstanceOf[EndpointProto])
    endPointConfigList.length should equal(1)
    endPointConfigList.apply(0).asInstanceOf[EndpointProto].getName should equal(endpoint.getName)

    val configFileList: Seq[AnyRef] = protos.filter(proto => proto.isInstanceOf[ConfigFile])
    configFileList.length should equal(1)
    configFileList.apply(0).asInstanceOf[ConfigFile].getName should equal(endpoint.getName + "-mapping.pi")
  }

  def testCommunicationsDnp3WithoutIndexesInBenchmarkModeSucceeds(fixture: Fixture) {
    import fixture._

    val equipmentPointUnits = HashMap[String, String]()
    equipmentPointUnits += ("ChapelHill.BigBkr.Mw" -> "Mw")

    val endpoint: Endpoint = createSimpleEndpoint(true)
    model.add(endpoint)

    loader.load(model, equipmentPointUnits, true)

    verifyNoLoadExceptions(loader)
    val protos: Seq[AnyRef] = modelLoader.allProtos.toSeq
    protos.length should equal(3)

    val endPointConfigList: Seq[AnyRef] = protos.filter(proto => proto.isInstanceOf[EndpointProto])
    endPointConfigList.length should equal(1)
    endPointConfigList.apply(0).asInstanceOf[EndpointProto].getName should equal(endpoint.getName)

    val configFileList: Seq[AnyRef] = protos.filter(proto => proto.isInstanceOf[ConfigFile])
    configFileList.length should equal(1)
    configFileList.apply(0).asInstanceOf[ConfigFile].getName should equal(endpoint.getName + "-sim.pi")
  }

  def createSimpleEndpoint(): Endpoint =
    {
      createSimpleEndpoint(false)
    }

  def createSimpleEndpoint(withIndices: Boolean): Endpoint =
    {
      val endpoint: Endpoint = new Endpoint("SimulatedEndpoint", Some(CommunicationsLoader.DNP3))
      val interface: Interface = new Interface("interface1", "127.0.0.1", 7799)
      val breaker: Equipment = new Equipment("BigBkr")

      val mwAnalog = if (withIndices)
        new Analog("Mw", Some(555))
      else
        new Analog("Mw")

      breaker.add(mwAnalog)
      val equipmentGroup: Equipment = new Equipment("ChapelHill")
      equipmentGroup.add(breaker)
      endpoint.set(interface).add(equipmentGroup)

      endpoint
    }

  private def makeEquipment(
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

    new Equipment(substationName).add(breaker)
  }

  private def logCollectedExceptions(loader: BaseConfigurationLoader) {
    if (loader.getExceptionCollector.hasErrors) {
      loader.getExceptionCollector.getErrors.foreach(error => logger.info("error: " + error))
    }
  }

  private def verifyNoLoadExceptions(loader: BaseConfigurationLoader) {
    logCollectedExceptions(loader)
    loader.getExceptionCollector.hasErrors should equal(false)
  }

}

