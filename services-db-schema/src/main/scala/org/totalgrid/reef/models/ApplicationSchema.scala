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

import org.squeryl.{ Schema, Query }
import org.squeryl.PrimitiveTypeMode._

object ApplicationSchema extends Schema {
  val entities = table[Entity]
  val edges = table[EntityEdge]
  val derivedEdges = table[EntityDerivedEdge]
  val entityTypes = table[EntityToTypeJoins]
  val entityTypeMetaModel = table[EntityTypeMetaModel]

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
  on(userRequests)(s => declare(s.errorMessage is dbType("TEXT")))
  val commandAccess = table[CommandLockModel]
  val commandToBlocks = table[CommandBlockJoin]

  val events = table[EventStore]
  on(events)(s => declare(
    s.time is (indexed),
    s.rendered is dbType("TEXT")))

  val eventConfigs = table[EventConfigStore]
  on(eventConfigs)(s => declare(s.resource is dbType("TEXT")))

  val overrides = table[OverrideConfig]

  val triggerSets = table[TriggerSet]

  val alarms = table[AlarmModel]
  on(alarms)(s => declare(
    s.eventId is (indexed)))

  val agents = table[Agent]
  val permissions = table[AuthPermission]
  val permissionSets = table[PermissionSet]
  val permissionSetJoins = table[PermissionSetJoin]
  val authTokens = table[AuthToken]

  //on(authTokens)(s => declare(s.token is (indexed), s.expirationTime is (indexed)))
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