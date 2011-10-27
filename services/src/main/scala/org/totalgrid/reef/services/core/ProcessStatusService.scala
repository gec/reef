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

import org.totalgrid.reef.models.HeartbeatStatus
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.ProcessStatus._

import org.totalgrid.reef.models.{ ApplicationInstance, ApplicationSchema }

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.exceptions.BadRequestException

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinator }
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }

// Implicits
import org.totalgrid.reef.proto.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.clientapi.sapi.types.Optional._
import org.totalgrid.reef.services.framework.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._

class ProcessStatusService(val model: ProcessStatusServiceModel)
    extends SyncModeledServiceBase[StatusSnapshot, HeartbeatStatus, ProcessStatusServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.statusSnapshot
}

class ProcessStatusServiceModel(
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[StatusSnapshot, HeartbeatStatus]
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
        ApplicationSchema.heartbeats.update(hbeat)
        (hbeat, true)
      } else {
        logger.info("App " + hbeat.instanceName.value + ": is shutting down at " + ss.getTime)
        takeApplicationOffline(context, hbeat, ss.getTime)
      }
    } else {
      throw new BadRequestException("App " + ss.getInstanceName + ": is marked offline but got message!")
    }
  }

  def addApplication(context: RequestContext, app: ApplicationInstance, periodMS: Int, processId: String, capabilities: List[String], now: Long = System.currentTimeMillis) {

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

  def takeApplicationOffline(context: RequestContext, hbeat: HeartbeatStatus, now: Long) = {

    logger.debug("App " + hbeat.instanceName + ": is being marked offline at " + now)
    val ret = update(context, new HeartbeatStatus(hbeat.applicationId, hbeat.periodMS, now, false, hbeat.processId), hbeat)

    notifyModels(context, hbeat.application.value, false, hbeat.application.value.capabilities.value.toList.map { _.capability })

    ret
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

  def getRoutingKey(req: StatusSnapshot) = ProtoRoutingKeys.generateRoutingKey {
    req.instanceName :: Nil
  }

  def searchQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    Nil
  }

  def uniqueQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    proto.processId.asParam(sql.processId === _) ::
      proto.instanceName.map { inst =>
        // TODO: Make this better; shouldn't have to make a proto to use interface
        val nameProto = ApplicationConfig.newBuilder.setInstanceName(inst).build
        sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(nameProto, { _.id })
      } :: Nil
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
