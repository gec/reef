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
package org.totalgrid.reef.api.request.impl

import org.totalgrid.reef.api.request.EventService
import org.totalgrid.reef.api.ISubscription
import org.totalgrid.reef.proto.Events.Event
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.request.builders.{ AlarmRequestBuilders, AlarmListRequestBuilders, EventListRequestBuilders }
import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.javaclient.IEventAcceptor
import org.totalgrid.reef.proto.Descriptors

trait EventServiceImpl extends ReefServiceBaseClass with EventService {

  def getRecentEvents(limit: Int) = {
    val ret = ops.getOneOrThrow(EventListRequestBuilders.getAll(limit))
    ret.getEventsList
  }
  def getRecentEvents(limit: Int, sub: ISubscription[Event]) = {
    // TODO: add subscription to EventList service
    // currently eventlist doesnt let us subscribe so we work around it by calling regular event
    // service, only issue is the limit is fixed.
    val events = ops.getOrThrow(Event.newBuilder.setEventType("*").build, sub)
    if (limit > events.size) throw new Exception("Limit larger than temporary limit of : " + events.size)
    events.slice(0, limit)
  }
  def publishEvent(event: Event) = {
    ops.putOneOrThrow(event)
  }

  def getRecentAlarms(limit: Int) = {
    val ret = ops.getOneOrThrow(AlarmListRequestBuilders.getAll(limit))
    ret.getAlarmsList
  }
  def getRecentAlarms(limit: Int, sub: ISubscription[Alarm]) = {
    // TODO: add subscription to AlarmList service
    val alarms = ops.getOrThrow(AlarmRequestBuilders.getAllByType("*"), sub)
    if (limit > alarms.size) throw new Exception("Limit larger than temporary limit of : " + alarms.size)
    alarms.slice(0, limit)
  }

  def createAlarmSubscription(callback: IEventAcceptor[Alarm]) = {
    ops.addSubscription(Descriptors.alarm.getKlass, callback.onEvent)
  }

  def createEventSubscription(callback: IEventAcceptor[Event]) = {
    ops.addSubscription(Descriptors.event.getKlass, callback.onEvent)
  }

  def removeAlarm(alarm: Alarm) = {
    ops.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.REMOVED).build)
  }

  def acknowledgeAlarm(alarm: Alarm) = {
    ops.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.ACKNOWLEDGED).build)
  }

  def silenceAlarm(alarm: Alarm) = {
    ops.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.UNACK_SILENT).build)
  }
}