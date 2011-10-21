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
package org.totalgrid.reef.models

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.util.LazyVar
import org.totalgrid.reef.services.core.EntityQueryManager
import java.util.UUID

case class EventStore(
    eventType: String,
    alarm: Boolean,
    time: Long,
    deviceTime: Option[Long],
    severity: Int,
    subsystem: String,
    userId: String,
    entityId: Option[UUID],
    args: Array[Byte],
    rendered: String) extends ModelWithId {

  // extra constructor for squeryl type inference
  def this() = this("", false, 0, Some(0), 0, "", "", Some(new UUID(0, 0)), Array[Byte](), "")

  val associatedAlarm = LazyVar(ApplicationSchema.alarms.where(a => a.eventUid === id).single)

  val entity = LazyVar(mayHaveOneByUuid(ApplicationSchema.entities, entityId))

  val groups = LazyVar(entityId.map { x => EntityQueryManager.getParentOfType(x, "owns", "EquipmentGroup").toList }.getOrElse(Nil))
  val equipments = LazyVar(entityId.map { x => EntityQueryManager.getParentOfType(x, "owns", "Equipment").toList }.getOrElse(Nil))
}

object EventConfigStore {
  import org.totalgrid.reef.proto.Alarms.EventConfig.Designation

  // Get the enum values from the proto.
  //
  val ALARM = Designation.ALARM.getNumber
  val EVENT = Designation.EVENT.getNumber
  val LOG = Designation.LOG.getNumber
}

case class EventConfigStore(
  val eventType: String, // Type of event: UserLogin, BreakerTrip, etc.
  val severity: Int, // Severity level
  val designation: Int, // Alarm, Event, or Log
  val alarmState: Int, // Initial alarm start state: UNACK_AUDIBLE, UNACK_SILENT, or ACKNOWLEDGED
  val resource: String,
  val builtIn: Boolean) extends ModelWithId // rendering string

/**
 * The Model for the Alarm. It's part DB map and part Model.
 */
object AlarmModel {
  import org.totalgrid.reef.proto.Alarms.Alarm.State

  // Get the enum values from the proto.
  //
  val UNACK_AUDIBLE = State.UNACK_AUDIBLE.getNumber
  val UNACK_SILENT = State.UNACK_SILENT.getNumber
  val ACKNOWLEDGED = State.ACKNOWLEDGED.getNumber
  val REMOVED = State.REMOVED.getNumber

  // Which state transitions are valid?
  // See: AlarmModel.isNextStateValid.
  //
  val ValidTransitions = Map(
    // FROM STATE->      TO STATES
    UNACK_AUDIBLE -> Map(UNACK_AUDIBLE -> true, UNACK_SILENT -> true, ACKNOWLEDGED -> true, REMOVED -> false),
    UNACK_SILENT -> Map(UNACK_AUDIBLE -> false, UNACK_SILENT -> true, ACKNOWLEDGED -> true, REMOVED -> false),
    ACKNOWLEDGED -> Map(UNACK_AUDIBLE -> false, UNACK_SILENT -> false, ACKNOWLEDGED -> true, REMOVED -> true),
    REMOVED -> Map(UNACK_AUDIBLE -> false, UNACK_SILENT -> false, ACKNOWLEDGED -> false, REMOVED -> true))
}

/**
 * The Model for the Alarm. It's part DB map and part Model.
 */
class AlarmModel(
    val state: Int,
    val eventUid: Long) extends ModelWithId {

  import AlarmModel._

  // Get an EventStore based on an EventType
  val event = LazyVar(hasOne(ApplicationSchema.events, eventUid))

  /**
   * Can we transition from our current state to the specified next state?
   */
  def isNextStateValid(nextState: Int): Boolean = ValidTransitions(state).apply(nextState);

  def isUnacked(): Boolean = state == UNACK_AUDIBLE || state == UNACK_SILENT
}