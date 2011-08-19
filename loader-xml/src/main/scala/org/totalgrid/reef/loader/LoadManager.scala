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

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.configuration._

import org.totalgrid.reef.sapi.client.RestOperations

import org.totalgrid.reef.util.{ Logging, XMLHelper }
import java.io.File
import org.totalgrid.reef.loader.helpers.{ CachingModelLoader, SymbolResponseProgressRenderer }
import org.totalgrid.reef.loader.common.ConfigFile

object LoadManager extends Logging {

  /**
   * TODO: Catch file not found exceptions and call usage.
   */
  def loadFile(client: => RestOperations, filename: String, benchmark: Boolean, dryRun: Boolean, ignoreWarnings: Boolean = false,
      createConfiguration: Boolean = true) =
  {
    val file = new File(filename)
    logger.info("Loading configuration file: " + file)

    try {
      validateXml(filename)

      val xml = XMLHelper.read(file, classOf[Configuration])

      val loader = new CachingModelLoader(None, createConfiguration)
      val valid = loadConfiguration(loader, xml, benchmark, Some(file, filename), file.getParentFile)

      logger.info("Finished analyzing configuration '" + filename + "'")

      if (!valid && !ignoreWarnings) {
        println("Configuration invalid, fix errors or add ignoreWarnings argument")
        false
      } else if (!dryRun) {
        val progress = new SymbolResponseProgressRenderer(Console.out)
        loader.flush(client, Some(progress))
        println("Configuration loaded.")
        true
      } else {
        println("DRYRUN: Skipping upload of " + loader.size + " objects.")
        true
      }

    } catch {
      case ex =>
        println("Error loading configuration file '" + filename + "' " + ex.getMessage)
        throw ex
    }
  }

  def loadConfiguration(client: ModelLoader, xml: Configuration, benchmark: Boolean, configurationFileTuple: Option[(File, String)] = None,
      path: File = new File(".")): Boolean =
  {
    var equipmentPointUnits = HashMap[String, String]()
    val actionModel = HashMap[String, ActionSet]()

    if (!xml.isSetEquipmentModel && !xml.isSetCommunicationsModel && !xml.isSetMessageModel)
      throw new Exception("No equipmentModel, communicationsModel, or messageModel. Nothing to do.")

    val loadCache = new LoadCache
    val exceptionCollector = new LoadingExceptionCollector
    try {
      val commonLoader = new CommonLoader(client, exceptionCollector, path)

      if (xml.isSetConfigFiles) commonLoader.load(xml.getConfigFiles)

      if (xml.isSetMessageModel) {
        val messageLoader = new MessageLoader(client, exceptionCollector)
        val messageModel = xml.getMessageModel
        messageLoader.load(messageModel)
      }

      if (xml.isSetActionModel) {
        val actionSets = xml.getActionModel.getActionSet.toList
        actionSets.foreach(as => actionModel += (as.getName -> as))
      }

      if (xml.isSetEquipmentModel) {
        val equipmentLoader = new EquipmentLoader(client, loadCache.loadCacheEquipment, exceptionCollector, commonLoader)
        val equipmentModel = xml.getEquipmentModel
        equipmentPointUnits = equipmentLoader.load(equipmentModel, actionModel)
      }

      if (xml.isSetCommunicationsModel) {
        val comLoader = new CommunicationsLoader(client, loadCache.loadCacheCommunication, exceptionCollector, commonLoader)
        val comModel = xml.getCommunicationsModel
        comLoader.load(comModel, equipmentPointUnits, benchmark)
      }

      configurationFileTuple.foreach {
        case (thisFile, fileName) =>
          val configFile = new ConfigFile()
          configFile.setMimeType("text/xml")
          configFile.setFileName(thisFile.getName)
          configFile.setName(fileName)
          client.putOrThrow(commonLoader.loadConfigFile(configFile))
      }

    } catch {
      case exception: Exception =>
        exceptionCollector.addError("Terminal parsing error: ", exception)
        logger.warn(exception.getStackTraceString)
    }

    val errors = exceptionCollector.getErrors

    if (errors.size > 0) {
      println
      println("Critical Errors found:")
      errors.foreach(println(_))
      println("Fix Critical Errors and try again.")
      println
      false
    } else {
      loadCache.validate
    }
  }

  def validateXml(filename: String) = {

    import javax.xml.validation.SchemaFactory
    import javax.xml.transform.stream.StreamSource
    import org.xml.sax.{ SAXParseException, ErrorHandler, SAXException }

    val sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")

    // load the schema definition out the jar file
    val schemaLocation = this.getClass.getResource("/configuration.xsd")
    val schema = sf.newSchema(schemaLocation)
    val validator = schema.newValidator()

    // load the xml file
    val source = new StreamSource(filename)

    val ex = new LoadingExceptionCollector
    val handler = new ErrorHandler {
      def warning(exception: SAXParseException) = ex.addError("Validation Warning", exception)
      def error(exception: SAXParseException) = ex.addError("Validation Error", exception)
      def fatalError(exception: SAXParseException) {
        ex.addError("Fatal Validation Error", exception)
        throw exception
      }
    }

    try {
      validator.setErrorHandler(handler)
      validator.validate(source)
    } catch {
      case e: SAXException =>
        throw new LoadingException("error during validation: " + e.getMessage)
    }

    val errors = ex.getErrors
    if (errors.size > 0) {
      println
      println("Validation errors found:")
      errors.foreach(println(_))
      throw new LoadingException("Fix Xml Validation errors and try again.")
    }
  }
}

