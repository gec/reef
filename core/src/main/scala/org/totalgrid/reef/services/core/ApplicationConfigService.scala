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

import org.totalgrid.reef.services.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.models.{ ApplicationInstance, ApplicationSchema, ApplicationCapability }

import org.totalgrid.reef.proto.Application._
import org.totalgrid.reef.proto.ProcessStatus._
import org.totalgrid.reef.proto.Envelope

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.services.ProtoRoutingKeys

import org.totalgrid.reef.messaging.ProtoSerializer._

import org.squeryl.PrimitiveTypeMode._
import OptionalProtos._ // implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

import scala.collection.JavaConversions._

class ApplicationConfigService(protected val modelTrans: ServiceTransactable[ApplicationConfigServiceModel])
    extends BasicProtoService[ApplicationConfig, ApplicationInstance, ApplicationConfigServiceModel] {

  def deserialize(bytes: Array[Byte]) = ApplicationConfig.parseFrom(bytes)
  val servedProto: Class[_] = classOf[ApplicationConfig]
}

class ApplicationConfigServiceModelFactory(pub: ServiceEventPublishers, procStatus: ModelFactory[ProcessStatusServiceModel])
    extends BasicModelFactory[ApplicationConfig, ApplicationConfigServiceModel](pub, classOf[ApplicationConfig]) {

  def model = new ApplicationConfigServiceModel(subHandler, procStatus.model)
  def model(procStatusModel: ProcessStatusServiceModel) = new ApplicationConfigServiceModel(subHandler, procStatusModel)
}

class ApplicationConfigServiceModel(protected val subHandler: ServiceSubscriptionHandler, procStatusModel: ProcessStatusServiceModel)
    extends SquerylServiceModel[ApplicationConfig, ApplicationInstance]
    with EventedServiceModel[ApplicationConfig, ApplicationInstance]
    with ApplicationConfigConversion {

  link(procStatusModel)

  override def createFromProto(req: ApplicationConfig): ApplicationInstance = {
    val sql = create(createModelEntry(req))

    val caps = req.getCapabilitesList.toList
    ApplicationSchema.capabilities.insert(caps.map { x => new ApplicationCapability(sql.id, x) })

    val time = if (req.hasHeartbeatCfg) req.getHeartbeatCfg.getPeriodMs else 60000
    procStatusModel.addApplication(sql, time, req.getProcessId, caps)

    sql
  }

  override def updateFromProto(req: ApplicationConfig, existing: ApplicationInstance): (ApplicationInstance, Boolean) = {
    val (sql, updated) = update(createModelEntry(req), existing)

    val newCaps: List[String] = req.getCapabilitesList.toList
    val oldCaps: List[String] = sql.capabilities.value.toList.map { _.capability }

    val addedCaps = newCaps.diff(oldCaps)
    val removedCaps = oldCaps.diff(newCaps)

    ApplicationSchema.capabilities.insert(addedCaps.map { x => new ApplicationCapability(sql.id, x) })
    procStatusModel.notifyModels(sql, false, removedCaps)
    ApplicationSchema.capabilities.deleteWhere(c => c.applicationId === sql.id and (c.capability in removedCaps.map { _.id }))

    val time = if (req.hasHeartbeatCfg) req.getHeartbeatCfg.getPeriodMs else 60000
    procStatusModel.addApplication(sql, time, req.getProcessId, newCaps)

    (sql, updated)
  }

  override def postDelete(sql: ApplicationInstance) {
    ApplicationSchema.capabilities.deleteWhere(c => c.applicationId === sql.id)
  }
}

trait ApplicationConfigConversion
    extends MessageModelConversion[ApplicationConfig, ApplicationInstance]
    with UniqueAndSearchQueryable[ApplicationConfig, ApplicationInstance] {

  val table = ApplicationSchema.apps

  def getRoutingKey(proto: ApplicationConfig) = ProtoRoutingKeys.generateRoutingKey {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasInstanceName, proto.getInstanceName) :: Nil
  }

  def searchQuery(proto: ApplicationConfig, sql: ApplicationInstance) = {
    List(proto.userName.asParam(sql.userName === _),
      proto.network.asParam(sql.network === _),
      proto.location.asParam(sql.location === _))
  }

  def uniqueQuery(proto: ApplicationConfig, sql: ApplicationInstance) = {
    List(proto.uid.asParam(sql.id === _.toInt),
      proto.instanceName.asParam(sql.instanceName === _))
  }

  def isModified(entry: ApplicationInstance, existing: ApplicationInstance): Boolean = {
    entry.location != existing.location || entry.network != existing.network
  }

  def createModelEntry(proto: ApplicationConfig): ApplicationInstance = {
    new ApplicationInstance(
      proto.getInstanceName,
      proto.getUserName,
      proto.getLocation,
      proto.getNetwork)
  }

  def convertToProto(entry: ApplicationInstance): ApplicationConfig = {

    val hbeat = entry.heartbeat

    val h = HeartbeatConfig.newBuilder
      .setDest("proc_status")
      .setPeriodMs(hbeat.value.periodMS)
      .setUid(hbeat.value.processId)
      .setRoutingKey(entry.instanceName)
      .setInstanceName(entry.instanceName)

    val s = StreamServicesConfig.newBuilder
      .setLogsDest("raw_logs")
      .setEventsDest("raw_events")
      .setNonopDest("raw_meas")

    val b = ApplicationConfig.newBuilder
      .setUid(entry.id.toString)
      .setUserName(entry.userName)
      .setInstanceName(entry.instanceName)
      .setNetwork(entry.network)
      .setLocation(entry.location)
      .setHeartbeatCfg(h).setStreamCfg(s)

    entry.capabilities.value.foreach(x => b.addCapabilites(x.capability))
    b.build
  }
}

object ApplicationConfigConversion extends ApplicationConfigConversion

