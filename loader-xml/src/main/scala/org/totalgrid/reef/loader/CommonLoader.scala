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

import java.io.File
import com.google.protobuf.ByteString

import org.totalgrid.reef.loader.common.{ Info, Attribute, ConfigFile, ConfigFiles }

import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.EnhancedXmlClasses._
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.util.IOHelpers
import org.totalgrid.reef.client.service.proto.Processing.TriggerSet

import scala.collection.mutable
import org.totalgrid.reef.client.service.proto.Model.{ EntityAttribute, EntityEdge, EntityAttributes, Entity, ConfigFile => ConfigFileProto }

object CommonLoader {

  def inferMimeType(filename: String): Option[String] = {
    val parts = filename.split('.')
    if (parts.length > 1) {
      parts.last.toLowerCase match {
        case "xml" => Some("text/xml")
        case "jpg" => Some("image/jpeg")
        case "jpeg" => Some("image/jpeg")
        case "png" => Some("image/png")
        case "gif" => Some("image/gif")
        case "svg" => Some("image/svg+xml")
        case other => None
      }
    } else {
      None
    }
  }

}

class CommonLoader(modelLoader: ModelLoader, exceptionCollector: ExceptionCollector, rootDir: File) extends Logging {

  val triggerCache = mutable.Map.empty[String, TriggerSet]

  def reset() {
    triggerCache.clear()
  }

  def getExceptionCollector: ExceptionCollector = {
    exceptionCollector
  }

  def load(files: ConfigFiles) {
    logger.info("loading config files")
    loadConfigFiles(files.getConfigFile.toList)
  }

  def loadConfigFiles(configFiles: List[ConfigFile]): List[ConfigFileProto] = {
    logger.info("loading config files: " + configFiles.size)
    var list = List.empty[ConfigFileProto]
    exceptionCollector.collect("Loading config files: ") {
      list = configFiles.map(loadConfigFile(_))
    }
    list
  }

  def loadConfigFile(configFile: ConfigFile, entity: Option[Entity] = None): ConfigFileProto = {
    if (!configFile.isSetName && !configFile.isSetFileName) {
      throw new LoadingException("Need to set either fileName or name for configFile: " + configFile)
    }

    val name = if (configFile.isSetName) configFile.getName else configFile.getFileName
    logger.debug(
      "processing config file: name: " + configFile.getName + ", fileName: " + configFile.getFileName + ", mimeType: " + configFile.getMimeType)

    val hasCData = configFile.isSetValue && configFile.getValue.size > 0
    val hasFilename = configFile.isSetFileName

    if (hasFilename && hasCData) throw new LoadingException("Cannot have both filename and inline-data for configFile: " + name)

    val mimeType = if (!configFile.isSetMimeType) {
      CommonLoader.inferMimeType(name) getOrElse {
        throw new LoadingException("Cannot guess mimeType for configfile, must be explictly defined: " + name)
      }
    } else {
      configFile.getMimeType
    }

    val proto = ConfigFileProto.newBuilder.setName(name).setMimeType(mimeType)

    val bytes = if (hasFilename) {
      val file = new File(rootDir, configFile.getFileName)
      if (!file.exists()) throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " doesn't exist.")
      IOHelpers.readBinary(file)
    } else {
      configFile.getValue.getBytes
    }

    proto.setFile(ByteString.copyFrom(bytes))
    entity.foreach(proto.addEntities(_))
    val configFileProto = proto.build
    logger.debug("new config file proto: name: " + name + ", mimeType: " + configFileProto.getMimeType)
    modelLoader.putOrThrow(configFileProto)
    configFileProto
  }

  def addInfo(entity: Entity, info: Info) {
    exceptionCollector.collect("Adding info for entity: " + entity.getName) {

      info.getConfigFile.map(configFile => loadConfigFile(configFile, Some(entity)))

      val attributeProto = toAttribute(entity, info.getAttribute)
      attributeProto.foreach(modelLoader.putOrThrow(_))
    }
  }

  def toAttribute(entity: Entity, attrElements: List[Attribute]): List[EntityAttribute] = {
    import org.totalgrid.reef.client.service.proto.Utils.{ Attribute => AttributeProto }
    import org.totalgrid.reef.client.service.proto.Utils.Attribute.Type

    attrElements.map { attr =>
      val entAttr = EntityAttribute.newBuilder.setEntity(entity)
      val b = AttributeProto.newBuilder.setName(attr.getName)
      attr.doubleValue.foreach { v => b.setValueDouble(v); b.setVtype(Type.DOUBLE) }
      attr.intValue.foreach { v => b.setValueSint64(v); b.setVtype(Type.SINT64) }
      attr.booleanValue.foreach { v => b.setValueBool(v); b.setVtype(Type.BOOL) }
      attr.stringValue.foreach { v => b.setValueString(v); b.setVtype(Type.STRING) }
      entAttr.setAttribute(b)
      entAttr.build
    }
  }

}