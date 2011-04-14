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
package org.totalgrid.reef.models

import org.squeryl.{ Schema, Table, KeyedEntity, Query }
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Transient

import org.totalgrid.reef.services.core.EQ
import org.totalgrid.reef.util.LazyVar

import org.totalgrid.reef.proto.Alarms._

import org.totalgrid.reef.proto.FEP.CommChannel

class ActiveModelException(msg: String) extends Exception(msg)

trait ActiveModel {
  def hasOne[A <: KeyedEntity[Long]](table: Table[A], id: Long): A = {
    table.lookup(id) match {
      case Some(s) => s
      case None =>
        throw new ActiveModelException("Missing id: " + id + " in " + table)
    }
  }

  def mayHaveOne[A <: KeyedEntity[Long]](table: Table[A], optId: Option[Long]): Option[A] = {
    optId match {
      case Some(-1) => None
      case Some(id) => Some(hasOne(table, id))
      case None => None
    }
  }

  def mayHaveOne[A](query: Query[A]): Option[A] = {
    query.toList match {
      case List(x) => Some(x)
      case _ => None
    }
  }

  def mayBelongTo[A](query: Query[A]): Option[A] = {

    query.size match {
      case 1 => Some(query.single)
      case _ => None
    }
  }

  def belongTo[A](query: Query[A]): A = {

    query.size match {
      case 1 => query.single
      case _ => throw new ActiveModelException("Missing belongTo relation")
    }
  }
}

trait ModelWithId extends KeyedEntity[Long] with ActiveModel {
  var id: Long = 0

}

case class ApplicationCapability(
    val applicationId: Long,
    val capability: String) extends ModelWithId {

  val application = LazyVar(hasOne(ApplicationSchema.apps, applicationId))
}

case class ApplicationInstance(
    val instanceName: String,
    val userName: String,
    var location: String,
    var network: String) extends ModelWithId {

  val heartbeat = LazyVar(belongTo(ApplicationSchema.heartbeats.where(p => p.applicationId === id)))

  val capabilities = LazyVar(ApplicationSchema.capabilities.where(p => p.applicationId === id))
}

class HeartbeatStatus(
    val applicationId: Long,
    val periodMS: Int,
    var timeoutAt: Long,
    var isOnline: Boolean,
    val processId: String) extends ModelWithId {

  val application = LazyVar(hasOne(ApplicationSchema.apps, applicationId))

  val instanceName = LazyVar(application.value.instanceName)
}

case class CommunicationProtocolApplicationInstance(
    val protocol: String,
    val applicationId: Long) extends ModelWithId {

  val application = LazyVar(hasOne(ApplicationSchema.apps, applicationId))
}

case class ChannelStatus(val name: String, val state: Int) extends ModelWithId

case class Point(
    val name: String,
    val entityId: Long,
    var abnormal: Boolean) extends ModelWithId {

  def this(n: String, entityId: Long) = this(n, entityId, false)

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))

  val logicalNode = LazyVar(mayHaveOne(EQ.getParentOfType(entityId, "source", "LogicalNode")))

  val sourceEdge = LazyVar(ApplicationSchema.edges.where(e => e.distance === 1 and e.childId === entityId and e.relationship === "source").headOption)

  /**
   * updated when the abnormal state is changed so we can "tunnel" this update through
   * to the service event.
   * The \@Transient attribute tells squeryl not to put this field in the database
   */
  @Transient
  var abnormalUpdated = false

  val endpoint = LazyVar(logicalNode.value.map(_.asType(ApplicationSchema.endpoints, "LogicalNode")))
}

case class Command(
    val name: String,
    val displayName: String,
    val entityId: Long,
    var connected: Boolean,
    var lastSelectId: Option[Long],
    var triggerId: Option[Long]) extends ModelWithId {

  def this() = this("", "", 0, false, Some(0), Some(0))
  def this(name: String, displayName: String, entityId: Long) = this(name, displayName, entityId, false, None, None)
  //def this(name: String, entityId: Long, connected: Boolean, lastSelectId: Option[Long], triggerId: Option[Long]) = this(name, name, entityId, false, None, None)
  def this(name: String, entityId: Long) = this(name, name, entityId, false, None, None)

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))

  val logicalNode = LazyVar(mayHaveOne(EQ.getParentOfType(entityId, "source", "LogicalNode")))

  val sourceEdge = LazyVar(ApplicationSchema.edges.where(e => e.distance === 1 and e.childId === entityId and e.relationship === "source").headOption)

  val endpoint = LazyVar(logicalNode.value.map(_.asType(ApplicationSchema.endpoints, "LogicalNode")))

}

case class FrontEndAssignment(
    val endpointId: Long,
    val state: Int,

    val serviceRoutingKey: Option[String],
    val applicationId: Option[Long],
    var assignedTime: Option[Long],
    var offlineTime: Option[Long],
    var onlineTime: Option[Long]) extends ModelWithId {

  def this() = this(0, 0, Some(""), Some(0), Some(0), Some(0), Some(0))

  val application = LazyVar(mayHaveOne(ApplicationSchema.apps, applicationId))
  val endpoint = LazyVar(ApplicationSchema.endpoints.where(p => p.id === endpointId).headOption)

}

case class MeasProcAssignment(
    val endpointId: Long,
    val serviceRoutingKey: Option[String],
    val applicationId: Option[Long],
    var assignedTime: Option[Long],
    var readyTime: Option[Long]) extends ModelWithId {

  def this() = this(0, Some(""), Some(0), Some(0), Some(0))

  val application = LazyVar(mayHaveOne(ApplicationSchema.apps, applicationId))
  val endpoint = LazyVar(ApplicationSchema.endpoints.where(p => p.id === endpointId).headOption)
}

case class FrontEndPort(
    val name: String,
    val network: Option[String],
    val location: Option[String],
    val state: Int,
    var proto: Array[Byte]) extends ModelWithId {

  def this() = this("", Some(""), Some(""), CommChannel.State.UNKNOWN.getNumber, Array.empty[Byte])
}

case class ConfigFile(
    val entityId: Long,
    val mimeType: String,
    var file: Array[Byte]) extends ModelWithId {

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))
  val owners = LazyVar(EQ.getParents(entity.value.id, "uses").toList)

  /// this flag allows us to tell if we have modified
  @Transient
  var changedOwners = false
}

case class CommunicationEndpoint(
    val entityId: Long,
    val protocol: String,
    var frontEndPortId: Option[Long]) extends ModelWithId {

  def this() = this(0, "", Some(0))
  def this(entityId: Long, protocol: String) = this(entityId, protocol, Some(0))

  val port = LazyVar(mayHaveOne(ApplicationSchema.frontEndPorts, frontEndPortId))
  val frontEndAssignment = LazyVar(ApplicationSchema.frontEndAssignments.where(p => p.endpointId === id).single)
  val measProcAssignment = LazyVar(ApplicationSchema.measProcAssignments.where(p => p.endpointId === id).single)

  val configFiles = LazyVar(Entity.asType(ApplicationSchema.configFiles, EQ.getChildrenOfType(entity.value.id, "uses", "ConfigurationFile").toList, Some("ConfigurationFile")))

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))
  val name = LazyVar(entity.value.name)

  val points = LazyVar(Entity.asType(ApplicationSchema.points, EQ.getChildrenOfType(entity.value.id, "source", "Point").toList, Some("Point")))
  val commands = LazyVar(Entity.asType(ApplicationSchema.commands, EQ.getChildrenOfType(entity.value.id, "source", "Command").toList, Some("Command")))
}

case class EventStore(
    eventType: String,
    alarm: Boolean,
    time: Long,
    deviceTime: Long,
    severity: Int,
    subsystem: String,
    userId: String,
    entityId: Option[Long],
    args: Array[Byte],
    rendered: String) extends ModelWithId {

  // extra constructor for squeryl type inference
  def this() = this("", false, 0, 0, 0, "", "", Some(0), Array[Byte](), "")

  val associatedAlarm = LazyVar(ApplicationSchema.alarms.where(a => a.eventUid === id).single)

  val entity = LazyVar(mayHaveOne(ApplicationSchema.entities, entityId))

  val groups = LazyVar(entityId.map { x => EQ.getParentOfType(x, "owns", "EquipmentGroup").toList }.getOrElse(Nil))
  val equipments = LazyVar(entityId.map { x => EQ.getParentOfType(x, "owns", "Equipment").toList }.getOrElse(Nil))
}

object EventConfigStore {
  import EventConfig.Designation

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
  val resource: String) extends ModelWithId // rendering string

case class TriggerConfig(
    val pointId: Long,
    val triggerName: String,
    var proto: Array[Byte]) extends ModelWithId {

  val point = LazyVar(hasOne(ApplicationSchema.points, pointId))
}

case class TriggerSet(
    val pointId: Long,
    var proto: Array[Byte]) extends ModelWithId {

  val point = LazyVar(hasOne(ApplicationSchema.points, pointId))
}

case class TransformConfig(
    val pointId: Long,
    var proto: Array[Byte]) extends ModelWithId {
  val point = LazyVar(hasOne(ApplicationSchema.points, pointId))
}

case class OverrideConfig(
    val pointId: Long,
    var proto: Array[Byte]) extends ModelWithId {
  val point = LazyVar(hasOne(ApplicationSchema.points, pointId))
}

/**
 * The Model for the Alarm. It's part DB map and part Model.
 */
object AlarmModel {
  import Alarm.State

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

class Entity(
    val name: String) extends ModelWithId {

  val types = LazyVar(from(ApplicationSchema.entityTypes)(t => where(id === t.entityId) select (&(t.entType))).toList)

  val attributes = LazyVar(from(ApplicationSchema.entityAttributes)(t => where(id === t.entityId) select (t)).toList)

  def asType[A <: { val entityId: Long }](table: Table[A], ofType: String) = {
    if (types.value.find(_ == ofType).isEmpty) {
      throw new Exception("entity: " + id + " didnt have type: " + ofType + " but had: " + types)
    }

    val l = from(table)(t => where(t.entityId === id) select (t)).toList
    if (l.size == 0) throw new Exception("Missing id: " + id + " table: " + table)
    l.head
  }

}
object Entity {

  def asType[A <: { val entityId: Long }](table: Table[A], entites: List[Entity], ofType: Option[String]) = {
    if (ofType.isDefined && !entites.forall(e => { e.types.value.find(_ == ofType.get).isDefined })) {
      throw new Exception("No all entities had type: " + ofType.get)
    }
    val ids = entites.map(_.id)
    if (ids.size > 0) from(table)(t => where(t.entityId in ids) select (t)).toList
    else Nil
  }
}

class EntityToTypeJoins(
  val entityId: Long,
  val entType: String) {}

class EntityEdge(
    val parentId: Long,
    val childId: Long,
    val relationship: String,
    val distance: Int) extends ModelWithId {

  val parent = LazyVar(hasOne(ApplicationSchema.entities, parentId))
  val child = LazyVar(hasOne(ApplicationSchema.entities, childId))
}
class EntityDerivedEdge(
    val edgeId: Long,
    val parentEdgeId: Long) extends ModelWithId {

  val edge = LazyVar(hasOne(ApplicationSchema.edges, edgeId))
  val parent = LazyVar(hasOne(ApplicationSchema.edges, parentEdgeId))
}

class EntityAttribute(
    val entityId: Long,
    val attrName: String,
    val stringVal: Option[String],
    val boolVal: Option[Boolean],
    val longVal: Option[Long],
    val doubleVal: Option[Double],
    val byteVal: Option[Array[Byte]]) extends ModelWithId {

  val entity = LazyVar(hasOne(ApplicationSchema.entities, entityId))

  def this() = this(5, "", Some(""), Some(true), Some(50L), Some(84.33), Some(Array.empty[Byte]))
}

object ApplicationSchema extends Schema {
  val entities = table[Entity]
  val edges = table[EntityEdge]
  val derivedEdges = table[EntityDerivedEdge]
  val entityTypes = table[EntityToTypeJoins]

  val entityAttributes = table[EntityAttribute]

  on(entities)(s => declare(
    //s.id is (indexed), // dont need index on primary keys
    s.name is (unique, indexed)))
  on(edges)(s => declare(
    columns(s.childId, s.relationship) are (indexed),
    columns(s.parentId, s.relationship) are (indexed)))
  on(entityTypes)(s => declare(
    s.entType is (indexed),
    s.entityId is (indexed)))

  val apps = table[ApplicationInstance]
  val capabilities = table[ApplicationCapability]
  val channelStatuses = table[ChannelStatus]
  val heartbeats = table[HeartbeatStatus]
  val protocols = table[CommunicationProtocolApplicationInstance]
  val points = table[Point]
  val commands = table[Command]

  val endpoints = table[CommunicationEndpoint]
  val frontEndPorts = table[FrontEndPort]
  val frontEndAssignments = table[FrontEndAssignment]
  val measProcAssignments = table[MeasProcAssignment]

  val configFiles = table[ConfigFile]

  val userRequests = table[UserCommandModel]
  val commandAccess = table[CommandAccessModel]
  val commandToBlocks = table[CommandBlockJoin]

  val events = table[EventStore]
  val eventConfigs = table[EventConfigStore]

  val triggers = table[TriggerConfig]
  val overrides = table[OverrideConfig]
  val transforms = table[TransformConfig]

  val triggerSets = table[TriggerSet]

  val alarms = table[AlarmModel]

  val agents = table[Agent]
  val permissions = table[AuthPermission]
  val permissionSets = table[PermissionSet]
  val permissionSetJoins = table[PermissionSetJoin]
  val authTokens = table[AuthToken]
  val tokenSetJoins = table[AuthTokenPermissionSetJoin]
  val agentSetJoins = table[AgentPermissionSetJoin]

  def reset() = {
    drop // its protected for some reason
    create
  }

  def idQuery[A <: ModelWithId](objQuery: Query[A]): Query[Long] = {
    from(objQuery)(o => select(o.id))
  }
}