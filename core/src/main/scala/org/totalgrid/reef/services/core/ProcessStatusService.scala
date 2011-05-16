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
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinatorFactory, MeasurementStreamCoordinator }

// Implicits
import org.totalgrid.reef.proto.OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import org.totalgrid.reef.messaging.ProtoSerializer._
import org.squeryl.PrimitiveTypeMode._

class ProcessStatusService(protected val modelTrans: ServiceTransactable[ProcessStatusServiceModel])
    extends SyncModeledServiceBase[StatusSnapshot, HeartbeatStatus, ProcessStatusServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.statusSnapshot
}

class ProcessStatusServiceModelFactory(
  pub: ServiceEventPublishers,
  coordinatorFac: MeasurementStreamCoordinatorFactory)
    extends BasicModelFactory[StatusSnapshot, ProcessStatusServiceModel](pub, classOf[StatusSnapshot]) {

  def model = new ProcessStatusServiceModel(subHandler, coordinatorFac.model)
}

class ProcessStatusServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[StatusSnapshot, HeartbeatStatus]
    with EventedServiceModel[StatusSnapshot, HeartbeatStatus]
    with ProcessStatusConversion
    with Logging {

  link(coordinator)

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
      case "Processing" => coordinator.onMeasProcAppChanged(app, online)
      case "FEP" => coordinator.onFepAppChanged(app, online)
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
    proto.processId.asParam(sql.processId === _) ::
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
      .setProcessId(entry.processId)
      .setInstanceName(entry.instanceName.value)
      .setOnline(entry.isOnline)
      .setTime(entry.timeoutAt)
      .build
  }
}
