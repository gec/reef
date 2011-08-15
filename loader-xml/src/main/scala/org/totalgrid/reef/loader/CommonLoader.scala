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

import org.totalgrid.reef.proto.Model.{ EntityAttributes, Entity, ConfigFile => ConfigFileProto }
import org.totalgrid.reef.loader.common.{ Info, Attribute, ConfigFile, ConfigFiles }

import scala.collection.JavaConversions._
import org.totalgrid.reef.loader.EnhancedXmlClasses._

class CommonLoader(client: ModelLoader, ex: ExceptionCollector, rootDir: File) {
  val configFiles = LoaderMap[ConfigFileProto]("Config Files")

  def reset() {
    configFiles.clear()
  }

  def load(files: ConfigFiles) {
    loadConfigFiles(files.getConfigFile.toList)
  }

  def loadConfigFiles(cfs: List[ConfigFile]): List[ConfigFileProto] = {
    var list = List.empty[ConfigFileProto]
    ex.collect("Loading config files: ") {
      list = cfs.map(loadConfigFile(_))
    }
    list
  }

  def loadConfigFile(cf: ConfigFile): ConfigFileProto = {

    if (!cf.isSetName && !cf.isSetFileName) throw new LoadingException("Need to set either fileName or name for configFile.")

    val name = if (cf.isSetName) cf.getName else cf.getFileName

    val cachedConfigFile = configFiles.get(name)

    val hasCData = cf.isSetValue && cf.getValue.size > 0
    val hasFilename = cf.isSetFileName

    if (cachedConfigFile.isDefined) {
      if (hasCData) throw new LoadingException("Cannot use same name as already loaded config file while respecifing data: " + name)
      return cachedConfigFile.get
    }

    if (hasFilename && hasCData) throw new LoadingException("Cannot have both filename and inline-data for configFile: " + name)

    if (hasCData && cachedConfigFile.isDefined) {
      throw new LoadingException("Cannot use same name as already loaded config file while respecifing data: " + name)
    }

    val mimeType = if (!cf.isSetMimeType) {

      name match {
        case s: String if (s.endsWith(".xml")) => "text/xml"
        case _ => throw new LoadingException("Cannot guess mimeType for configfile, must be explictly defined: " + name)
      }

    } else {
      cf.getMimeType
    }

    val proto = ConfigFileProto.newBuilder
      .setName(name)
      .setMimeType(mimeType)

    val bytes = if (hasFilename) {
      val file = new File(rootDir, cf.getFileName)
      if (!file.exists()) throw new LoadingException("External ConfigFile: " + file.getAbsolutePath + " doesn't exist.")
      scala.io.Source.fromFile(file).mkString.getBytes
    } else {
      cf.getValue.getBytes
    }
    proto.setFile(ByteString.copyFrom(bytes))
    val protoCf = proto.build
    configFiles.put(name, protoCf)
    client.putOrThrow(protoCf)
    protoCf
  }

  def addInfo(entity: Entity, info: Info) {
    ex.collect("Adding info for: " + entity.getName) {
      val configFileProtos = info.getConfigFile.map(cf => loadConfigFile(cf))
      val configFileUses = configFileProtos.map(cf => ProtoUtils.toEntityEdge(entity, ProtoUtils.toEntityType(cf.getName, "ConfigurationFile" :: Nil), "uses"))
      configFileUses.foreach(client.putOrThrow(_))

      val attributeProto = toAttribute(entity, info.getAttribute)
      attributeProto.foreach(client.putOrThrow(_))
    }
  }

  def toAttribute(entity: Entity, attrElements: List[Attribute]): Option[EntityAttributes] = {
    import org.totalgrid.reef.proto.Utils.{ Attribute => AttributeProto }
    import org.totalgrid.reef.proto.Utils.Attribute.Type

    if (attrElements.isEmpty) return None

    val b = EntityAttributes.newBuilder.setEntity(entity)

    attrElements.foreach { attrElement =>
      val ab = AttributeProto.newBuilder.setName(attrElement.getName)
      attrElement.doubleValue.foreach { v => ab.setValueDouble(v); ab.setVtype(Type.DOUBLE) }
      attrElement.intValue.foreach { v => ab.setValueSint64(v); ab.setVtype(Type.SINT64) }
      attrElement.booleanValue.foreach { v => ab.setValueBool(v); ab.setVtype(Type.BOOL) }
      attrElement.stringValue.foreach { v => ab.setValueString(v); ab.setVtype(Type.STRING) }
      b.addAttributes(ab)
    }

    Some(b.build)
  }

}