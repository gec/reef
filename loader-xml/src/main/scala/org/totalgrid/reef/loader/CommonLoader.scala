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

import java.io.{ File, FileInputStream }
import com.google.protobuf.ByteString

import org.totalgrid.reef.loader.common.{ Info, Attribute, ConfigFile, ConfigFiles }

import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.EnhancedXmlClasses._
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.proto.Model.{ EntityEdge, EntityAttributes, Entity, ConfigFile => ConfigFileProto }

class CommonLoader(modelLoader: ModelLoader, exceptionCollector: ExceptionCollector, rootDir: File) extends Logging {

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
      name match {
        case s: String if (s.endsWith(".xml")) => "text/xml"
        case s: String if (s.endsWith(".jpg")) => "image/jpeg"
        case s: String if (s.endsWith(".jpeg")) => "image/jpeg"
        case s: String if (s.endsWith(".png")) => "image/png"
        case s: String if (s.endsWith(".gif")) => "image/gif"
        case s: String if (s.endsWith(".svg")) => "image/svg+xml"
        case _ => throw new LoadingException("Cannot guess mimeType for configfile, must be explictly defined: " + name)
      }
    } else {
      configFile.getMimeType
    }

    val proto = ConfigFileProto.newBuilder.setName(name).setMimeType(mimeType)

    val bytes = if (hasFilename) {
      val file = new File(rootDir, configFile.getFileName)
      if (!file.exists()) throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " doesn't exist.")
      readFileToByteArray(file, mimeType)
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

  def readFileToByteArray(file: File, mimeType: String): Array[Byte] = {
    if (mimeType.startsWith("image")) {
      val fis = new FileInputStream(file);
      val length = file.length()
      if (length > Integer.MAX_VALUE) {
        throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " is larger than " + Integer.MAX_VALUE + " bytes")
      }
      val buffer = new Array[Byte](length.toInt);
      fis.read(buffer);
      fis.close();
      buffer
    } else {
      scala.io.Source.fromFile(file).mkString.getBytes
    }
  }

  def addInfo(entity: Entity, info: Info) {
    logger.info("adding info for entity: " + entity + ", info: " + info)

    exceptionCollector.collect("Adding info for entity: " + entity.getName) {

      info.getConfigFile.map(configFile => loadConfigFile(configFile, Some(entity)))

      val attributeProto = toAttribute(entity, info.getAttribute)
      attributeProto.foreach(modelLoader.putOrThrow(_))
    }
  }

  def toAttribute(entity: Entity, attrElements: List[Attribute]): Option[EntityAttributes] = {
    import org.totalgrid.reef.proto.Utils.{ Attribute => AttributeProto }
    import org.totalgrid.reef.proto.Utils.Attribute.Type

    if (attrElements.isEmpty) return None

    val builder = EntityAttributes.newBuilder.setEntity(entity)

    attrElements.foreach { attrElement =>
      val attributeProtoBuilder = AttributeProto.newBuilder.setName(attrElement.getName)
      attrElement.doubleValue.foreach { v => attributeProtoBuilder.setValueDouble(v); attributeProtoBuilder.setVtype(Type.DOUBLE) }
      attrElement.intValue.foreach { v => attributeProtoBuilder.setValueSint64(v); attributeProtoBuilder.setVtype(Type.SINT64) }
      attrElement.booleanValue.foreach { v => attributeProtoBuilder.setValueBool(v); attributeProtoBuilder.setVtype(Type.BOOL) }
      attrElement.stringValue.foreach { v => attributeProtoBuilder.setValueString(v); attributeProtoBuilder.setVtype(Type.STRING) }
      builder.addAttributes(attributeProtoBuilder)
    }

    Some(builder.build)
  }

}