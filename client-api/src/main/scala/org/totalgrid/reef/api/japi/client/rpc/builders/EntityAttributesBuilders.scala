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
package org.totalgrid.reef.api.japi.client.rpc.impl.builders

import org.totalgrid.reef.proto.Utils.Attribute
import com.google.protobuf.ByteString
import org.totalgrid.reef.proto.Model.{ ReefUUID, Entity, EntityAttributes }

object EntityAttributesBuilders {

  def getForEntityUid(uuid: ReefUUID) = {
    EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uuid)).build
  }

  def getForEntityName(name: String) = {
    EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setName(name)).build
  }

  def putAttributesToEntityUid(uuid: ReefUUID, attributes: java.util.List[Attribute]) = {
    EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setUuid(uuid)).addAllAttributes(attributes).build
  }

  def putAttributesToEntityName(name: String, attributes: java.util.List[Attribute]) = {
    EntityAttributes.newBuilder.setEntity(Entity.newBuilder.setName(name)).addAllAttributes(attributes).build
  }

  def boolAttribute(name: String, value: Boolean): Attribute = {
    Attribute.newBuilder.setName(name).setVtype(Attribute.Type.BOOL).setValueBool(value).build
  }

  def longAttribute(name: String, value: Long): Attribute = {
    Attribute.newBuilder.setName(name).setVtype(Attribute.Type.SINT64).setValueSint64(value).build
  }

  def stringAttribute(name: String, value: String): Attribute = {
    Attribute.newBuilder.setName(name).setVtype(Attribute.Type.STRING).setValueString(value).build
  }

  def doubleAttribute(name: String, value: Double): Attribute = {
    Attribute.newBuilder.setName(name).setVtype(Attribute.Type.DOUBLE).setValueDouble(value).build
  }

  def byteArrayAttribute(name: String, value: Array[Byte]): Attribute = {
    Attribute.newBuilder.setName(name).setVtype(Attribute.Type.BYTES).setValueBytes(ByteString.copyFrom(value)).build
  }

}