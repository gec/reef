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

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.service.proto.ProcessStatus._

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinator }
import java.util.UUID
import org.squeryl.Query
import org.totalgrid.reef.models.{ OverrideConfig, HeartbeatStatus, ApplicationInstance, ApplicationSchema }
import org.totalgrid.reef.authz.VisibilityMap

// Implicits
import org.totalgrid.reef.client.service.proto.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import org.totalgrid.reef.services.framework.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._

class ProcessStatusService(val model: ProcessStatusServiceModel, useServerTime: Boolean = true)
    extends SyncModeledServiceBase[StatusSnapshot, HeartbeatStatus, ProcessStatusServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.statusSnapshot

  override protected def preUpdate(context: RequestContext, request: StatusSnapshot, existing: HeartbeatStatus) = {
    // ignore any time sent to us, use our own clock for consistency
    if (useServerTime) {
      request.toBuilder.setTime(System.currentTimeMillis()).build
    } else {
      // in tests we want to accept whatever time is sent to us
      request
    }
  }
}

class ProcessStatusServiceModel(
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[Long, StatusSnapshot, HeartbeatStatus]
    with EventedServiceModel[StatusSnapshot, HeartbeatStatus]
    with ProcessStatusConversion
    with Logging {

  def createFromProto(context: RequestContext, req: StatusSnapshot) =
    throw new BadRequestException("Cannot create heartbeat status, register application first. Instance: "
      + req.getInstanceName + " Process: " + req.getProcessId + " unknown.")

  override def updateFromProto(context: RequestContext, ss: StatusSnapshot, hbeat: HeartbeatStatus): (HeartbeatStatus, Boolean) = {
    if (hbeat.isOnline) {
      if (ss.getOnline) {
        logger.info("Got heartbeat for: " + ss.getInstanceName + ": " + ss.getProcessId + " by " + (hbeat.timeoutAt - ss.getTime))
        hbeat.timeoutAt = ss.getTime + hbeat.periodMS * 2
        // don't publish a modify
        try {
          ApplicationSchema.heartbeats.update(hbeat)
        } catch {
          // if we have deleted the application we need to fail any remaining heartbeats
          case ex: RuntimeException => throw new BadRequestException("Application deleted")
        }
        (hbeat, true)
      } else {
        logger.info("App " + hbeat.instanceName.value + ": is shutting down at " + ss.getTime)
        handleOfflineHeartbeat(context, hbeat, ss.getTime)
      }
    } else {
      if (ss.getOnline) throw new BadRequestException("App " + ss.getInstanceName + ": is marked offline but got message. Application should be restarted and reregistered.")
      else (hbeat, false)
    }
  }

  def addApplication(context: RequestContext, app: ApplicationInstance, periodMS: Int, inputId: String, capabilities: List[String], now: Long = System.currentTimeMillis) {

    val processId = if (inputId == null || inputId.length() < 5) {
      logger.info("Invalid processId " + inputId + " sent during registration, creating new random id")
      UUID.randomUUID().toString
    } else inputId

    // give the app twice as long to come online
    val firstCheck = now + periodMS * 2

    val hbSql = new HeartbeatStatus(app.id, periodMS, firstCheck, true, processId)

    val existing = table.where(h => h.applicationId === app.id)

    val ret = if (existing.size == 1) {
      logger.info("App " + hbSql.instanceName.value + ": is being marked back online at " + now + " id: " + processId)
      update(context, hbSql, existing.head)
    } else {
      logger.info("App " + hbSql.instanceName.value + ": is added and marked online at " + now + " id: " + processId)
      create(context, hbSql)
    }

    notifyModels(context, app, true, capabilities)

    ret
  }

  def handleOfflineHeartbeat(context: RequestContext, hbeat: HeartbeatStatus, now: Long = System.currentTimeMillis) = {

    val ret = update(context, new HeartbeatStatus(hbeat.applicationId, hbeat.periodMS, now, false, hbeat.processId), hbeat)

    takeApplicationOffline(context, hbeat.application.value)

    ret
  }

  def takeApplicationOffline(context: RequestContext, app: ApplicationInstance) {
    logger.debug("App " + app.instanceName + ": is being marked offline")
    notifyModels(context, app, false, app.capabilities.value)
  }

  def notifyModels(context: RequestContext, app: ApplicationInstance, online: Boolean, capabilities: List[String]) {
    capabilities.foreach(_ match {
      case "Processing" => coordinator.onMeasProcAppChanged(context, app, online)
      case "FEP" => coordinator.onFepAppChanged(context, app, online)
      case _ =>
    })
  }
}

trait ProcessStatusConversion
    extends UniqueAndSearchQueryable[StatusSnapshot, HeartbeatStatus] {

  val table = ApplicationSchema.heartbeats

  def sortResults(list: List[StatusSnapshot]) = list.sortBy(_.getInstanceName)

  def getRoutingKey(req: StatusSnapshot) = ProtoRoutingKeys.generateRoutingKey {
    req.instanceName :: Nil
  }

  def relatedEntities(models: List[HeartbeatStatus]) = {
    models.map { _.application.value.agent.value.entityId }
  }

  private def resourceId = Descriptors.statusSnapshot.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: HeartbeatStatus) = {
    sql.id in from(table, ApplicationSchema.apps, ApplicationSchema.agents)((hbeat, app, agent) =>
      where(
        (hbeat.applicationId === app.id) and
          (app.agentId === agent.id) and
          (agent.entityId in entitySelector))
        select (hbeat.id))
  }

  override def selector(map: VisibilityMap, sql: HeartbeatStatus) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }

  def searchQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    Nil
  }

  def uniqueQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    List[Option[SearchTerm]](
      proto.processId.asParam(sql.processId === _).unique,
      proto.instanceName.map { inst =>
        // TODO: Make this better; shouldn't have to make a proto to use interface
        val nameProto = ApplicationConfig.newBuilder.setInstanceName(inst).build
        sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(nameProto, { _.id })
      })
  }

  def isModified(entry: HeartbeatStatus, existing: HeartbeatStatus): Boolean = {
    entry.isOnline != existing.isOnline || entry.processId != existing.processId
  }

  def convertToProto(entry: HeartbeatStatus): StatusSnapshot = {
    StatusSnapshot.newBuilder
      .setProcessId(entry.processId)
      .setInstanceName(entry.instanceName.value)
      .setOnline(entry.isOnline)
      .setTime(entry.timeoutAt)
      .build
  }
}
