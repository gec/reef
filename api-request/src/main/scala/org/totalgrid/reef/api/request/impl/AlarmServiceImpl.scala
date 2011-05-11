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
import org.totalgrid.reef.api.request.{ AlarmService }
import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.request.builders.{ AlarmListRequestBuilders, AlarmRequestBuilders }
import org.totalgrid.reef.api.javaclient.IEventAcceptor
import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Model.ReefUUID

trait AlarmServiceImpl extends ReefServiceBaseClass with AlarmService {
  def getAlarm(uid: String) = {
    reThrowExpectationException("Alarm with UID: " + uid + " not found") {
      ops { _.getOneOrThrow(AlarmRequestBuilders.getByUID(uid)) }
    }
  }

  def getActiveAlarms(limit: Int) = {
    ops { _.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledged(limit)).getAlarmsList }
  }
  def getActiveAlarms(limit: Int, sub: ISubscription[Alarm]) = {
    ops { _.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledged(limit), sub).getAlarmsList }
  }

  def getActiveAlarms(types: java.util.List[String], limit: Int) = {
    ops { _.getOneOrThrow(AlarmListRequestBuilders.getUnacknowledgedWithTypes(types, limit)).getAlarmsList }
  }

  def removeAlarm(alarm: Alarm) = {
    ops { _.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.REMOVED).build) }
  }

  def acknowledgeAlarm(alarm: Alarm) = {
    ops { _.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.ACKNOWLEDGED).build) }
  }

  def silenceAlarm(alarm: Alarm) = {
    ops { _.putOneOrThrow(alarm.toBuilder.setState(Alarm.State.UNACK_SILENT).build) }
  }

  def createAlarmSubscription(callback: IEventAcceptor[Alarm]) = {
    ops { _.addSubscription(Descriptors.alarm.getKlass, callback.onEvent) }
  }
}