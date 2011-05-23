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

import org.totalgrid.reef.api.request.{ AlarmService }
import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.request.builders.{ AlarmListRequestBuilders, AlarmRequestBuilders }
import org.totalgrid.reef.proto.Descriptors

import scala.collection.JavaConversions._

trait AlarmServiceImpl extends ReefServiceBaseClass with AlarmService {

  override def getAlarm(uid: String) = ops {
    _.get(AlarmRequestBuilders.getByUID(uid)).await().expectOne("Alarm with UID: " + uid + " not found")
  }

  override def getActiveAlarms(limit: Int) = ops {
    _.get(AlarmListRequestBuilders.getUnacknowledged(limit)).await().expectOne.getAlarmsList
  }

  override def subscribeToActiveAlarms(limit: Int) = ops { session =>
    useSubscription(session, Descriptors.alarm.getKlass) { sub =>
      session.get(AlarmListRequestBuilders.getUnacknowledged(limit), sub).await().expectOne.getAlarmsList
    }
  }

  override def getActiveAlarms(types: java.util.List[String], limit: Int) = ops {
    _.get(AlarmListRequestBuilders.getUnacknowledgedWithTypes(types, limit)).await().expectOne.getAlarmsList
  }

  override def removeAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.REMOVED)

  override def acknowledgeAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.ACKNOWLEDGED)

  override def silenceAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.UNACK_SILENT)

  private def changeAlarmState(alarm: Alarm, state: Alarm.State) = ops {
    _.put(alarm.toBuilder.setState(state).build).await().expectOne
  }
}