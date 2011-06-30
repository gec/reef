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
package org.totalgrid.reef.services.framework

import org.totalgrid.reef.proto.Events.Event
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto }

import org.totalgrid.reef.services.core.util.AttributeList
import org.totalgrid.reef.event.{ SystemEventSink, EventType }
import org.totalgrid.reef.models.Entity
import org.totalgrid.reef.services.framework.SquerylModel._
import org.totalgrid.reef.japi.InternalServiceException

trait SystemEventCreator {

  /**
   * this is an easy to use method for creating a system event with name/value pairs and handles the
   * creation of attributes based on the closest fit of the attribute type
   */
  def createSystemEvent(
    eventType: String,
    subsystem: String,
    entityUuid: Option[Entity] = None,
    entityName: Option[String] = None,
    args: List[(String, Any)] = Nil,
    deviceTime: Option[Long] = None): Event.Builder = {

    val b = Event.newBuilder
      .setEventType(eventType.toString)
      .setSubsystem(subsystem)

    deviceTime.foreach(b.setDeviceTime(_))
    entityUuid.foreach { u => b.setEntity(EntityProto.newBuilder.setUuid(makeUuid(u))) }
    entityName.foreach { n => b.setEntity(EntityProto.newBuilder.setName(n)) }

    if (!args.isEmpty) {
      val aList = new AttributeList
      args.foreach { case (name, value) => aList.addAttribute(name, value) }
      b.setArgs(aList.toProto)
    }
    b
  }
}

/**
 * a trait to be mixed into the model classes to make creating and publishing events as simple as possible
 * so we will be more inclined to add them everywhere necessary.
 */
trait ServiceModelSystemEventPublisher extends SystemEventCreator { self: EnvHolder =>

  def eventSink: SystemEventSink

  def postSystemEvent(eventType: EventType, entity: Option[Entity] = None, args: List[(String, Any)] = Nil, deviceTime: Option[Long] = None) {

    // TODO: put name of services instance handling request on RequestContext, attach to events
    val b = createSystemEvent(eventType, "services", entity, None, args, deviceTime)

    b.setUserId(env.userName.getOrElse(throw new InternalServiceException("No user during event generation")))
    b.setTime(System.currentTimeMillis())

    eventSink.publishSystemEvent(b.build)
  }
}