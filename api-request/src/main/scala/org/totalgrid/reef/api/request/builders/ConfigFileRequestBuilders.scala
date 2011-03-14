package org.totalgrid.reef.api.request.builders

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

import com.google.protobuf.ByteString

import org.totalgrid.reef.proto.Model.{ Entity, ConfigFile }
import org.totalgrid.reef.api.request.ReefUUID

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
