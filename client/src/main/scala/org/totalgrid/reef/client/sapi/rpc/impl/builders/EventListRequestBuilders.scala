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

import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Events.{ EventList, EventSelect }

object EventListRequestBuilders {
  def getByEventSelect(select: EventSelect): EventList = EventList.newBuilder.setSelect(select).build
  def getByEventSelect(select: EventSelect.Builder): EventList = EventList.newBuilder.setSelect(select).build

  private def withLimit(limit: Int) = EventSelect.newBuilder.setLimit(limit)
  private def noLimit() = EventSelect.newBuilder

  def getAll(): EventList = getByEventSelect(getAllSelector())
  def getAll(limit: Int): EventList = getByEventSelect(getAllSelector(limit))
  def getAllSelector(): EventSelect.Builder = noLimit().addEventType("*")
  def getAllSelector(limit: Int): EventSelect.Builder = withLimit(limit).addEventType("*")

  def getAllByEventType(typ: String): EventList = getByEventSelect(getAllByEventTypeSelector(typ))
  def getAllByEventType(typ: String, limit: Int): EventList = getByEventSelect(getAllByEventTypeSelector(typ, limit))
  def getAllByEventTypeSelector(typ: String): EventSelect.Builder = noLimit().addEventType(typ)
  def getAllByEventTypeSelector(typ: String, limit: Int): EventSelect.Builder = withLimit(limit).addEventType(typ)

  def getAllByEventTypes(typs: java.util.List[String]): EventList = getByEventSelect(noLimit.addAllEventType(typs))
  def getAllByEventTypes(typs: java.util.List[String], limit: Int): EventList = getByEventSelect(withLimit(limit).addAllEventType(typs))

  def getByTimeRangeAndSubsystem(from: Long, to: Long, subsystem: String, limit: Int) = {
    getByEventSelect(getByTimeRangeAndSubsystemSelector(from, to, subsystem, limit))
  }
  def getByTimeRangeAndSubsystemSelector(from: Long, to: Long, subsystem: String, limit: Int) = {
    withLimit(limit).setTimeFrom(from).setTimeTo(to).addSubsystem(subsystem)
  }
}