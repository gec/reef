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

import org.totalgrid.reef.client.service.proto.Events.EventSelect
import org.totalgrid.reef.client.service.proto.Alarms.{ Alarm, AlarmSelect, AlarmList }
import org.totalgrid.reef.client.service.proto.Model.Entity

object AlarmListRequestBuilders {
  def getByAlarmSelect(select: AlarmSelect): AlarmList = AlarmList.newBuilder.setSelect(select).build
  def getByAlarmSelect(select: AlarmSelect.Builder): AlarmList = AlarmList.newBuilder.setSelect(select).build

  def getAlarmSelectFromEventSelect(select: EventSelect): AlarmSelect.Builder = AlarmSelect.newBuilder.setEventSelect(select)
  def getAlarmSelectFromEventSelect(select: EventSelect.Builder): AlarmSelect.Builder = AlarmSelect.newBuilder.setEventSelect(select)

  def getUnacknowledgedSelector() = AlarmSelect.newBuilder.addState(Alarm.State.UNACK_AUDIBLE).addState(Alarm.State.UNACK_SILENT)

  def getAll(): AlarmList = getByAlarmSelect(getAlarmSelectFromEventSelect(EventListRequestBuilders.getAllSelector()))
  def getAll(limit: Int): AlarmList = getByAlarmSelect(getAlarmSelectFromEventSelect(EventListRequestBuilders.getAllSelector(limit)))

  def getUnacknowledged(limit: Int): AlarmList = {
    getByAlarmSelect(getUnacknowledgedSelector().setEventSelect(EventSelect.newBuilder.setLimit(limit)))
  }

  def getUnacknowledgedWithType(typ: String, limit: Int): AlarmList = {
    getByAlarmSelect(getUnacknowledgedSelector().setEventSelect(EventSelect.newBuilder.setLimit(limit).addEventType(typ)))
  }

  def getUnacknowledgedWithTypes(typs: java.util.List[String], limit: Int): AlarmList = {
    getByAlarmSelect(getUnacknowledgedSelector().setEventSelect(EventSelect.newBuilder.setLimit(limit).addAllEventType(typs)))
  }

  def getUnacknowledgedWithTypesAndEntity(typs: java.util.List[String], entity: Entity, limit: Int): AlarmList = {
    getByAlarmSelect(getUnacknowledgedSelector().setEventSelect(EventSelect.newBuilder.setLimit(limit).addAllEventType(typs).addEntity(entity)))
  }
}