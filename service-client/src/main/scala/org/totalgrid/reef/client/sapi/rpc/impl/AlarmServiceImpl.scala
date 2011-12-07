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

import org.totalgrid.reef.client.service.proto.Alarms.Alarm
import org.totalgrid.reef.client.sapi.rpc.impl.builders.{ AlarmListRequestBuilders, AlarmRequestBuilders }

import org.totalgrid.reef.client.service.proto.OptionalProtos._

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.Entity

import org.totalgrid.reef.client.sapi.rpc.AlarmService
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations

trait AlarmServiceImpl extends HasAnnotatedOperations with AlarmService {

  override def getAlarmById(id: String) = ops.operation("Couldn't get alarm with id: " + id) {
    _.get(AlarmRequestBuilders.getByUID(id)).map(_.one)
  }

  override def getActiveAlarms(limit: Int) = ops.operation("Couldn't get the last " + limit + " active alarms") {
    _.get(AlarmListRequestBuilders.getUnacknowledged(limit)).map(_.one.map(_.getAlarmsList.toList))
  }

  override def subscribeToActiveAlarms(limit: Int) = ops.subscription(Descriptors.alarm, "Couldn't subscribe to active alarms") { (sub, client) =>
    client.get(AlarmListRequestBuilders.getUnacknowledged(limit), sub).map(_.one.map(_.getAlarmsList.toList))
  }

  override def getActiveAlarms(types: List[String], limit: Int) = {
    ops.operation("Couldn't get active alarms with types: " + types) {
      _.get(AlarmListRequestBuilders.getUnacknowledgedWithTypes(types, limit)).map(_.one.map(_.getAlarmsList.toList))
    }
  }

  override def getActiveAlarmsByEntity(entityTree: Entity, types: List[String], recentAlarmLimit: Int) = {
    ops.operation("Couldn't get active alarms with types: " + types + " and entity: " + entityTree) {
      _.get(AlarmListRequestBuilders.getUnacknowledgedWithTypesAndEntity(types, entityTree, recentAlarmLimit)).map(_.one.map(_.getAlarmsList.toList))
    }
  }

  override def removeAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.REMOVED)

  override def acknowledgeAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.ACKNOWLEDGED)

  override def silenceAlarm(alarm: Alarm) = changeAlarmState(alarm, Alarm.State.UNACK_SILENT)

  private def changeAlarmState(alarm: Alarm, state: Alarm.State) = {
    ops.operation("Couldn't update alarm: " + alarm.id + " to state: " + state) {
      _.put(alarm.toBuilder.setState(state).build).map(_.one)
    }
  }
}