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

import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.proto.Events.{ Event => EventProto }
import org.totalgrid.reef.models.{ ApplicationSchema, AlarmModel, EventStore, Entity }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table
import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.protoapi.ProtoServiceException

import org.totalgrid.reef.messaging.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.messaging.Descriptors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class AlarmService(protected val modelTrans: ServiceTransactable[AlarmServiceModel])
    extends BaseProtoService[Alarm, AlarmModel, AlarmServiceModel]
    with BaseProtoService.GetEnabled
    with BaseProtoService.SubscribeEnabled
    with BaseProtoService.PutPostEnabled
    with BaseProtoService.DeleteEnabled {

  override val descriptor = Descriptors.alarm

  // Alarms are created by events. No create via an Alarm proto.
  override def preCreate(req: Alarm) = {
    throw new ProtoServiceException("Create on alarms not allowed via this service.", Envelope.Status.NOT_ALLOWED)
  }

  // If they don't have a state, what are they doing with an update?
  override def preUpdate(proto: ProtoType, existing: ModelType) = {
    if (!proto.hasState)
      throw new ProtoServiceException("AlarmService update is for changing alarm state, but there is no state field in this proto.", Envelope.Status.NOT_ALLOWED)

    proto
  }
}

class AlarmServiceModelFactory(pub: ServiceEventPublishers, summary: SummaryPoints)
    extends BasicModelFactory[Alarm, AlarmServiceModel](pub, classOf[Alarm]) {

  def model = new AlarmServiceModel(subHandler, summary)

}

class AlarmServiceModel(protected val subHandler: ServiceSubscriptionHandler, summary: SummaryPoints)
    extends SquerylServiceModel[Alarm, AlarmModel]
    with EventedServiceModel[Alarm, AlarmModel]
    with AlarmConversion with AlarmSummaryCalculations {

  override def getEventProtoAndKey(alarm: AlarmModel) = {
    val (_, eventKeys) = EventConversion.makeEventProtoAndKey(alarm.event.value)
    val proto = convertToProto(alarm)
    val keys = eventKeys.map { ProtoRoutingKeys.generateRoutingKey(proto.uid :: Nil) + "." + _ }

    (proto, keys)
  }

  override def getSubscribeKeys(req: Alarm): List[String] = {
    val eventKeys = EventConversion.makeSubscribeKeys(req.event.getOrElse(EventProto.newBuilder.build))

    eventKeys.map { ProtoRoutingKeys.generateRoutingKey(req.uid :: Nil) + "." + _ }
  }

  // Update an Alarm. Currently, only the state can be updated.
  // Enforce valid state transitions.
  //
  override def updateFromProto(proto: Alarm, existing: AlarmModel): (AlarmModel, Boolean) = {

    if (existing.isNextStateValid(proto.getState.getNumber))
      update(updateModelEntry(proto, existing), existing)
    else {
      // TODO: access the proto to print the state names in the exception.
      throw new ProtoServiceException("Invalid state transistion from " + existing.state + " to " + proto.getState.getNumber, Envelope.Status.BAD_REQUEST)
    }
  }

  /// hooks to feed the populated models to the summary counter
  override def postCreate(created: AlarmModel): Unit = {
    super.postCreate(created)
    updateSummaries(created, None, summary.incrementSummary _)
  }
  override def postUpdate(updated: AlarmModel, original: AlarmModel): Unit = {
    super.postUpdate(updated, original)
    updateSummaries(updated, Some(original), summary.incrementSummary _)
  }
}

/**
 * keeps a running tally of unacked alarms in the system.
 */
trait AlarmSummaryCalculations {

  // TODO: get summary point names from configuration
  def severityName(n: Int) = "summary.unacked_alarms_severity_" + n
  def subsystemName(n: String) = "summary.unacked_alarms_subsystem_" + n
  def eqGroupName(n: String) = "summary.unacked_alarms_equipment_group_" + n

  def initializeSummaries(summary: SummaryPoints) {
    val severities = (for (i <- 1 to 3) yield i).toList
    val subsystems = List("FEP", "Processing")
    val eqGroups = EQ.findEntitiesByType("EquipmentGroup" :: Nil).toList.map { _.name }

    // TODO: get list of summary points from configuration somewhere
    val names = severities.map { severityName(_) } ::: subsystems.map { subsystemName(_) } ::: eqGroups.map { eqGroupName(_) }

    // look through all unacked alarms to regenerate counts
    val results = from(ApplicationSchema.alarms, ApplicationSchema.events)((alarm, event) =>
      where(alarm.state in List(AlarmModel.UNACK_AUDIBLE, AlarmModel.UNACK_SILENT) and
        alarm.eventUid === event.id)
        select (alarm, event))

    val alarms = AlarmQueries.populate(results.toList)

    // build a map with the intial values
    val m = scala.collection.mutable.Map.empty[String, Int]
    names.foreach(m(_) = 0) // start with 0
    alarms.foreach(updateSummaries(_, None, { (name, value) =>
      m.get(name) match {
        case Some(prev) => m(name) = prev + value
        case None => m(name) = value
      }
    }))

    // publish the initial values only once
    m.foreach(e => summary.setSummary(e._1, e._2))
  }

  def updateSummaries(alarm: AlarmModel, previous: Option[AlarmModel], func: (String, Int) => Any) {

    // if we are counting for the integrity its either +1 or 0, if updating an event its either
    // 0, -1, or 1 depending on state changes
    var incr = if (alarm.isUnacked && !(previous.isDefined && previous.get.isUnacked)) 1 else 0
    if (!alarm.isUnacked && previous.isDefined && previous.get.isUnacked) incr = -1

    if (incr == 0) return

    // TODO: publish by category instead of subsystem
    func(severityName(alarm.event.value.severity), incr)
    func(subsystemName(alarm.event.value.subsystem), incr)
    alarm.event.value.groups.value.foreach(ent => func(eqGroupName(ent.name), incr))
  }

}
object AlarmSummaryCalculations extends AlarmSummaryCalculations

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.services.ProtoServiceCoordinator

class AlarmSummaryInitializer(modelFac: AlarmServiceModelFactory, summary: SummaryPoints) extends ProtoServiceCoordinator with AlarmSummaryCalculations {

  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Reactable) {
    reactor.execute {
      modelFac.transaction { model =>
        initializeSummaries(summary)
      }
    }
  }
}

/**
 * Trait for coordinating between service message types and data model type
 */
trait AlarmConversion
    extends MessageModelConversion[Alarm, AlarmModel] with AlarmQueries {

  val table = ApplicationSchema.alarms

  def getRoutingKey(req: Alarm) = {
    // TODO: get rid of this method from the message conversion interface
    throw new Exception("wrong interface")
  }

  // Did the update change anything that requires a notification to bus
  // subscribers.
  //
  // We cannot update the event contained within this Alarm, just the state
  // of the Alarm.
  //
  def isModified(entry: AlarmModel, existing: AlarmModel): Boolean = {
    entry.state != existing.state
  }

  def createModelEntry(proto: Alarm): AlarmModel = {
    new AlarmModel(
      proto.getState.getNumber,
      proto.getEvent.getUid.toLong)
  }

  // Don't allow any updates except on the alarm state.
  override def updateModelEntry(proto: Alarm, existing: AlarmModel): AlarmModel = {
    new AlarmModel(
      proto.getState.getNumber,
      existing.eventUid) // Use the existing event so there's no possibility of an update.
  }

  def convertToProto(entry: AlarmModel): Alarm = {
    Alarm.newBuilder
      .setUid(entry.id.toString)
      .setState(Alarm.State.valueOf(entry.state))
      .setEvent(EventConversion.convertToProto(entry.event.value))
      .build
  }

  def convertToProto(entry: AlarmModel, event: EventStore): Alarm = {
    Alarm.newBuilder
      .setUid(entry.id.toString)
      .setState(Alarm.State.valueOf(entry.state))
      .setEvent(EventConversion.convertToProto(event))
      .build
  }
}
object AlarmConversion extends AlarmConversion

import org.squeryl.dsl.ast.{ LogicalBoolean, BinaryOperatorNodeLogicalBoolean }
import org.squeryl.dsl.ast.{ OrderByArg, ExpressionNode }
import org.squeryl.PrimitiveTypeMode._

trait AlarmQueries {

  def searchQuery(proto: Alarm, sql: AlarmModel): List[LogicalBoolean] = {

    // by default we dont return removed items to a get request
    val defaultStateList = List(AlarmModel.UNACK_AUDIBLE, AlarmModel.UNACK_SILENT, AlarmModel.ACKNOWLEDGED)
    val stateList = proto.state.map { _.getNumber :: Nil }.getOrElse(defaultStateList)

    (Some(sql.state in stateList) :: Nil).flatten
  }

  def uniqueQuery(proto: Alarm, sql: AlarmModel): List[LogicalBoolean] = {
    (proto.uid.asParam(sql.id === _.toLong) :: Nil).flatten // if exists, use it.
  }

  def searchEventQuery(event: EventStore, select: Option[EventProto]): LogicalBoolean = {
    select.map(EventConversion.searchParams(_, event)) getOrElse (true === true)
  }
  def uniqueEventQuery(event: EventStore, select: Option[EventProto]): LogicalBoolean = {
    select.map(EventConversion.uniqueParams(_, event)) getOrElse (true === true)
  }

  def findRecords(req: Alarm): List[AlarmModel] = {

    val query = from(ApplicationSchema.alarms, ApplicationSchema.events)((alarm, event) =>
      where(searchQuery(req, alarm) and
        searchEventQuery(event, req.event) and
        alarm.eventUid === event.id)
        select ((alarm, event))
        orderBy (new OrderByArg(event.time).desc)).page(0, 50)

    populate(query.toList)
  }

  def populate(results: List[(AlarmModel, EventStore)]): List[AlarmModel] = {
    // TODO: figure out why this totally hoses squeryl
    //    val results = from(ApplicationSchema.alarms, ApplicationSchema.events, ApplicationSchema.entities)((alarm, event, entity) =>
    //        where(buildQuery(req, alarm) and
    //          optionalEventQuery(event, req.event) and
    //          alarm.eventUid === event.id and event.entityId === entity.id)
    //          select ((alarm, event, entity))
    //          orderBy (new OrderByArg(event.time).desc)).page(0, 50)

    val entityIds = results.map { _._2.entityId }.flatten
    val entities = from(ApplicationSchema.entities)(entity => where(entity.id in entityIds) select (entity)).toList
    results.map { case (a, evt) => { if (evt.entityId.isDefined) { evt.entity.value = entities.find(_.id == evt.entityId.get) }; a.event.value = evt; a } }
  }

  def findRecord(req: Alarm): Option[AlarmModel] = {
    val query = from(ApplicationSchema.alarms, ApplicationSchema.events)((alarm, event) =>
      where(uniqueQuery(req, alarm) and
        uniqueEventQuery(event, req.event) and
        alarm.eventUid === event.id)
        select ((alarm, event))
        orderBy (new OrderByArg(event.time).desc)).page(0, 50)

    val uniqueItems = populate(query.toList)
    uniqueItems.size match {
      case 0 => None
      case 1 => Some(uniqueItems.head)
      case _ => throw new Exception("Unique query returned " + uniqueItems.size + " entries")
    }
  }
}
object AlarmQueries extends AlarmQueries
