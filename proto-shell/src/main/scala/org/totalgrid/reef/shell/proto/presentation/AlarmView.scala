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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.client.service.proto.Alarms.Alarm
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Events.Event
import org.totalgrid.reef.util.Table

object AlarmView {

  def printTable(alarms: List[Alarm]) = {
    Table.printTable(header, alarms.map(row(_)))
  }

  def header = {
    "Id" :: "State" :: "Type" :: "Sev" :: "Device or Subsystem" :: "User" :: "Time" :: "Message" :: Nil
  }

  def row(a: Alarm) = {
    val e = a.getEvent
    a.getId.getValue :: a.getState.toString :: e.getEventType :: e.getSeverity.toString :: associatedEntity(e) :: e.getUserId :: EventView.timeString(e.time) :: e.getRendered :: Nil
  }

  def associatedEntity(e: Event): String = {
    if (e.hasEntity)
      e.getEntity.getName
    else
      e.getSubsystem
  }
}