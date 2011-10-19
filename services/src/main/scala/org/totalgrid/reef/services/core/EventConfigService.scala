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

import org.totalgrid.reef.api.proto.Alarms._
import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.api.sapi.impl.Descriptors
import org.totalgrid.reef.api.sapi.impl.OptionalProtos._
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.api.sapi.Optional._

object EventConfigService {
  def seed() {
    import org.squeryl.PrimitiveTypeMode._
    import org.squeryl.Table
    import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }
    import org.totalgrid.reef.event.EventType._
    import org.totalgrid.reef.api.proto.Alarms._

    inTransaction {
      if (ApplicationSchema.eventConfigs.Count.head == 0) {
        val ALARM = EventConfig.Designation.ALARM.getNumber
        val UNACK_SILENT = Alarm.State.UNACK_SILENT.getNumber

        def makeBuiltIn(name: String, resource: String) = {
          EventConfigStore(name, 8, ALARM, UNACK_SILENT, resource, true)
        }

        // TODO: make a default event config for handling unknown events

        val ecs = List[EventConfigStore](
          makeBuiltIn(System.UserLogin, "User logged in"),
          makeBuiltIn(System.UserLoginFailure, "User login failed {reason}"),
          makeBuiltIn(System.UserLogout, "User logged out"),
          makeBuiltIn(System.SubsystemStarting, "Subsystem is starting"),
          makeBuiltIn(System.SubsystemStarted, "Subsystem has started"),
          makeBuiltIn(System.SubsystemStopping, "Subsystem is stopping"),
          makeBuiltIn(System.SubsystemStopped, "Subsystem has stopped"),

          makeBuiltIn(Scada.ControlExe, "Executed control {command}"),
          makeBuiltIn(Scada.UpdatedSetpoint, "Updated setpoint {command} to {value}"),
          makeBuiltIn(Scada.OutOfNominal, "Measurement not in nominal range: {value}"),
          makeBuiltIn(Scada.OutOfReasonable, "Measurement not reasonable: {value}"),
          makeBuiltIn(Scada.SetOverride, "Point overridden"),
          makeBuiltIn(Scada.SetNotInService, "Point removed from service"),
          makeBuiltIn(Scada.RemoveOverride, "Removed override on point"),
          makeBuiltIn(Scada.RemoveNotInService, "Returned point to service"),
          // comm endpoint events
          makeBuiltIn(Scada.CommEndpointOffline, "Endpoint {name} offline"),
          makeBuiltIn(Scada.CommEndpointOnline, "Endpoint {name} online"),
          makeBuiltIn(Scada.CommEndpointDisabled, "Endpoint {name} disabled"),
          makeBuiltIn(Scada.CommEndpointEnabled, "Endpoint {name} enabled"))

        ecs.foreach(ApplicationSchema.eventConfigs.insert(_))
      }
    }
  }
}

class EventConfigService(protected val model: EventConfigServiceModel)
    extends SyncModeledServiceBase[EventConfig, EventConfigStore, EventConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.eventConfig

  override def preCreate(context: RequestContext, proto: EventConfig): EventConfig = {
    if (!proto.hasDesignation || !proto.hasEventType || !proto.hasSeverity || !proto.hasResource) {
      throw new BadRequestException("Must fill in designation, eventType, severity and resource fields.")
    }
    if (proto.hasBuiltIn && proto.getBuiltIn) {
      throw new BadRequestException("Cannot create an event configuration with \"builtIn\" flag set.")
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

  override protected def preUpdate(context: RequestContext, request: EventConfig, existing: EventConfigStore): EventConfig = {
    preCreate(context, request)
    // TODO: should we re-render all events with the same event type?
  }
}

class EventConfigServiceModel
    extends SquerylServiceModel[EventConfig, EventConfigStore]
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

  override def updateModelEntry(proto: EventConfig, existing: EventConfigStore): EventConfigStore = {
    createModelEntry(proto, existing.builtIn)
  }

  override def preDelete(context: RequestContext, entry: EventConfigStore) {
    if (entry.builtIn)
      throw new BadRequestException("Cannot delete \"builtIn\" event configurations, only update destination and message: " + entry.eventType)
  }
}

trait EventConfigConversion
    extends UniqueAndSearchQueryable[EventConfig, EventConfigStore] {

  val table = ApplicationSchema.eventConfigs

  def getRoutingKey(req: EventConfig) = ProtoRoutingKeys.generateRoutingKey {
    req.eventType :: Nil
  }

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

  def createModelEntry(proto: EventConfig): EventConfigStore = createModelEntry(proto, false)
  def createModelEntry(proto: EventConfig, builtIn: Boolean): EventConfigStore = {
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
