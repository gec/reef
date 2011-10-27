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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.proto.Events.Event
import org.totalgrid.reef.proto.Model.{ Entity, ReefUUID }
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.HasAnnotatedOperations

//import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.Utils.{ AttributeList, Attribute }

import org.totalgrid.reef.client.sapi.rpc.EventCreationService
import org.totalgrid.reef.clientapi.sapi.client.rest.RestOperations

trait EventCreationServiceImpl extends HasAnnotatedOperations with EventCreationService {

  override def publishEvent(event: Event) = ops.operation("Couldn't publish event: " + event) {
    _.put(event).map(_.one)
  }

  override def publishEvent(eventType: String, subsystem: String) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, None, None, None)
    }
  }

  override def publishEvent(eventType: String, subsystem: String, deviceTime: Long, arguments: List[Attribute]) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, Some(deviceTime), None, Some(arguments))
    }
  }

  override def publishEvent(eventType: String, subsystem: String, arguments: List[Attribute]) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, None, None, Some(arguments))
    }
  }

  override def publishEvent(eventType: String, subsystem: String, deviceTime: Long, entityUuid: ReefUUID) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, Some(deviceTime), Some(entityUuid), None)
    }
  }

  override def publishEvent(eventType: String, subsystem: String, entityUuid: ReefUUID) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, None, Some(entityUuid), None)
    }
  }

  override def publishEvent(eventType: String, subsystem: String, entityUuid: ReefUUID, arguments: List[Attribute]) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, None, Some(entityUuid), Some(arguments))
    }
  }

  override def publishEvent(eventType: String, subsystem: String, deviceTime: Long, entityUuid: ReefUUID, arguments: List[Attribute]) = {
    ops.operation("Couldn't publish event with type: " + eventType) {
      makeNewEvent(_, eventType, subsystem, Some(deviceTime), Some(entityUuid), Some(arguments))
    }
  }

  private def makeNewEvent(session: RestOperations,
    eventType: String,
    subsystem: String,
    dt: Option[Long],
    uuid: Option[ReefUUID],
    args: Option[List[Attribute]]) = {

    val b = Event.newBuilder.setEventType(eventType).setSubsystem(subsystem)
    dt.foreach(b.setDeviceTime(_))
    uuid.foreach(u => b.setEntity(Entity.newBuilder.setUuid(u)))

    args.foreach { argList =>
      val aList = AttributeList.newBuilder
      argList.toList.foreach { aList.addAttribute(_) }
      b.setArgs(aList)
    }
    session.put(b.build).map(_.one)
  }
}