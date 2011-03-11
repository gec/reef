/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Model.{ Entity, ConfigFile }
import com.google.protobuf.ByteString

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.ExpectationException

/**
 * the RequestBuilders objects are used to encapsulate most of the direct proto manipulations and
 * minimize duplication of Builder code.
 */
object ConfigFileRequestBuilders {
  def getByUid(uid: ReefUUID) = ConfigFile.newBuilder().setUid(uid.uuid).build
  def getByName(name: String) = ConfigFile.newBuilder().setName(name).build

  def getByMimeType(mimeType: String) = ConfigFile.newBuilder().setMimeType(mimeType).build

  def getByEntity(entity: Entity) = ConfigFile.newBuilder().addEntities(entity).build

  def getByEntity(entity: Entity, mimeType: String) = {
    ConfigFile.newBuilder().setMimeType(mimeType).addEntities(entity).build
  }

  private def makeBasicConfigFile(name: String, mimeType: String, data: Array[Byte]) = {
    ConfigFile.newBuilder().setName(name).setMimeType(mimeType).setFile(ByteString.copyFrom(data))
  }

  def makeConfigFile(name: String, mimeType: String, data: Array[Byte]) = {
    makeBasicConfigFile(name, mimeType, data).build
  }

  def makeConfigFile(name: String, mimeType: String, data: Array[Byte], entity: Entity) = {
    makeBasicConfigFile(name, mimeType, data).addEntities(entity).build
  }
}

/**
 * implementation of the ConfigFileService Interface. The calls are implemented including the verbs and whatever
 * processing of the results. This will allow us to hide the irregularities in the current service implementation
 * (EventList selectors for instance) and even replace the single request/response type with multiple types without
 * disturbing client code (much). We can also add additional assertions on client behavior here to fail faster and
 * let people fall into the 'pit of the success' more often
 */
trait ConfigFileServiceImpl extends ReefServiceBaseClass with ConfigFileService {

  def getConfigFileByUid(uid: ReefUUID): ConfigFile = {
    ops.getOneOrThrow(ConfigFileRequestBuilders.getByUid(uid))
  }
  def getConfigFileByName(name: String): ConfigFile = {
    ops.getOneOrThrow(ConfigFileRequestBuilders.getByName(name))
  }

  def getConfigFilesUsedByEntity(entity: Entity): java.util.List[ConfigFile] = {
    ops.getOrThrow(ConfigFileRequestBuilders.getByEntity(entity))
  }
  def getConfigFilesUsedByEntityUid(entityUid: ReefUUID): java.util.List[ConfigFile] = {
    getConfigFilesUsedByEntity(EntityRequestBuilders.getByUid(entityUid))
  }
  def getConfigFilesUsedByEntityName(entityName: String): java.util.List[ConfigFile] = {
    getConfigFilesUsedByEntity(EntityRequestBuilders.getByName(entityName))
  }

  def getConfigFilesUsedByEntity(entity: Entity, mimeType: String): java.util.List[ConfigFile] = {
    ops.getOrThrow(ConfigFileRequestBuilders.getByEntity(entity, mimeType))
  }
  def getConfigFilesUsedByEntityUid(entityUid: ReefUUID, mimeType: String): java.util.List[ConfigFile] = {
    getConfigFilesUsedByEntity(EntityRequestBuilders.getByUid(entityUid), mimeType)
  }
  def getConfigFilesUsedByEntityName(entityName: String, mimeType: String): java.util.List[ConfigFile] = {
    getConfigFilesUsedByEntity(EntityRequestBuilders.getByName(entityName), mimeType)
  }

  def createConfigFile(name: String, mimeType: String, data: Array[Byte]): ConfigFile = {
    ops.putOneOrThrow(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data))
  }
  def createConfigFile(name: String, mimeType: String, data: Array[Byte], entity: Entity): ConfigFile = {
    ops.putOneOrThrow(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data, entity))
  }
  def createConfigFile(name: String, mimeType: String, data: Array[Byte], entityUid: ReefUUID): ConfigFile = {
    ops.putOneOrThrow(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data, EntityRequestBuilders.getByUid(entityUid)))
  }

  def updateConfigFile(configFile: ConfigFile, data: Array[Byte]): ConfigFile = {
    if (!configFile.hasUid) throw new ExpectationException("uid field is expected to be set. Cannot update a config file with only a name, need to know uid.")
    ops.putOneOrThrow(configFile.toBuilder.setFile(ByteString.copyFrom(data)).build)
  }

  def addConfigFileUserByEntity(configFile: ConfigFile, entity: Entity): ConfigFile = {
    if (!configFile.hasUid) throw new ExpectationException("uid field is expected to be set. Cannot add a config file user with only a name, need to know uid.")
    ops.putOneOrThrow(configFile.toBuilder.addEntities(entity).build)
  }

  def deleteConfigFile(configFile: ConfigFile): ConfigFile = {
    if (!configFile.hasUid) throw new ExpectationException("uid field is expected to be set. Cannot delete a config file with only a name, need to know uid.")
    ops.deleteOneOrThrow(ConfigFile.newBuilder.setUid(configFile.getUid).build)
  }
}