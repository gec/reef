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

import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.ProtoRoutingKeys

import org.totalgrid.reef.proto.OptionalProtos._

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

object EventConfigService {
  def seed() {
    import org.squeryl.PrimitiveTypeMode._
    import org.squeryl.Table
    import org.totalgrid.reef.models.{ ApplicationSchema, EventConfigStore }
    import org.totalgrid.reef.event.EventType._
    import org.totalgrid.reef.proto.Alarms._

    inTransaction {
      if (ApplicationSchema.eventConfigs.Count.head == 0) {
        val ALARM = EventConfig.Designation.ALARM.getNumber
        val UNACK_SILENT = Alarm.State.UNACK_SILENT.getNumber

        val ecs = List[EventConfigStore](
          EventConfigStore(System.UserLogin, 8, ALARM, UNACK_SILENT, "User log in {status} {reason}"),
          EventConfigStore(System.UserLogout, 8, ALARM, UNACK_SILENT, "User logged out"),
          EventConfigStore(System.SubsystemStarting, 8, ALARM, UNACK_SILENT, "Subsystem is starting"),
          EventConfigStore(System.SubsystemStarted, 8, ALARM, UNACK_SILENT, "Subsystem has started"),
          EventConfigStore(System.SubsystemStopping, 8, ALARM, UNACK_SILENT, "Subsystem is stapping"),
          EventConfigStore(System.SubsystemStopped, 8, ALARM, UNACK_SILENT, "Subsystem has stopped"),

          EventConfigStore(Scada.ControlExe, 8, ALARM, UNACK_SILENT, "User executed control {control} on device {device}"),
          EventConfigStore(Scada.OutOfNominal, 8, ALARM, UNACK_SILENT, "Measurement not in nominal range: {value}{unit} validity {validity} "),
          EventConfigStore(Scada.OutOfReasonable, 8, ALARM, UNACK_SILENT, "Measurement not reasonable: {value}{unit} validity {validity}"))

        ecs.foreach(ApplicationSchema.eventConfigs.insert(_))
      }
    }
  }
}

class EventConfigService(protected val modelTrans: ServiceTransactable[EventConfigServiceModel])
    extends SyncModeledServiceBase[EventConfig, EventConfigStore, EventConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.eventConfig
}

class EventConfigServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[EventConfig, EventConfigServiceModel](pub, classOf[EventConfig]) {

  def model = new EventConfigServiceModel(subHandler)
}

class EventConfigServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[EventConfig, EventConfigStore]
    with EventedServiceModel[EventConfig, EventConfigStore]
    with EventConfigConversion {

  def getProperties(eventType: String) = {
    val result = from(table)(cfg =>
      where(cfg.eventType === eventType)
        select ((cfg.severity, cfg.designation, cfg.alarmState, cfg.resource))).toList

    // If the EventType exists in the config database, return it;
    // otherwise we have to assume it's an alarm.
    result.headOption getOrElse (1, EventConfig.Designation.ALARM.getNumber, Alarm.State.UNACK_SILENT.getNumber, "")
  }
}

trait EventConfigConversion
    extends MessageModelConversion[EventConfig, EventConfigStore]
    with UniqueAndSearchQueryable[EventConfig, EventConfigStore] {

  val table = ApplicationSchema.eventConfigs

  def getRoutingKey(req: EventConfig) = ProtoRoutingKeys.generateRoutingKey {
    req.eventType :: Nil
  }

  def searchQuery(proto: EventConfig, sql: EventConfigStore) = {
    Nil
  }

  def uniqueQuery(proto: EventConfig, sql: EventConfigStore) = {
    proto.eventType.asParam(sql.eventType === _) :: Nil
  }

  def isModified(entry: EventConfigStore, existing: EventConfigStore): Boolean = {
    true
  }

  def createModelEntry(proto: EventConfig): EventConfigStore = {
    // If it's not an alarm, set the state to 0.
    val state = proto.getDesignation match {
      case EventConfig.Designation.ALARM => proto.getAlarmState.getNumber
      case _ => 0
    }

    EventConfigStore(proto.getEventType,
      proto.getSeverity,
      proto.getDesignation.getNumber,
      state,
      proto.getResource)
  }

  def convertToProto(entry: EventConfigStore): EventConfig = {
    val ec = EventConfig.newBuilder
      .setEventType(entry.eventType)
      .setSeverity(entry.severity)
      .setDesignation(EventConfig.Designation.valueOf(entry.designation))
      .setResource(entry.resource)

    if (entry.alarmState > 0)
      ec.setAlarmState(Alarm.State.valueOf(entry.alarmState))

    ec.build
  }
}
