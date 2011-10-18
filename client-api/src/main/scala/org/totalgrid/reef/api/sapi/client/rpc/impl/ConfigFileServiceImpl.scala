package org.totalgrid.reef.api.sapi.client.rpc.impl

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
import com.google.protobuf.ByteString

import org.totalgrid.reef.api.japi.client.rpc.impl.builders.{ EntityRequestBuilders, ConfigFileRequestBuilders }
import org.totalgrid.reef.api.sapi.impl.OptionalProtos._
import org.totalgrid.reef.api.proto.Model.{ ConfigFile, ReefUUID }

import org.totalgrid.reef.api.sapi.client.rpc.ConfigFileService
import org.totalgrid.reef.api.japi.ExpectationException
import org.totalgrid.reef.api.sapi.client.rpc.impl.framework.HasAnnotatedOperations

/**
 * implementation of the ConfigFileService Interface. The calls are implemented including the verbs and whatever
 * processing of the results. This will allow us to hide the irregularities in the current service implementation
 * (EventList selectors for instance) and even replace the single request/response type with multiple types without
 * disturbing client code (much). We can also add additional assertions on client behavior here to fail faster and
 * let people fall into the 'pit of the success' more often
 */
trait ConfigFileServiceImpl extends HasAnnotatedOperations with ConfigFileService {

  override def getAllConfigFiles() = ops.operation("Couldn't get list of all config files") {
    _.get(ConfigFileRequestBuilders.getAll).map(_.many)
  }

  override def getConfigFileByUid(uid: ReefUUID) = ops.operation("Couldn't get config file with uid: " + uid.uuid) {
    _.get(ConfigFileRequestBuilders.getByUid(uid)).map(_.one)
  }

  override def getConfigFileByName(name: String) = ops.operation("Couldn't get config file with name: " + name) {
    _.get(ConfigFileRequestBuilders.getByName(name)).map(_.one)
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID) = {
    ops.operation("Couldn't get config files used by entity: " + entityUid.uuid) {
      _.get(ConfigFileRequestBuilders.getByEntity(entityUid)).map(_.many)
    }
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID, mimeType: String) = {
    ops.operation("Couldn't get config files used by entity: " + entityUid.uuid + " mimeType: " + mimeType) {
      _.get(ConfigFileRequestBuilders.getByEntity(entityUid, mimeType)).map(_.many)
    }
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte]) = {
    ops.operation("Couldn't create config file with name: " + name + " mimeType: " + mimeType + " dataLength: " + data.length) {
      _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data)).map(_.one)
    }
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte], entityUid: ReefUUID) = {
    ops.operation("Couldn't create config file with name: " + name + " mimeType: " + mimeType + " dataLength: " + data.length
      + " for entity: " + entityUid.uuid) {
      _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data, entityUid)).map(_.one)
    }
  }

  override def createConfigFile(mimeType: String, data: Array[Byte], entityUid: ReefUUID) = {
    ops.operation("Couldn't create config file with mimeType: " + mimeType + " dataLength: " + data.length
      + " for entity: " + entityUid.uuid) {
      _.put(ConfigFileRequestBuilders.makeConfigFile(mimeType, data, entityUid)).map(_.one)
    }
  }

  //TODO - Evaluate why we're doing client side validation. Seems that all validation should be server-side JAC

  override def updateConfigFile(configFile: ConfigFile, data: Array[Byte]) = {

    ops.operation("Couldn't update configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.put(configFile.toBuilder.setFile(ByteString.copyFrom(data)).build).map(_.one)
    }
  }

  override def addConfigFileUsedByEntity(configFile: ConfigFile, entityUid: ReefUUID) = {

    ops.operation("Couldn't not associate: " + entityUid.uuid + " with configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.put(configFile.toBuilder.addEntities(EntityRequestBuilders.getByUid(entityUid)).build).map(_.one)
    }
  }

  override def deleteConfigFile(configFile: ConfigFile) = {

    ops.operation("Couldn't delete configFile uuid: " + configFile.uuid + " name: " + configFile.name) { session =>
      if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set.")
      session.delete(ConfigFile.newBuilder.setUuid(configFile.getUuid).build).map(_.one)
    }
  }
}

