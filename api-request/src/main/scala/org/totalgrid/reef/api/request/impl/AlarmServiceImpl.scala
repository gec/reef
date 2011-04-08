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

import org.totalgrid.reef.api.ISubscription
import org.totalgrid.reef.api.request.{ AlarmService, ReefUUID }
import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.request.builders.{ AlarmListRequestBuilders, AlarmRequestBuilders }
import org.totalgrid.reef.api.javaclient.IEventAcceptor
import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._

trait AlarmServiceImpl extends ReefServiceBaseClass with AlarmService {
  def getAlarm(uuid: ReefUUID) = {
    reThrowExpectationException("Alarm with UUID: " + uuid.getUuid + " not found") {
      ops.getOneOrThrow(AlarmRequestBuilders.getByUUID(uuid))
    }
  }

  def getActiveAlarms(limit: Int) = {
    val ret = ops.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledged(limit))
    ret.getAlarmsList
  }
  def getActiveAlarms(limit: Int, sub: ISubscription[Alarm]) = {
    val ret = ops.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledged(limit), sub)
    ret.getAlarmsList
  }

  def getActiveAlarms(types: java.util.List[String], limit: Int) = {
    val ret = ops.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledgedWithTypes(types, limit))
    ret.getAlarmsList
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

  def createAlarmSubscription(callback: IEventAcceptor[Alarm]) = {
    ops.addSubscription(Descriptors.alarm.getKlass, callback.onEvent)
  }
}