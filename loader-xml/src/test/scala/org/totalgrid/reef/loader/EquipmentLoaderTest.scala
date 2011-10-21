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

import helpers.{ ModelContainer, CachingModelLoader }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.fixture.FixtureSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.loader.sx.equipment._
import org.scalatest.mock.MockitoSugar
import collection.mutable.{ Seq, Queue, HashMap }
import com.weiglewilczek.slf4s.Logging
import org.junit.Assert
import org.totalgrid.reef.proto.Model.{ Entity, ConfigFile }
import sx.communications.CommunicationsModel
import sx.{ InfoXmlBean, ConfigFileXmlBean }
import scala.None

import org.totalgrid.reef.api.sapi.client.SuccessResponse
import org.totalgrid.reef.proto.Model

@RunWith(classOf[JUnitRunner])
class EquipmentLoaderTest extends FixtureSuite with BeforeAndAfterAll with ShouldMatchers with MockitoSugar with Logging {

  case class Fixture(modelLoader: ModelLoader, exceptionCollector: ExceptionCollector, loader: EquipmentLoader,
      model: EquipmentModel) {
    def reset {
      loader.reset
      modelLoader.reset()
      exceptionCollector.reset()
      model.reset
    }
  }

  type FixtureParam = Fixture

  /**
   *  This is run before each test.
   */
  def withFixture(test: OneArgTest) =
    {
      val modelLoader = new CachingModelLoader(None)
      val model = new EquipmentModel
      val exceptionCollector = new LoadingExceptionCollector
      val commonLoader = new CommonLoader(modelLoader, exceptionCollector, new java.io.File("."))
      val loader = new EquipmentLoader(modelLoader, new LoadCache().loadCacheEquipment, exceptionCollector, commonLoader)

      test(Fixture(modelLoader, exceptionCollector, loader, model))
    }

  def testEquipmentModelProfiles(fixture: Fixture) =
    {
      import fixture._

      val actionModel = HashMap[String, configuration.ActionSet]()
      val breakerProfile = makeEquipmentProfile("FullBreaker")

      val profiles = new Profiles
      model.setProfiles(profiles)

      profiles.add(breakerProfile)
      model.add(makeSubstationWithBreaker("ChapelHill", false, Some(breakerProfile))) // F: no triggers
      loader.load(model, actionModel)
      val container: ModelContainer = loader.getModelLoader.getModelContainer
      val protos = container.getModels
      protos.length should equal(12)

      // TODO break into two tests
      reset

      model.add(makeSubstationWithBreaker("ChapelHill", false)) // No trigger, No profile
      loader.load(model, actionModel)
      container.getModels.length should equal(12)

      loader.getExceptionCollector.hasErrors should equal(false)
    }

  def testEquipmentModelProfilesWithTriggers(fixture: Fixture) =
    {
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

      model.add(makeSubstationWithBreaker("ChapelHill", true, Some(breakerProfile)))

      loader.load(model, actionModel)
      var container: ModelContainer = loader.getModelLoader.getModelContainer
      var protos = container.getModels
      protos.length should equal(12)
      modelLoader.getModelContainer.getTriggerSets().length should equal(1)

      // TODO break into two tests
      reset

      model.add(makeSubstationWithBreaker("ChapelHill", true)) // No profile
      loader.load(model, actionModel)
      container = loader.getModelLoader.getModelContainer
      protos = container.getModels
      protos.length should equal(12)
      modelLoader.getModelContainer.getTriggerSets().length should equal(1)

      loader.getExceptionCollector.hasErrors should equal(false)
    }

  def testSimple(fixture: Fixture) =
    {
      import fixture._

      val actionModel = HashMap[String, configuration.ActionSet]()
      actionModel += ("Nominal" -> makeActionSetNominal())

      val normallyFalsePointProfile: PointProfile = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
      val normallyTruePointProfile: PointProfile = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
      val breakerProfile: EquipmentProfile = makeEquipmentProfile("FullBreaker", Some(normallyTruePointProfile))

      val profiles = new Profiles
      profiles.add(normallyTruePointProfile)
      profiles.add(normallyFalsePointProfile)
      profiles.add(breakerProfile)
      model.setProfiles(profiles)

      val substation: Equipment = makeSubstation("Substation1")
      val breaker1: Equipment = makeBreaker("Breaker1", Some(breakerProfile), true)
      substation.add(breaker1)
      model.add(substation)

      loader.load(model, actionModel)
      val modelLoader: ModelLoader = loader.getModelLoader
      val modelContainer: ModelContainer = modelLoader.getModelContainer
      modelContainer.getEntities().foreach(entity => logger.info("entity: " + entity))

      var entity: Option[Entity] = modelContainer.getEntity("Substation1")
      Assert.assertFalse(entity.isEmpty)
      entity = modelContainer.getEntity("Substation1.Breaker1")
      Assert.assertFalse(entity.isEmpty)

      loader.getExceptionCollector.hasErrors should equal(false)
    }

  def testEquipmentWithConfigFile(fixture: Fixture) =
    {
      import fixture._

      val actionModel = HashMap[String, configuration.ActionSet]()
      actionModel += ("Nominal" -> makeActionSetNominal())

      val normallyFalsePointProfile: PointProfile = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
      val normallyTruePointProfile: PointProfile = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
      val breakerProfile: EquipmentProfile = makeEquipmentProfile("FullBreaker", Some(normallyTruePointProfile))

      val profiles = new Profiles
      profiles.add(normallyTruePointProfile)
      profiles.add(normallyFalsePointProfile)
      profiles.add(breakerProfile)
      model.setProfiles(profiles)

      val substation: Equipment = makeSubstation("Substation1")
      val breaker1: Equipment = makeBreaker("Breaker1", Some(breakerProfile), true)
      substation.add(breaker1)

      val mimeType: String = "oneline/xml"
      val onelineContent: String = "oneline content"
      val info: InfoXmlBean = createInfoConfigFile(createInlineConfig("oneline", mimeType, onelineContent))
      substation.add(info)

      model.add(substation)

      loader.load(model, actionModel)
      if (loader.getExceptionCollector.hasErrors) {
        loader.getExceptionCollector.getErrors.foreach(error => logger.info("error: " + error))
      }
      loader.getExceptionCollector.hasErrors should equal(false)

      val modelLoader: ModelLoader = loader.getModelLoader
      val modelContainer: ModelContainer = modelLoader.getModelContainer
      modelContainer.getEntities().foreach(entity => logger.info("entity: " + entity))

      var substationEntity: Option[Entity] = modelContainer.getEntity("Substation1")
      Assert.assertFalse(substationEntity.isEmpty)
      var entity = modelContainer.getEntity("Substation1.Breaker1")
      Assert.assertFalse(entity.isEmpty)
      val optionOfConfigFile: Option[ConfigFile] = modelContainer.getConfigFile("oneline")
      Assert.assertFalse(optionOfConfigFile.isEmpty)
      val configFile: ConfigFile = optionOfConfigFile.get
      Assert.assertEquals("oneline", configFile.getName)
      Assert.assertEquals(mimeType, configFile.getMimeType)
      Assert.assertEquals(onelineContent, configFile.getFile.toStringUtf8)
    }

  def testEquipmentTwoConfigFilesFails(fixture: Fixture) =
    {
      import fixture._

      val actionModel = HashMap[String, configuration.ActionSet]()
      actionModel += ("Nominal" -> makeActionSetNominal())

      val normallyFalsePointProfile: PointProfile = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
      val normallyTruePointProfile: PointProfile = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
      val breakerProfile: EquipmentProfile = makeEquipmentProfile("FullBreaker", Some(normallyTruePointProfile))

      val profiles = new Profiles
      profiles.add(normallyTruePointProfile)
      profiles.add(normallyFalsePointProfile)
      profiles.add(breakerProfile)
      model.setProfiles(profiles)

      val substation: Equipment = makeSubstation("Substation1")
      val breaker1: Equipment = makeBreaker("Breaker1", Some(breakerProfile), true)
      substation.add(breaker1)

      val mimeType: String = "oneline/xml"
      val onelineContent: String = "oneline content"
      substation.add(createInfoConfigFile(createInlineConfig("oneline", mimeType, onelineContent)))
      substation.add(createInfoConfigFile(createInlineConfig("oneline2", mimeType, onelineContent)))

      model.add(substation)

      loader.load(model, actionModel)
      if (loader.getExceptionCollector.hasErrors) {
        loader.getExceptionCollector.getErrors.foreach(error => logger.info("error: " + error))
      }
      loader.getExceptionCollector.hasErrors should equal(true)
      loader.getExceptionCollector.getErrors.exists(error => error.contains("More than one Info defined")) should equal(true)
    }

  private def createChildEntityName(substationEntity: Model.Entity, childName: String): String =
    {
      substationEntity.getName + "." + childName
    }

  def testTwoEquipmentGroupsWithConfigFiles(fixture: Fixture) =
    {
      import fixture._

      val actionModel = HashMap[String, configuration.ActionSet]()
      actionModel += ("Nominal" -> makeActionSetNominal())

      val normallyFalsePointProfile: PointProfile = makePointProfile("NormallyFalseAlarmed", true, "Nominal")
      val normallyTruePointProfile: PointProfile = makePointProfile("NormallyTrueAlarmed", false, "Nominal")
      val breakerProfile: EquipmentProfile = makeEquipmentProfile("FullBreaker", Some(normallyTruePointProfile))

      val profiles = new Profiles
      profiles.add(normallyTruePointProfile)
      profiles.add(normallyFalsePointProfile)
      profiles.add(breakerProfile)
      model.setProfiles(profiles)

      val mimeType: String = "oneline/xml"
      val onelineContent: String = "oneline content"

      var substation: Equipment = makeSubstation("Substation1", makeBreaker("Breaker1", Some(breakerProfile), true))
      substation.add(createInfoConfigFile(createInlineConfig("onelineA", mimeType, onelineContent)))
      model.add(substation)

      substation = makeSubstation("Substation2", makeBreaker("Breaker1", Some(breakerProfile), true))
      substation.add(createInfoConfigFile(createInlineConfig("onelineB", mimeType, onelineContent)))
      model.add(substation)

      loader.load(model, actionModel)
      if (loader.getExceptionCollector.hasErrors) {
        loader.getExceptionCollector.getErrors.foreach(error => logger.info("error: " + error))
      }
      loader.getExceptionCollector.hasErrors should equal(false)

      val modelLoader: ModelLoader = loader.getModelLoader
      val modelContainer: ModelContainer = modelLoader.getModelContainer
      modelContainer.getEntities().foreach(entity => logger.info("entity: " + entity))

      var substationEntity: Option[Entity] = modelContainer.getEntity("Substation1")
      Assert.assertFalse(substationEntity.isEmpty)
      var entity = modelContainer.getEntity("Substation1.Breaker1")
      Assert.assertFalse(entity.isEmpty)
      var optionOfConfigFile: Option[ConfigFile] = modelContainer.getConfigFile("onelineA")
      Assert.assertFalse(optionOfConfigFile.isEmpty)
      var configFile: ConfigFile = optionOfConfigFile.get
      Assert.assertEquals("onelineA", configFile.getName)
      Assert.assertEquals(mimeType, configFile.getMimeType)
      Assert.assertEquals(onelineContent, configFile.getFile.toStringUtf8)

      substationEntity = modelContainer.getEntity("Substation2")
      Assert.assertFalse(substationEntity.isEmpty)
      entity = modelContainer.getEntity("Substation2.Breaker1")
      Assert.assertFalse(entity.isEmpty)
      optionOfConfigFile = modelContainer.getConfigFile("onelineB")
      Assert.assertFalse(optionOfConfigFile.isEmpty)
      configFile = optionOfConfigFile.get
      Assert.assertEquals("onelineB", configFile.getName)
      Assert.assertEquals(mimeType, configFile.getMimeType)
      Assert.assertEquals(onelineContent, configFile.getFile.toStringUtf8)
    }

  private def makeSubstation(name: String, subEquipment: Equipment): Equipment =
    {
      val substation: Equipment = makeSubstation(name)
      substation.add(subEquipment)
      substation
    }

  private def createInfoConfigFile(configFile: ConfigFileXmlBean): InfoXmlBean =
    {
      val info = new InfoXmlBean()
      info.getConfigFileOrAttribute.add(configFile)
      info
    }

  private def createConfigFile(name: String, mimeType: String, fileName: String): ConfigFileXmlBean =
    {
      val configFile = new ConfigFileXmlBean()
      configFile.setFileName(fileName)
      configFile.setName(name)
      configFile.setMimeType(mimeType)
      configFile
    }

  private def createInlineConfig(name: String, mimeType: String, content: String): ConfigFileXmlBean =
    {
      val configFile = new ConfigFileXmlBean()
      configFile.setName(name)
      configFile.setMimeType(mimeType)
      configFile.setValue(content)
      configFile
    }

  private def makeBreaker(name: String, equipmentProfile: Option[EquipmentProfile], isTrigger: Boolean): Equipment =
    {
      val breaker = new Equipment(name)
      equipmentProfile match {
        case Some(profile) =>
          breaker.add(profile)
        case None =>
          val status = new Status("Bkr", "status")

          if (isTrigger) {
            status.add(new Unexpected(false, "Nominal"))
            status.add(new Transform("raw", "status", new ValueMap("false", "CLOSED"), new ValueMap("true", "OPEN")))
          }
          breaker.add(new Type("Breaker")).add(new Control("trip")).add(new Control("close")).add(status)
      }
      breaker
    }

  private def makeSubstation(substationName: String): Equipment =
    {
      new Equipment(substationName).add(new Type("Substation"))
    }

  private def makeSubstationWithBreaker(substationName: String, isTrigger: Boolean, equipmentProfile: Option[EquipmentProfile] = None): Equipment =
    {
      val breaker: Equipment = makeBreaker("Brk1", equipmentProfile, isTrigger)
      new Equipment(substationName).add(new Type("Substation")).add(breaker)
    }

  private def makeEquipmentProfile(profileName: String, pointProfile: Option[PointProfile] = None): EquipmentProfile =
    {
      new EquipmentProfile(profileName).add(new Type("Breaker")).add(new Control("trip")).add(new Control("close"))
        .add(new Status("Bkr", "status", pointProfile))
    }

  private def makePointProfile(profileName: String, value: Boolean, actionSet: String): PointProfile =
    {
      new PointProfile(profileName).add(new Unexpected(value, actionSet))
        .add(new Transform("raw", "status", new ValueMap("false", "CLOSED"), new ValueMap("true", "OPEN")))
    }

  private def makeActionSetNominal(): sx.ActionSet =
    {
      val rising = new configuration.Rising()
      rising.setMessage(new sx.Message("Scada.OutOfNominal"))

      val high = new configuration.High
      high.setSetAbnormal(new Object)

      val as = new sx.ActionSet("Nominal")
      as.setRising(rising)
      as.setHigh(high)
      as
    }

  private def logProtos(list: Seq[AnyRef]) {
    logger.debug("")
    logger.debug("protos: " + list.length)
    list.foreach(proto => logger.debug("proto(" + proto.getClass.getSimpleName + "): " + proto))
    logger.debug("")
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

