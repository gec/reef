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
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.proto.Model.{ EntityEdge, EntityAttributes, Entity, ConfigFile => ConfigFileProto }

class CommonLoader(modelLoader: ModelLoader, exceptionCollector: ExceptionCollector, rootDir: File) extends Logging {
  val configFiles = LoaderMap[ConfigFileProto]("Config Files")

  def reset() {
    configFiles.clear()
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

  def loadConfigFile(configFile: ConfigFile): ConfigFileProto =
    {
      loadConfigFile(configFile, None)
    }

  def loadConfigFile(configFile: ConfigFile, namePrefixOption: Option[String]): ConfigFileProto = {
    if (!configFile.isSetName && !configFile.isSetFileName) {
      throw new LoadingException("Need to set either fileName or name for configFile: " + configFile)
    }

    val name = if (configFile.isSetName) {
      namePrefixOption match {
        case None => configFile.getName
        case Some(namePrefix) => namePrefix + "." + configFile.getName
      }
    } else {
      configFile.getFileName
    }
    logger.debug(
      "processing config file: name: " + configFile.getName + ", fileName: " + configFile.getFileName + ", mimeType: " + configFile.getMimeType)

    val cachedConfigFile = configFiles.get(name)
    val hasCData = configFile.isSetValue && configFile.getValue.size > 0
    val hasFilename = configFile.isSetFileName

    if (cachedConfigFile.isDefined) {
      if (hasCData) throw new LoadingException("Cannot use same name as already loaded config file while respecifying data: " + name)
      return cachedConfigFile.get
    }

    if (hasFilename && hasCData) throw new LoadingException("Cannot have both filename and inline-data for configFile: " + name)

    val mimeType = if (!configFile.isSetMimeType) {
      name match {
        case s: String if (s.endsWith(".xml")) => "text/xml"
        case _ => throw new LoadingException("Cannot guess mimeType for configfile, must be explictly defined: " + name)
      }
    } else {
      configFile.getMimeType
    }

    val proto = ConfigFileProto.newBuilder.setName(name).setMimeType(mimeType)

    val bytes = if (hasFilename) {
      val file = new File(rootDir, configFile.getFileName)
      if (!file.exists()) throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " doesn't exist.")
      scala.io.Source.fromFile(file).mkString.getBytes
    } else {
      configFile.getValue.getBytes
    }

    proto.setFile(ByteString.copyFrom(bytes))
    val configFileProto = proto.build
    logger.debug("new config file proto: name: " + name + ", mimeType: " + configFileProto.getMimeType)
    configFiles.put(name, configFileProto)
    modelLoader.putOrThrow(configFileProto)
    configFileProto
  }

  // TODO replace implementation with addInfo() method with add name prefix false
  def addInfo(entity: Entity, info: Info) {
    logger.info("adding info for entity: " + entity + ", info: " + info)

    exceptionCollector.collect("Adding info for: " + entity.getName) {
      val configFileProtos: List[ConfigFileProto] = info.getConfigFile.map(configFile => loadConfigFile(configFile))
      val configFileEdge: List[EntityEdge] = configFileProtos
        .map(configFile => ProtoUtils.toEntityEdge(entity, ProtoUtils.toEntityType(configFile.getName, "ConfigurationFile" :: Nil), "uses"))
      configFileEdge.foreach(modelLoader.putOrThrow(_))

      val attributeProto = toAttribute(entity, info.getAttribute)
      attributeProto.foreach(modelLoader.putOrThrow(_))
    }
  }

  def addInfo(entity: Entity, info: Info, addEntityNamePrefix: Boolean) {
    logger.info("adding info for entity: " + entity + ", add entity prefix: " + addEntityNamePrefix + ", info: " + info)

    exceptionCollector.collect("Adding info for entity: " + entity.getName) {
      val configFileProtos: List[ConfigFileProto] = info.getConfigFile.map(configFile =>
        {
          if (addEntityNamePrefix) {
            loadConfigFile(configFile, Some(entity.getName))
          } else {
            loadConfigFile(configFile)
          }
        })

      val configFileEdge: List[EntityEdge] = configFileProtos
        .map(configFile => ProtoUtils.toEntityEdge(entity, ProtoUtils.toEntityType(configFile.getName, "ConfigurationFile" :: Nil), "uses"))
      configFileEdge.foreach(modelLoader.putOrThrow(_))

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