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
import org.totalgrid.reef.services.core.util.AttributeList
import org.totalgrid.reef.event.{ SystemEventSink, EventType }

trait ServiceModelSystemEventPublisher { self: EnvHolder =>
  def subsystem: String = "Core"
  def userId: String = env.userName.getOrElse("")
  def time: Long = System.currentTimeMillis()

  def eventSink: SystemEventSink

  def postSystemEvent(eventType: EventType, args: (String, Any)*) {
    val b = Event.newBuilder
      .setTime(time)
      .setEventType(eventType.toString)
      .setSubsystem(subsystem)
      .setUserId(userId)

    if (!args.isEmpty) {
      val aList = new AttributeList
      args.foreach { case (name, value) => aList.addAttribute(name, value) }
      b.setArgs(aList.toProto)
    }
    eventSink.publishSystemEvent(b.build)
  }
}