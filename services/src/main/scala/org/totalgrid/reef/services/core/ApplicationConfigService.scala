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

import org.totalgrid.reef.models.{ ApplicationInstance, ApplicationSchema, ApplicationCapability }
import org.totalgrid.reef.client.service.proto.Application._
import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.exception.BadRequestException

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.client.service.proto.OptionalProtos._

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.models.UUIDConversions._
import org.totalgrid.reef.client.sapi.types.Optional._

import scala.collection.JavaConversions._

class ApplicationConfigService(val model: ApplicationConfigServiceModel)
    extends SyncModeledServiceBase[ApplicationConfig, ApplicationInstance, ApplicationConfigServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.applicationConfig
}

class ApplicationConfigServiceModel(procStatusModel: ProcessStatusServiceModel)
    extends SquerylServiceModel[Long, ApplicationConfig, ApplicationInstance]
    with EventedServiceModel[ApplicationConfig, ApplicationInstance]
    with ApplicationConfigConversion {

  val entityModel = new EntityServiceModel

  private def createModelEntry(context: RequestContext, proto: ApplicationConfig, userName: String): ApplicationInstance = {
    val ent = entityModel.findOrCreate(context, proto.getInstanceName, "Application" :: Nil, None) //EntityQuery.findOrCreateEntity(proto.getInstanceName, "Application" :: Nil, None)
    val a = new ApplicationInstance(ent.id, proto.getInstanceName, userName, proto.getLocation, proto.getNetwork)
    a.entity.value = ent
    a
  }

  override def createFromProto(context: RequestContext, req: ApplicationConfig): ApplicationInstance = {
    val sql = create(context, createModelEntry(context: RequestContext, req, context.getHeaders.userName.get))

    val caps = req.getCapabilitesList.toList
    ApplicationSchema.capabilities.insert(caps.map { x => new ApplicationCapability(sql.id, x) })

    // TODO: make heartbeating a capability
    val time = if (req.hasHeartbeatCfg) req.getHeartbeatCfg.getPeriodMs else 60000
    procStatusModel.addApplication(context, sql, time, req.getProcessId, caps)

    sql
  }

  override def updateFromProto(context: RequestContext, req: ApplicationConfig, existing: ApplicationInstance): (ApplicationInstance, Boolean) = {

    val username = context.getHeaders.userName.getOrElse(throw new BadRequestException("No username in headers"))
    val (sql, updated) = update(context, createModelEntry(context, req, username), existing)

    val newCaps: List[String] = req.getCapabilitesList.toList
    val oldCaps: List[String] = sql.capabilities.value.toList.map { _.capability }

    val addedCaps = newCaps.diff(oldCaps)
    val removedCaps = oldCaps.diff(newCaps)

    ApplicationSchema.capabilities.insert(addedCaps.map { x => new ApplicationCapability(sql.id, x) })
    procStatusModel.notifyModels(context, sql, false, removedCaps)
    ApplicationSchema.capabilities.deleteWhere(c => c.applicationId === sql.id and (c.capability in removedCaps.map { _.id }))

    val time = if (req.hasHeartbeatCfg) req.getHeartbeatCfg.getPeriodMs else 60000
    procStatusModel.addApplication(context, sql, time, req.getProcessId, newCaps)

    (sql, updated)
  }

  override def preDelete(context: RequestContext, sql: ApplicationInstance) {
    procStatusModel.delete(context, sql.heartbeat.value)
  }

  override def postDelete(context: RequestContext, sql: ApplicationInstance) {
    ApplicationSchema.capabilities.deleteWhere(c => c.applicationId === sql.id)
    entityModel.delete(context, sql.entity.value)
  }
}

trait ApplicationConfigConversion
    extends UniqueAndSearchQueryable[ApplicationConfig, ApplicationInstance] {

  val table = ApplicationSchema.apps

  def sortResults(list: List[ApplicationConfig]) = list.sortBy(_.getInstanceName)

  def getRoutingKey(proto: ApplicationConfig) = ProtoRoutingKeys.generateRoutingKey {
    proto.uuid.value :: proto.instanceName :: Nil
  }

  def searchQuery(proto: ApplicationConfig, sql: ApplicationInstance) = {
    List(proto.userName.asParam(sql.userName === _),
      proto.network.asParam(sql.network === _),
      proto.location.asParam(sql.location === _))
  }

  def uniqueQuery(proto: ApplicationConfig, sql: ApplicationInstance) = {
    val eSearch = EntitySearch(proto.uuid.value, proto.instanceName, proto.instanceName.map(x => List("Application")))
    List(eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })))
  }

  def isModified(entry: ApplicationInstance, existing: ApplicationInstance): Boolean = {
    entry.location != existing.location || entry.network != existing.network
  }

  def convertToProto(entry: ApplicationInstance): ApplicationConfig = {

    val hbeat = entry.heartbeat.value

    val h = HeartbeatConfig.newBuilder
      .setPeriodMs(hbeat.periodMS)
      .setProcessId(hbeat.processId)
      .setInstanceName(entry.instanceName)

    val b = ApplicationConfig.newBuilder
      .setUuid(makeUuid(entry))
      .setUserName(entry.userName)
      .setInstanceName(entry.instanceName)
      .setNetwork(entry.network)
      .setLocation(entry.location)
      .setHeartbeatCfg(h)
      .setOnline(hbeat.isOnline)
      .setTimesOutAt(hbeat.timeoutAt)
      .setProcessId(hbeat.processId)

    entry.capabilities.value.foreach(x => b.addCapabilites(x.capability))
    b.build
  }
}

object ApplicationConfigConversion extends ApplicationConfigConversion

