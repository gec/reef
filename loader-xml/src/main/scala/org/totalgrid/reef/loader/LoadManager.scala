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
  def loadFile(client: => RestOperations, filename: String, benchmark: Boolean, dryRun: Boolean, ignoreWarnings: Boolean = false, createConfiguration: Boolean = true) = {

    logger.info("Loading configuration file '" + filename + "'")

    val file = new File(filename)
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

  def loadConfiguration(client: ModelLoader, xml: Configuration, benchmark: Boolean, configurationFile: Option[(File, String)] = None, path: File = new File(".")): Boolean = {

    var equipmentPointUnits = HashMap[String, String]()
    val actionModel = HashMap[String, ActionSet]()

    if (!xml.isSetEquipmentModel && !xml.isSetCommunicationsModel && !xml.isSetMessageModel)
      throw new Exception("No equipmentModel, communicationsModel, or messageModel. Nothing to do.")

    val loadCache = new LoadCache
    val ex = new LoadingExceptionCollector
    try {
      if (xml.isSetMessageModel) {
        val messageLoader = new MessageLoader(client, ex)
        val messageModel = xml.getMessageModel
        messageLoader.load(messageModel)
      }

      if (xml.isSetActionModel) {
        val actionSets = xml.getActionModel.getActionSet.toList
        actionSets.foreach(as => actionModel += (as.getName -> as))
      }

      if (xml.isSetEquipmentModel) {
        val equLoader = new EquipmentLoader(client, loadCache.loadCacheEqu, ex)
        val equModel = xml.getEquipmentModel
        equipmentPointUnits = equLoader.load(equModel, actionModel)
      }

      if (xml.isSetCommunicationsModel) {
        val comLoader = new CommunicationsLoader(client, loadCache.loadCacheCom, ex)
        val comModel = xml.getCommunicationsModel
        comLoader.load(comModel, path, equipmentPointUnits, benchmark)
      }

      configurationFile.foreach {
        case (thisFile, fileName) =>
          val cf = new ConfigFile()
          cf.setMimeType("text/xml")
          cf.setFileName(fileName)
          client.putOrThrow(ProtoUtils.toConfigFile(cf, new File(".")))
      }

    } catch {
      case exception: Exception =>
        ex.addError("Terminal parsing error: ", exception)
        logger.warn(exception.getStackTraceString)
    }

    val errors = ex.getErrors

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

    //    import javax.xml.validation.SchemaFactory
    //    import javax.xml.transform.stream.StreamSource
    //    import org.xml.sax.{ SAXParseException, ErrorHandler, SAXException }
    //
    //    val sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
    //
    //    val schemaLocation = new File("schema/xsd/configuration.xsd")
    //    if (!schemaLocation.exists()) throw new LoadingException("Cannot find schema to validate against: " + schemaLocation.getAbsolutePath)
    //
    //    val schema = sf.newSchema(schemaLocation)
    //
    //    val validator = schema.newValidator()
    //
    //    val source = new StreamSource(filename)
    //
    //    val ex = new LoadingExceptionCollector
    //
    //    val handler = new ErrorHandler {
    //      def warning(exception: SAXParseException) {
    //        ex.addError("Validation Warning: ", exception)
    //      }
    //
    //      def error(exception: SAXParseException) {
    //        ex.addError("Validation Error: ", exception)
    //      }
    //
    //      def fatalError(exception: SAXParseException) {
    //        ex.addError("Fatal Validation Error: ", exception)
    //        throw exception
    //      }
    //    }
    //
    //    try {
    //      validator.setErrorHandler(handler)
    //      validator.validate(source)
    //    } catch {
    //      case e: SAXException =>
    //        throw new LoadingException("error during validation: " + e.getMessage)
    //    }
    //
    //    val errors = ex.getErrors
    //
    //    if (errors.size > 0) {
    //      println
    //      println("Validation errors found:")
    //      errors.foreach(println(_))
    //      throw new LoadingException("Fix Xml Validation errors and try again.")
    //    }
  }
}

