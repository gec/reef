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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.client.service.proto.Alarms._

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.squeryl.Query
import java.util.UUID
import org.totalgrid.reef.models.{ Command, ApplicationSchema, EventConfigStore }
import org.totalgrid.reef.authz.VisibilityMap

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._

object EventConfigService {

  def builtInEventConfigurations() = {

    import org.totalgrid.reef.event.EventType._
    import org.totalgrid.reef.models.EventConfigStore

    val ALARM = EventConfig.Designation.ALARM.getNumber
    val EVENT = EventConfig.Designation.EVENT.getNumber
    val UNACK_SILENT = Alarm.State.UNACK_SILENT.getNumber

    def makeBuiltIn(name: String, designation: Int, serverity: Int, resource: String) = {
      name -> EventConfigStore(name, serverity, designation, UNACK_SILENT, resource, true)
    }
    List(makeBuiltIn(System.UserLogin, EVENT, 5, "User logged in: {user}"),
      makeBuiltIn(System.UserLoginFailure, ALARM, 1, "User login failed {reason}"),
      makeBuiltIn(System.UserLogout, EVENT, 5, "User logged out"),
      makeBuiltIn(System.SubsystemStarting, EVENT, 5, "Subsystem is starting"),
      makeBuiltIn(System.SubsystemStarted, EVENT, 5, "Subsystem has started"),
      makeBuiltIn(System.SubsystemStopping, EVENT, 5, "Subsystem is stopping"),
      makeBuiltIn(System.SubsystemStopped, EVENT, 5, "Subsystem has stopped"),

      makeBuiltIn(Scada.ControlExe, EVENT, 3, "Executed control {command}"),
      makeBuiltIn(Scada.UpdatedSetpoint, EVENT, 3, "Updated setpoint {command} to {value}"),
      makeBuiltIn(Scada.OutOfNominal, ALARM, 2, "Measurement not in nominal range: {value}"),
      makeBuiltIn(Scada.OutOfReasonable, ALARM, 2, "Measurement not reasonable: {value}"),
      makeBuiltIn(Scada.SetOverride, EVENT, 3, "Point overridden"),
      makeBuiltIn(Scada.SetNotInService, EVENT, 3, "Point removed from service"),
      makeBuiltIn(Scada.RemoveOverride, EVENT, 3, "Removed override on point"),
      makeBuiltIn(Scada.RemoveNotInService, EVENT, 3, "Returned point to service"),
      // comm endpoint events
      makeBuiltIn(Scada.CommEndpointOffline, EVENT, 2, "Endpoint {name} offline"),
      makeBuiltIn(Scada.CommEndpointOnline, EVENT, 2, "Endpoint {name} online"),
      makeBuiltIn(Scada.CommEndpointDisabled, EVENT, 3, "Endpoint {name} disabled"),
      makeBuiltIn(Scada.CommEndpointEnabled, EVENT, 3, "Endpoint {name} enabled")).toMap
  }

  def seed() {
    val eventConfigs = builtInEventConfigurations()
    val defaultEventNames = eventConfigs.keys.toList

    val alreadyIn = from(ApplicationSchema.eventConfigs)(sql =>
      where(sql.eventType in defaultEventNames)
        select (sql.eventType)).toList

    val needed = defaultEventNames.diff(alreadyIn)
    val toInsert = needed.map { eventConfigs(_) }.toList

    ApplicationSchema.eventConfigs.insert(toInsert)
  }
}

class EventConfigService(protected val model: EventConfigServiceModel)
    extends SyncModeledServiceBase[EventConfig, EventConfigStore, EventConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.eventConfig

  override def preCreate(context: RequestContext, request: EventConfig): EventConfig = {
    if (request.hasBuiltIn && request.getBuiltIn) {
      throw new BadRequestException("Cannot create an event configuration with \"builtIn\" flag set.")
    }
    populateProto(context, request)
  }

  override protected def preUpdate(context: RequestContext, request: EventConfig, existing: EventConfigStore): EventConfig = {
    populateProto(context, request)
  }

  private def populateProto(context: RequestContext, proto: EventConfig): EventConfig = {
    if (!proto.hasDesignation || !proto.hasEventType || !proto.hasSeverity || !proto.hasResource) {
      throw new BadRequestException("Must fill in designation, eventType, severity and resource fields.")
    }
    if (proto.getDesignation == EventConfig.Designation.ALARM) {
      if (!proto.hasAlarmState) throw new BadRequestException("Must set initial alarm state if designation is alarm")
      proto.getAlarmState match {
        case Alarm.State.UNACK_AUDIBLE | Alarm.State.UNACK_SILENT => // ok states
        case _ => throw new BadRequestException("Initial alarm state can only be UNACK_AUDIBLE or UNACK_SILENT")
      }
      proto
    } else {
      // remove the alarm state field for non alarm messages
      if (proto.hasAlarmState) proto.toBuilder.clearAlarmState().build
      else proto
    }
  }
}

class EventConfigServiceModel
    extends SquerylServiceModel[Long, EventConfig, EventConfigStore]
    with EventedServiceModel[EventConfig, EventConfigStore]
    with SimpleModelEntryCreation[EventConfig, EventConfigStore]
    with EventConfigConversion {

  def getProperties(eventType: String) = {
    val result = from(table)(cfg =>
      where(cfg.eventType === eventType)
        select ((cfg.severity, cfg.designation, cfg.alarmState, cfg.resource))).toList

    // If the EventType exists in the config database, return it;
    // otherwise we have to assume it's an alarm.
    result.headOption getOrElse (1, EventConfig.Designation.ALARM.getNumber, Alarm.State.UNACK_SILENT.getNumber, "")
  }

  override def updateModelEntry(context: RequestContext, proto: EventConfig, existing: EventConfigStore): EventConfigStore = {
    createModelEntry(context, proto, existing.builtIn)
  }

  override def createModelEntry(context: RequestContext, proto: EventConfig): EventConfigStore = createModelEntry(context, proto, false)

  def createModelEntry(context: RequestContext, proto: EventConfig, builtIn: Boolean): EventConfigStore = {
    // If it's not an alarm, set the state to 0.
    val state = proto.getDesignation match {
      case EventConfig.Designation.ALARM => proto.getAlarmState.getNumber
      case _ => -1
    }

    EventConfigStore(proto.getEventType,
      proto.getSeverity,
      proto.getDesignation.getNumber,
      state,
      proto.getResource,
      builtIn)
  }

  override def delete(context: RequestContext, entry: EventConfigStore) = {
    if (entry.builtIn) {
      val originalConfiguration = EventConfigService.builtInEventConfigurations.get(entry.eventType)
      super.delete(context, entry)
      create(context, originalConfiguration.get)
    } else {
      super.delete(context, entry)
    }
  }
}

trait EventConfigConversion
    extends UniqueAndSearchQueryable[EventConfig, EventConfigStore] {

  val table = ApplicationSchema.eventConfigs

  def sortResults(list: List[EventConfig]) = list.sortBy(_.getEventType)

  def getRoutingKey(req: EventConfig) = ProtoRoutingKeys.generateRoutingKey {
    req.eventType :: Nil
  }

  def relatedEntities(entries: List[EventConfigStore]) = {
    Nil
  }

  override def selector(map: VisibilityMap, sql: EventConfigStore) = (true === true)

  def searchQuery(proto: EventConfig, sql: EventConfigStore) = {
    proto.builtIn.asParam(sql.builtIn === _) :: Nil
  }

  def uniqueQuery(proto: EventConfig, sql: EventConfigStore) = {
    proto.eventType.asParam(sql.eventType === _) :: Nil
  }

  def isModified(entry: EventConfigStore, existing: EventConfigStore): Boolean = {
    entry.alarmState != existing.alarmState ||
      entry.designation != existing.designation ||
      entry.resource != existing.resource ||
      entry.severity != existing.severity
  }

  def convertToProto(entry: EventConfigStore): EventConfig = {
    val ec = EventConfig.newBuilder
      .setEventType(entry.eventType)
      .setSeverity(entry.severity)
      .setDesignation(EventConfig.Designation.valueOf(entry.designation))
      .setResource(entry.resource)
      .setBuiltIn(entry.builtIn)

    if (entry.alarmState != -1)
      ec.setAlarmState(Alarm.State.valueOf(entry.alarmState))

    ec.build
  }
}
