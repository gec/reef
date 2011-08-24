/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.japi.request.impl

import com.google.protobuf.ByteString

import scala.collection.JavaConversions._
import org.totalgrid.reef.japi.ExpectationException
import org.totalgrid.reef.japi.request.{ ConfigFileService }
import org.totalgrid.reef.japi.request.builders.{ EntityRequestBuilders, ConfigFileRequestBuilders }
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Model.{Entity, ConfigFile, ReefUUID}
import collection.mutable.Buffer

/**
 * implementation of the ConfigFileService Interface. The calls are implemented including the verbs and whatever
 * processing of the results. This will allow us to hide the irregularities in the current service implementation
 * (EventList selectors for instance) and even replace the single request/response type with multiple types without
 * disturbing client code (much). We can also add additional assertions on client behavior here to fail faster and
 * let people fall into the 'pit of the success' more often
 */
trait ConfigFileServiceImpl extends ReefServiceBaseClass with ConfigFileService {

  override def getAllConfigFiles(): java.util.List[ConfigFile] = ops("Couldn't get list of all config files") {
    _.get(ConfigFileRequestBuilders.getAll).await().expectMany()
  }

  override def getConfigFileByUid(uid: ReefUUID): ConfigFile = ops("Couldn't get config file with uid: " + uid.uuid) {
    _.get(ConfigFileRequestBuilders.getByUid(uid)).await().expectOne
  }

  override def getConfigFileByName(name: String): ConfigFile = ops("Couldn't get config file with name: " + name) {
    _.get(ConfigFileRequestBuilders.getByName(name)).await().expectOne
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID): java.util.List[ConfigFile] = {
    ops("Couldn't get config files used by entity: " + entityUid.uuid) {
      _.get(ConfigFileRequestBuilders.getByEntity(entityUid)).await().expectMany()
    }
  }

  override def getConfigFileWithRelativeName(entity: Entity, relativeName: String): ConfigFile = {
    val configFiles: java.util.List[ConfigFile] = ops(
      "Couldn't get config file for entity: " + entity.getName + "(" + entity.getUuid + "), with relative name: " + relativeName)
    {
      _.get(ConfigFileRequestBuilders.getByEntity(entity.getUuid)).await().expectMany()
    }

    val filteredFiles: Buffer[ConfigFile] = configFiles.filter(configFile => configFile.getName.equals(entity.getName + "." + relativeName))
    if ( filteredFiles.length == 0 ) {
      return null
    }
    if ( filteredFiles.length == 1 ) {
      return filteredFiles.get(0)
    }
    throw new ExpectationException(
      "Received unexpected results when retrieving config file for entity: " + entity.getName + "(" + entity.getUuid + "), with relative name: " +
          relativeName)
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID, mimeType: String): java.util.List[ConfigFile] = {
    ops("Couldn't get config files used by entity: " + entityUid.uuid + " mimeType: " + mimeType) {
      _.get(ConfigFileRequestBuilders.getByEntity(entityUid, mimeType)).await().expectMany()
    }
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte]): ConfigFile = {
    ops("Couldn't create config file with name: " + name + " mimeType: " + mimeType + " dataLength: " + data.length) {
      _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data)).await().expectOne
    }
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte], entityUid: ReefUUID): ConfigFile = {
    ops("Couldn't create config file with name: " + name + " mimeType: " + mimeType + " dataLength: " + data.length
      + " for entity: " + entityUid.uuid) {
      _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data, entityUid)).await().expectOne
    }
  }

  //TODO - Evaluate why we're doing client side validation. Seems that all validation should be server-side JAC

  override def updateConfigFile(configFile: ConfigFile, data: Array[Byte]): ConfigFile = {

    ops("Couldn't update configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.put(configFile.toBuilder.setFile(ByteString.copyFrom(data)).build).await().expectOne
    }
  }

  override def addConfigFileUsedByEntity(configFile: ConfigFile, entityUid: ReefUUID): ConfigFile = {

    ops("Couldn't not associate: " + entityUid.uuid + " with configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.put(configFile.toBuilder.addEntities(EntityRequestBuilders.getByUid(entityUid)).build).await().expectOne
    }
  }

  override def deleteConfigFile(configFile: ConfigFile): ConfigFile = {

    ops("Couldn't delete configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.delete(ConfigFile.newBuilder.setUuid(configFile.getUuid).build).await().expectOne
    }
  }
}

