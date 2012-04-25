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

import org.totalgrid.reef.client.service.proto.Events._

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.service.proto.Utils.{ AttributeList => AttributeListProto }
import org.squeryl.dsl.QueryYield
import org.squeryl.dsl.ast.OrderByArg
import org.squeryl.dsl.fsm.{ SelectState }
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException
import java.util.UUID
import org.squeryl.Query
import org.totalgrid.reef.authz.VisibilityMap
import org.totalgrid.reef.models._

//import org.totalgrid.reef.services.framework.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.UUIDConversions._

import org.totalgrid.reef.event.AttributeList
import org.totalgrid.reef.services.core.util.MessageFormatter
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import ServiceBehaviors._

class EventService(protected val model: EventServiceModel)
    extends SyncModeledServiceBase[Event, EventStore, EventServiceModel]
    with GetEnabled
    with SubscribeEnabled
    with PutOnlyCreates
    with DeleteEnabled {

  override val descriptor = Descriptors.event

  override def preCreate(context: RequestContext, proto: Event): Event = {
    val b = proto.toBuilder

    // we will clear any user given userId so we can apply auth token name
    b.clearUserId()
    b.setTime(System.currentTimeMillis)
    b.build
  }
}

// The business model for managing incoming events and deciding whether they are Alarms, Events, or Logs.
// This will use the ConfigService to determine what is an Alarm, Event, or Log.
class EventServiceModel(eventConfig: EventConfigServiceModel, alarmServiceModel: AlarmServiceModel)
    extends SquerylServiceModel[Long, Event, EventStore]
    with EventedServiceModel[Event, EventStore]
    with EventConversion {

  // linking means the bus notifications generated in the alarm service will be
  // sent at the same time as the notifications from this service.

  // functions are defined here to workaround traits with default values
  override def getEventProtoAndKey(event: EventStore) = makeEventProtoAndKey(event)
  override def getSubscribeKeys(req: Event): List[String] = makeSubscribeKeys(req)

  /**
   * A raw Event comes in and we need to process it into a real Event, Alarm, or Log.
   */
  def makeEvent(context: RequestContext, designation: Int, request: Event, severity: Int, entity: Option[Entity],
    renderedMessage: String, userId: String, alarmState: Int): (EventStore, Option[AlarmModel]) = {
    val (eventStore, alarm) = designation match {
      case EventConfigStore.ALARM =>
        // Create event and alarm instances. Post them to the bus.
        val event = create(context, createModelEntry(request, true, severity, entity, renderedMessage, userId)) // true: is an alarm
        val alarm = alarmServiceModel.createAlarmForEvent(context, event, alarmState)

        (event, Some(alarm))

      case EventConfigStore.EVENT =>
        // create an EventStore and store it in the database + publish
        (create(context, createModelEntry(request, false, severity, entity, renderedMessage, userId)), None)

      case EventConfigStore.LOG =>
        // Instead of returning a "Message", return an "unsaved" eventstore, the lack of UID can indicate
        // to the user that the message wasn't saved to the database
        (createModelEntry(request, false, severity, entity, renderedMessage, userId), None)

      case _ =>
        throw new BadRequestException("Unknown designation (i.e. ALARM, EVENT, LOG): '" + designation + "' for EventType: '" + request.getEventType +
          "'", Envelope.Status.INTERNAL_ERROR)
    }
    // we log all events regardless of designation
    log(eventStore)

    (eventStore, alarm)
  }

  def validateEventProto(request: Event) {
    if (!request.hasEventType) throw new BadRequestException("invalid event: " + request + ", EventType must be set")
    if (!request.hasTime) throw new BadRequestException("invalid event: " + request + ", Time must be set")
    if (!request.hasSubsystem) throw new BadRequestException("invalid event: " + request + ", Subsystem must be set.")
  }

  override def createFromProto(context: RequestContext, request: Event): EventStore = {

    validateEventProto(request)

    // in the case of the "thunked events" or "server generated events" we are not creating the event
    // in a standard request/response cycle so we dont have access to the username via the headers
    val userId = if (!request.hasUserId) {
      context.agent.entityName
    } else {
      request.getUserId
    }

    val (severity, designation, alarmState, resource) = eventConfig.getProperties(request.getEventType)
    val renderedMessage = renderEventMessage(request, resource)

    // if the raw event had the entity filled out try to find that entity
    val entity = request.entity.map(EntityQuery.findEntity(_)).getOrElse(None)

    val (eventStore, alarmOption) = makeEvent(context, designation, request, severity, entity, renderedMessage, userId, alarmState)
    eventStore
  }

  def log(event: EventStore) {
    val eventStringParts = "severity: " :: event.severity ::
      ", type: " :: event.eventType ::
      ", related: " :: event.entity.value.map { _.name }.getOrElse("_") ::
      ", user id: " :: event.userId ::
      ", rendered: " :: event.rendered :: Nil

    logger.info(eventStringParts.mkString(""))
  }
}

trait EventConversion
    extends UniqueAndSearchQueryable[Event, EventStore] {

  val table = ApplicationSchema.events

  override def getOrdering[R](select: SelectState[R], sql: EventStore): QueryYield[R] = select.orderBy(new OrderByArg(sql.time).asc)

  // we've already sorted with the getOrdering we needed to reterive from the database
  def sortResults(list: List[Event]) = list

  def relatedEntities(entries: List[EventStore]) = {
    entries.map { _.entityId }.flatten
  }

  private def resourceId = Descriptors.event.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: EventStore) = {
    sql.entityId in entitySelector
  }

  override def selector(map: VisibilityMap, sql: EventStore) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }

  // Derive a AMQP routing key from a proto. Used by post?
  def getRoutingKey(req: Event) = ProtoRoutingKeys.generateRoutingKey {
    req.eventType ::
      req.severity ::
      req.subsystem ::
      req.userId ::
      req.entity.uuid.value ::
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
    val entities = req.entity.map { EntityTreeQuery.protoTreeQuery(_).map { _.ent } }.getOrElse(Nil)

    val keys = if (entities.size > 0) entities.map(getRoutingKey(req, _))
    else getRoutingKey(req) :: Nil

    keys
  }

  // Derive a SQL expression from the proto. Used by GET. 
  override def searchQuery(context: RequestContext, proto: Event, sql: EventStore) = {
    List(
      proto.eventType.asParam(sql.eventType === _),
      proto.severity.asParam(sql.severity === _),
      proto.subsystem.asParam(sql.subsystem === _),
      proto.userId.asParam(sql.userId === _),
      proto.entity.map(ent => sql.entityId in EntityTreeQuery.idsFromProtoQuery(ent)))
  }

  override def uniqueQuery(context: RequestContext, proto: Event, sql: EventStore) = {
    List(
      proto.id.value.asParam(sql.id === _.toLong).unique)
  }

  def isModified(entry: EventStore, existing: EventStore): Boolean = {
    true
  }

  def renderEventMessage(proto: Event, resourceString: String) = {

    if (proto.hasRendered) {
      // if the user set the rendered string keep it as is
      proto.getRendered
    } else {
      val attributeList = proto.args.map { new AttributeList(_) }.getOrElse(new AttributeList)
      try {
        MessageFormatter.format(resourceString, attributeList)
      } catch {
        case x: Exception =>
          "Error rendering event string: " + resourceString + " with attributes: " + attributeList + " error: " + x
      }
    }
  }

  def createModelEntry(proto: Event, isAlarm: Boolean, severity: Int, entity: Option[Entity], message: String, userId: String): EventStore = {

    val args = proto.args.map { _.toByteArray }.getOrElse(Array[Byte]())

    val entityUuid = entity.map { _.id }

    val es = EventStore(proto.getEventType, isAlarm, proto.getTime, proto.deviceTime, severity,
      proto.getSubsystem, userId, entityUuid, args, message)

    es.entity.value = entity
    es
  }

  def convertToProto(entry: EventStore): Event = {
    val b = Event.newBuilder
      .setId(makeId(entry))
      .setAlarm(entry.alarm)
      .setEventType(entry.eventType)
      .setTime(entry.time)
      .setSeverity(entry.severity)
      .setSubsystem(entry.subsystem)
      .setUserId(entry.userId)
      .setRendered(entry.rendered)

    entry.deviceTime.foreach(b.setDeviceTime(_))
    entry.entity.value // force it to try to load the related entity for now
    entry.entity.asOption.foreach(_.foreach(x => b.setEntity(EntityQuery.entityToProto(x).build)))
    if (entry.args.length > 0) {
      b.setArgs(AttributeListProto.parseFrom(entry.args))
    }

    b.build
  }
}

// Needed to construct an event proto from AlarmConversion
object EventConversion extends EventConversion
