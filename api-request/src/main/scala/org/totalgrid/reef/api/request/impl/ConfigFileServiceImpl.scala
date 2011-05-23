package org.totalgrid.reef.api.request.impl

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
import org.totalgrid.reef.proto.Model.{ Entity, ConfigFile, ReefUUID }
import com.google.protobuf.ByteString

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.ExpectationException
import org.totalgrid.reef.api.request.{ ConfigFileService }
import org.totalgrid.reef.api.request.builders.{ EntityRequestBuilders, ConfigFileRequestBuilders }

/**
 * implementation of the ConfigFileService Interface. The calls are implemented including the verbs and whatever
 * processing of the results. This will allow us to hide the irregularities in the current service implementation
 * (EventList selectors for instance) and even replace the single request/response type with multiple types without
 * disturbing client code (much). We can also add additional assertions on client behavior here to fail faster and
 * let people fall into the 'pit of the success' more often
 */
trait ConfigFileServiceImpl extends ReefServiceBaseClass with ConfigFileService {

  override def getAllConfigFiles(): java.util.List[ConfigFile] = ops {
    _.get(ConfigFileRequestBuilders.getAll).await().expectMany()
  }

  override def getConfigFileByUid(uid: ReefUUID): ConfigFile = ops {
    _.get(ConfigFileRequestBuilders.getByUid(uid)).await().expectOne
  }

  override def getConfigFileByName(name: String): ConfigFile = ops {
    _.get(ConfigFileRequestBuilders.getByName(name)).await().expectOne
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID): java.util.List[ConfigFile] = ops {
    _.get(ConfigFileRequestBuilders.getByEntity(entityUid)).await().expectMany()
  }

  override def getConfigFilesUsedByEntity(entityUid: ReefUUID, mimeType: String): java.util.List[ConfigFile] = ops {
    _.get(ConfigFileRequestBuilders.getByEntity(entityUid, mimeType)).await().expectMany()
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte]): ConfigFile = ops {
    _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data)).await().expectOne
  }

  override def createConfigFile(name: String, mimeType: String, data: Array[Byte], entityUid: ReefUUID): ConfigFile = ops {
    _.put(ConfigFileRequestBuilders.makeConfigFile(name, mimeType, data, entityUid)).await().expectOne
  }

  //TODO - Evaluate why we're doing client side validation. Seems that all validation should be server-side JAC

  override def updateConfigFile(configFile: ConfigFile, data: Array[Byte]): ConfigFile = {
    if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set. Cannot update a config file with only a name, need to know uid.")
    ops { _.put(configFile.toBuilder.setFile(ByteString.copyFrom(data)).build).await().expectOne }
  }

  override def addConfigFileUserByEntity(configFile: ConfigFile, entityUid: ReefUUID): ConfigFile = {
    if (!configFile.hasUuid) throw new ExpectationException("uid field is expected to be set. Cannot add a config file user with only a name, need to know uid.")
    ops { _.put(configFile.toBuilder.addEntities(EntityRequestBuilders.getByUid(entityUid)).build).await().expectOne }
  }

  override def deleteConfigFile(configFile: ConfigFile): ConfigFile = {
    if (!configFile.hasUuid) throw new ExpectationException("uuid field is expected to be set. Cannot delete a config file with only a name, need to know uid.")
    ops { _.delete(ConfigFile.newBuilder.setUuid(configFile.getUuid).build).await().expectOne }
  }
}

