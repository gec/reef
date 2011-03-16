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

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.models.HeartbeatStatus
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.ProcessStatus._

import org.totalgrid.reef.models.{ ApplicationInstance, ApplicationSchema }

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.api.BadRequestException
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors

// Implicits
import org.totalgrid.reef.proto.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._

class ProcessStatusService(protected val modelTrans: ServiceTransactable[ProcessStatusServiceModel])
    extends BasicSyncModeledService[StatusSnapshot, HeartbeatStatus, ProcessStatusServiceModel] {

  override val descriptor = Descriptors.statusSnapshot
}

class ProcessStatusServiceModelFactory(
  pub: ServiceEventPublishers,
  measProcFac: ModelFactory[MeasurementProcessingConnectionServiceModel],
  fepModelFac: ModelFactory[CommunicationEndpointConnectionServiceModel])
    extends BasicModelFactory[StatusSnapshot, ProcessStatusServiceModel](pub, classOf[StatusSnapshot]) {

  def model = new ProcessStatusServiceModel(subHandler, measProcFac.model, fepModelFac.model)
}

class ProcessStatusServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  measProcModel: MeasurementProcessingConnectionServiceModel,
  fepModel: CommunicationEndpointConnectionServiceModel)
    extends SquerylServiceModel[StatusSnapshot, HeartbeatStatus]
    with EventedServiceModel[StatusSnapshot, HeartbeatStatus]
    with ProcessStatusConversion
    with Logging {

  link(measProcModel)
  link(fepModel)

  def addApplication(app: ApplicationInstance, periodMS: Int, processId: String, capabilities: List[String], now: Long = System.currentTimeMillis) {

    // give the app twice as long to come online
    val firstCheck = now + periodMS * 2

    val hbSql = new HeartbeatStatus(app.id, periodMS, firstCheck, true, processId)

    val existing = table.where(h => h.applicationId === app.id)

    val ret = if (existing.size == 1) {
      info("App " + hbSql.instanceName.value + ": is being marked back online at " + now + " id: " + processId)
      update(hbSql, existing.head)
    } else {
      info("App " + hbSql.instanceName.value + ": is added and marked online at " + now + " id: " + processId)
      create(hbSql)
    }

    notifyModels(app, true, capabilities)

    ret
  }

  def takeApplicationOffline(hbeat: HeartbeatStatus, now: Long) {

    debug("App " + hbeat.instanceName + ": is being marked offline at " + now)
    val ret = update(new HeartbeatStatus(hbeat.applicationId, hbeat.periodMS, now, false, hbeat.processId), hbeat)

    notifyModels(hbeat.application.value, false, hbeat.application.value.capabilities.value.toList.map { _.capability })

    ret
  }

  def notifyModels(app: ApplicationInstance, online: Boolean, capabilities: List[String]) {
    capabilities.foreach(_ match {
      case "Processing" => measProcModel.onAppChanged(app, online)
      case "FEP" => fepModel.onAppChanged(app, online)
      case _ =>
    })
  }
}

trait ProcessStatusConversion
    extends MessageModelConversion[StatusSnapshot, HeartbeatStatus]
    with UniqueAndSearchQueryable[StatusSnapshot, HeartbeatStatus] {

  val table = ApplicationSchema.heartbeats

  def getRoutingKey(req: StatusSnapshot) = ProtoRoutingKeys.generateRoutingKey {
    req.instanceName :: Nil
  }

  def searchQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    Nil
  }

  def uniqueQuery(proto: StatusSnapshot, sql: HeartbeatStatus) = {
    proto.uid.asParam(sql.id === _.toLong) ::
      proto.instanceName.map { inst =>
        val nameProto = ApplicationConfig.newBuilder.setInstanceName(inst).build // TODO: Make this better; shouldn't have to make a proto to use interface
        sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(nameProto, { _.id })
      } :: Nil
  }

  def isModified(entry: HeartbeatStatus, existing: HeartbeatStatus): Boolean = {
    entry.isOnline != existing.isOnline || entry.processId != existing.processId
  }

  def createModelEntry(proto: StatusSnapshot): HeartbeatStatus = {
    throw new BadRequestException("can't put heartbeat configuations")
  }

  def convertToProto(entry: HeartbeatStatus): StatusSnapshot = {
    StatusSnapshot.newBuilder
      .setUid(entry.id.toString)
      .setInstanceName(entry.instanceName.value)
      .setOnline(entry.isOnline)
      .setTime(entry.timeoutAt)
      .build
  }
}

/*
class ProcessStatusModel(subHandler: ServiceSubscriptionHandler)
  extends BasicServiceModel[StatusSnapshot, HeartbeatStatus](subHandler, ApplicationSchema.heartbeats) with Logging {

  def isProtoValidModel(proto: StatusSnapshot): Boolean = {
    return true
  }

  def getRoutingKey(req: StatusSnapshot) = ProtoRoutingKeys.routingKey(req)
  def deserialize(bytes: Array[Byte]) = StatusSnapshot.parseFrom(bytes)

  def toSql(proto: StatusSnapshot): HeartbeatStatus = {
    throw new ReefReefServiceException("can't put heartbeat configuations")
  }

  def toProto(sql: HeartbeatStatus): StatusSnapshot = {
    val b = StatusSnapshot.newBuilder
    val app = sql.application
    b.setUid(sql.id.toString)
    b.setInstanceName(app.instanceName)

    b.setOnline(sql.isOnline).setTime(sql.timeoutAt)

    b.build
  }

  override def findRecords(req: StatusSnapshot): List[HeartbeatStatus] = {
    req.hasInstanceName ? req.getInstanceName match {
      case Some("*") => table.where(t => true === true).toList
      case Some(name) =>
        from(table)(h =>
          where(h.applicationId in from(ApplicationSchema.apps)(a =>
            where(a.instanceName === name)
              select (&(a.id))))
            select (h)).toList
      case None => Nil
    }
  }

  def addApplication(appId: Long, periodMS: Int, deadmanSwitch: String, now: Long = System.currentTimeMillis) {

    // give the app twice as long to come online
    val firstCheck = now + periodMS * 2

    val hbSql = new HeartbeatStatus(appId, periodMS, firstCheck, true, deadmanSwitch)

    info("App " + hbSql.application.instanceName + ": is being marked online at " + now)

    val existing = table.where(h => h.applicationId === appId)
    if (existing.size == 1) {
      hbSql.id = existing.head.id
      updateEntry(hbSql)
    } else {
      insertEntry(hbSql)
    }
  }

  def takeApplicationOffline(hbeat: HeartbeatStatus, now: Long) {
    info("App " + hbeat.application.instanceName + ": is being marked offline at " + now)
    hbeat.isOnline = false
    hbeat.timeoutAt = now

    updateEntry(hbeat)
  }
}*/ 