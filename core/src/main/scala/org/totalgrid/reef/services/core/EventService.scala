/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.models.{ ApplicationSchema, EventStore, AlarmModel, EventConfigStore, Entity }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.proto.Utils.AttributeList
import org.squeryl.dsl.QueryYield
import org.squeryl.dsl.ast.OrderByArg
import org.squeryl.dsl.fsm.{ SelectState }

//import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.core.util.MessageFormatter
import org.totalgrid.reef.services.ProtoRoutingKeys

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.api.{ Envelope, BadRequestException }
import org.totalgrid.reef.api.service.SyncServiceBase

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import ServiceBehaviors._

class EventService(protected val modelTrans: ServiceTransactable[EventServiceModel])
    extends SyncModeledServiceBase[Event, EventStore, EventServiceModel]
    with GetEnabled
    with SubscribeEnabled
    with PutOnlyCreates
    with DeleteEnabled {

  override val descriptor = Descriptors.event
}

class EventServiceModelFactory(pub: ServiceEventPublishers, eventConfig: ModelFactory[EventConfigServiceModel], alarmServiceModel: ModelFactory[AlarmServiceModel])
    extends BasicModelFactory[Event, EventServiceModel](pub, classOf[Event]) {

  def model = new EventServiceModel(subHandler, eventConfig.model, alarmServiceModel.model)
}

// The business model for managing incoming events and deciding whether
// they are Alarms, Events, or Logs.
// This will use the ConfigService to determine what is an Alarm, Event, or Log.
//
class EventServiceModel(protected val subHandler: ServiceSubscriptionHandler, eventConfig: EventConfigServiceModel, alarmServiceModel: AlarmServiceModel)
    extends SquerylServiceModel[Event, EventStore]
    with EventedServiceModel[Event, EventStore]
    with EventConversion {

  // linking means the bus notifications generated in the alarm service will be
  // sent at the same time as the notifications from this service.
  link(alarmServiceModel)

  // TODO: figure out better way to get these functions in here without renaming
  override def getEventProtoAndKey(event: EventStore) = makeEventProtoAndKey(event)
  override def getSubscribeKeys(req: Event): List[String] = makeSubscribeKeys(req)

  /**
   * A raw Event comes in and we need to process it
   * into a real Event, Alarm, or Log.
   */
  override def createFromProto(req: Event): EventStore = {
    import org.totalgrid.reef.event.EventType

    if (!req.hasEventType) { throw new BadRequestException("Unknown EventType: '" + req.getEventType + "'", Envelope.Status.BAD_REQUEST) }

    val (severity, designation, alarmState, resource) = eventConfig.getProperties(req.getEventType)

    // if the raw event had the entity filled out try to find that entity
    val entity = req.entity.map(EQ.findEntity(_)).getOrElse(None)

    designation match {
      case EventConfigStore.ALARM =>
        // Create event and alarm instances. Post them to the bus.
        val event = create(createModelEntry(req, true, severity, entity, resource)) // true: is an alarm
        val alarm = new AlarmModel(alarmState, event.id)
        alarm.event.value = event // so we don't lookup the event again
        alarmServiceModel.create(alarm)
        log(req, event, entity)
        event

      case EventConfigStore.EVENT =>
        // Create the event instance and post it to the bus.
        val event = create(createModelEntry(req, false, severity, entity, resource)) // false: not an alarm
        log(req, event, entity)
        event

      case EventConfigStore.LOG =>
        // Instead of returning a "Message", return an EventStore without a UID. Not great, but it will do for now.
        val event = createModelEntry(req, false, severity, entity, resource)
        log(req, event, entity)
        event

      case _ =>
        throw new BadRequestException("Unknown designation (i.e. ALARM, EVENT, LOG): '" + designation + "' for EventType: '" + req.getEventType + "'", Envelope.Status.INTERNAL_ERROR)
    }
  }

  def log(req: Event, event: EventStore, entity: Option[Entity]): Unit = {
    def entityToString(entity: Option[Entity]) = entity match {
      case Some(e) => e.name
      case None => "-"
    }

    def eventToList(req: Event, entity: Option[Entity]) =
      event.severity :: event.eventType :: entityToString(entity) :: event.userId :: event.rendered :: Nil

    info(eventToList(req, entity).mkString(" "))

  }

}

trait EventConversion
    extends MessageModelConversion[Event, EventStore]
    with UniqueAndSearchQueryable[Event, EventStore] {

  val table = ApplicationSchema.events

  override def getOrdering[R](select: SelectState[R], sql: EventStore): QueryYield[R] = select.orderBy(new OrderByArg(sql.time).asc)

  // Derive a AMQP routing key from a proto. Used by post?
  def getRoutingKey(req: Event) = ProtoRoutingKeys.generateRoutingKey {
    req.eventType ::
      req.severity ::
      req.subsystem ::
      req.userId ::
      req.entity.uid ::
      Nil
  }

  def getRoutingKey(req: Event, entity: Entity) = {
    // add a prefix for the "equipment" and "equipmentgroup" types
    val prefix = if (entity.types.value.contains("EquipmentGroup")) "group." else { if (entity.types.value.contains("Equipment")) "equipment." else "" }
    prefix + ProtoRoutingKeys.generateRoutingKey {
      req.eventType ::
        req.severity ::
        req.subsystem ::
        req.userId ::
        Some(entity.id) ::
        Nil
    }
  }

  def makeEventProtoAndKey(event: EventStore) = {
    val proto = convertToProto(event)
    var simpleKey = getRoutingKey(proto) :: Nil

    // publish the simple key last
    val keys = event.groups.value.map { x => getRoutingKey(proto, x) } :::
      event.equipments.value.map { x => getRoutingKey(proto, x) } ::: simpleKey

    (proto, keys)
  }

  def makeSubscribeKeys(req: Event): List[String] = {

    // just get top level entities from query, skip subscribe on descendents
    val entities = req.entity.map { EQ.protoTreeQuery(_).map { _.ent } }.getOrElse(Nil)

    val keys = if (entities.size > 0) entities.map(getRoutingKey(req, _))
    else getRoutingKey(req) :: Nil

    keys
  }

  // Derive a SQL expression from the proto. Used by GET. 
  def searchQuery(proto: Event, sql: EventStore) = {
    proto.eventType.asParam(sql.eventType === _) ::
      proto.severity.asParam(sql.severity === _) ::
      proto.subsystem.asParam(sql.subsystem === _) ::
      proto.userId.asParam(sql.userId === _) ::
      proto.entity.map(ent => sql.entityId in EQ.idsFromProtoQuery(ent)) ::
      Nil
  }

  def uniqueQuery(proto: Event, sql: EventStore) = {
    proto.uid.asParam(sql.id === _.toLong) :: Nil // if exists, use it.
  }

  def isModified(entry: EventStore, existing: EventStore): Boolean = {
    true
  }

  def createModelEntry(proto: Event): EventStore = {
    // TODO: get rid of this method from the message conversion interface
    throw new Exception("wrong interface")
  }

  def createModelEntry(proto: Event, isAlarm: Boolean, severity: Int, entity: Option[Entity], resource: String): EventStore = {

    var rendered = ""
    val args = if (proto.hasArgs) {
      val alist = proto.getArgs
      rendered = MessageFormatter.format(resource, alist)
      alist.toByteArray
    } else {
      Array[Byte]()
    }

    val es = EventStore(proto.getEventType,
      isAlarm,
      proto.getTime,
      proto.getDeviceTime,
      severity,
      proto.getSubsystem,
      proto.getUserId,
      entity.map { _.id },
      args,
      rendered)

    es.entity.value = entity
    es
  }

  def convertToProto(entry: EventStore): Event = {
    val b = Event.newBuilder
      .setUid(entry.id.toString)
      .setAlarm(entry.alarm)
      .setEventType(entry.eventType)
      .setTime(entry.time)
      .setDeviceTime(entry.deviceTime)
      .setSeverity(entry.severity)
      .setSubsystem(entry.subsystem)
      .setUserId(entry.userId)
      .setRendered(entry.rendered)
    entry.entity.value // force it to try to load the related entity for now
    entry.entity.asOption.foreach(_.foreach(x => b.setEntity(EQ.entityToProto(x).build)))
    if (entry.args.length > 0) {
      b.setArgs(AttributeList.parseFrom(entry.args))
    }
    //TODO: could set rendered here!

    b.build
  }
}

// Needed to construct an event proto from AlarmConversion
object EventConversion extends EventConversion
