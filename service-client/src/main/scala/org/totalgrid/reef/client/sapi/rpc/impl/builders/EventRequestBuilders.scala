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
package org.totalgrid.reef.client.sapi.rpc.impl.builders

import org.totalgrid.reef.client.service.proto.Events.Event
import org.totalgrid.reef.client.service.proto.Utils.{ AttributeList, Attribute }
import org.totalgrid.reef.client.service.proto.Model.ReefID

object EventRequestBuilders {
  def makeNewEventForEntityByName(eventType: String, entityName: String) = {
    Event.newBuilder.setEventType(eventType).setEntity(EntityRequestBuilders.getByName(entityName)).build
  }

  def makeNewEventWithAttributes(eventType: String, tuples: Tuple2[String, String]*) = {
    val attrs = AttributeList.newBuilder()
    tuples.foreach {
      case (name, value) =>
        attrs.addAttribute(Attribute.newBuilder.setName(name).setValueString(value).setVtype(Attribute.Type.STRING))
    }
    Event.newBuilder.setEventType(eventType).setArgs(attrs).build
  }

  def getByUID(id: ReefID) = {
    Event.newBuilder.setId(id).build
  }
}