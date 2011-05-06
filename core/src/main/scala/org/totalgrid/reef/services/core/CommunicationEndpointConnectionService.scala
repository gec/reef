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

import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.models.{ ApplicationSchema, FrontEndAssignment, CommunicationEndpoint, ApplicationInstance, MeasProcAssignment }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.ProtoRoutingKeys

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.BadRequestException
import ServiceBehaviors._
import org.totalgrid.reef.proto.Application.ApplicationConfig

import org.totalgrid.reef.api.service.SyncServiceBase

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

import org.totalgrid.reef.measurementstore.MeasurementStore

class CommunicationEndpointConnectionService(protected val modelTrans: ServiceTransactable[CommunicationEndpointConnectionServiceModel])
    extends SyncModeledServiceBase[ConnProto, FrontEndAssignment, CommunicationEndpointConnectionServiceModel]
    with GetEnabled
    with PutEnabled
    with DeleteEnabled
    with PostPartialUpdate
    with SubscribeEnabled {

  override val descriptor = Descriptors.commEndpointConnection

  override def merge(req: ServiceType, current: ModelType): ServiceType = {
    import org.totalgrid.reef.proto.OptionalProtos._

    val builder = CommunicationEndpointConnectionConversion.convertToProto(current).toBuilder
    req.state.foreach { builder.setState(_) }
    builder.build
  }
}

class CommunicationEndpointConnectionModelFactory(pub: ServiceEventPublishers, measurementStore: MeasurementStore)
    extends BasicModelFactory[ConnProto, CommunicationEndpointConnectionServiceModel](pub, classOf[ConnProto]) {

  def model = new CommunicationEndpointConnectionServiceModel(subHandler, measurementStore)
}

import org.totalgrid.reef.services.coordinators._
class CommunicationEndpointConnectionServiceModel(protected val subHandler: ServiceSubscriptionHandler, val measurementStore: MeasurementStore)
    extends SquerylServiceModel[ConnProto, FrontEndAssignment]
    with EventedServiceModel[ConnProto, FrontEndAssignment]
    with CommunicationEndpointConnectionConversion
    with CommunicationEndpointOfflineBehaviors
    with MeasurementCoordinationQueries {

  def onEndpointCreated(ce: CommunicationEndpoint) {

    val now = System.currentTimeMillis
    markOffline(ce)

    // get routing key if measProc is assigned and ready
    val measProcAssignment = ce.measProcAssignment.value
    val serviceRoutingKey = measProcAssignment.readyTime.map { l => measProcAssignment.serviceRoutingKey.get }

    val applicationId = getFep(ce).map { _.id }
    val assignedTime = applicationId.map { x => now }

    create(new FrontEndAssignment(ce.id, ConnProto.State.COMMS_DOWN.getNumber, serviceRoutingKey, applicationId, assignedTime, Some(now), None))
  }

  def onEndpointUpdated(ce: CommunicationEndpoint) {
    val assign = table.where(fep => fep.endpointId === ce.id).single
    delete(assign) // TODO: if the fep is watching for endpoint changes we shouldn't have to do this
    onEndpointCreated(ce)
  }

  def onEndpointDeleted(ce: CommunicationEndpoint) {
    val assign = table.where(fep => fep.endpointId === ce.id).single
    if (assign.onlineTime.isDefined) {
      val newAssign = assign.copy(serviceRoutingKey = None)
      update(newAssign, assign)
    } else {
      delete(assign)
    }
  }

  def onMeasProcAssignmentChanged(meas: MeasProcAssignment, added: Boolean) {
    info { "MeasProc Change: added: " + added + " rechecking: " + meas.endpoint.value.get.name.value + " readyTime: " + meas.readyTime + " key:" + meas.serviceRoutingKey }
    table.where(fep => fep.endpointId === meas.endpointId).headOption.foreach { assign =>

      if (added && meas.readyTime.isDefined && meas.serviceRoutingKey.isDefined) {
        val newAssign = assign.copy(serviceRoutingKey = meas.serviceRoutingKey)
        update(newAssign, assign)
        checkAssignment(newAssign, assign.endpoint.value.get)
      } else {
        markOffline(assign.endpoint.value.get)
        val newAssign = assign.copy(offlineTime = Some(System.currentTimeMillis), onlineTime = None, serviceRoutingKey = None)
        update(newAssign, assign)
      }

    }
  }

  def onAppChanged(app: ApplicationInstance, added: Boolean) {

    val rechecks = if (added) {
      table.where(fep => fep.applicationId.isNull).toList
    } else {
      table.where(fep => fep.applicationId === app.id).toList
    }
    info { "FEP: " + app.instanceName + " added: " + added + " rechecking: " + rechecks.map { _.endpoint.value.get.name.value } }
    rechecks.foreach { a => checkAssignment(a, a.endpoint.value.get) }
  }

  private def checkAssignment(assign: FrontEndAssignment, ce: CommunicationEndpoint) {
    val applicationId = getFep(ce).map { _.id }

    info { ce.name.value + " assigned FEP: " + applicationId + " protocol: " + ce.protocol + " port: " + ce.port.value + " routingKey: " + assign.serviceRoutingKey }

    val assignedTime = applicationId.map { x => System.currentTimeMillis }
    if (assign.applicationId != applicationId) {
      val now = System.currentTimeMillis
      markOffline(ce)
      val newAssign = assign.copy(applicationId = applicationId, assignedTime = assignedTime, offlineTime = Some(now), onlineTime = None, state = ConnProto.State.COMMS_DOWN.getNumber)
      update(newAssign, assign)
    }
  }

  override def updateFromProto(proto: ConnProto, existing: FrontEndAssignment): (FrontEndAssignment, Boolean) = {
    if (existing.application.value.isEmpty)
      throw new BadRequestException("No application assigned")

    val endpoint = existing.endpoint.value.get

    val newState = proto.getState.getNumber
    val online = proto.getState == ConnProto.State.COMMS_UP

    if (newState == existing.state)
      throw new BadRequestException("Allready has state: " + proto.getState)

    if (online) {
      markOnline(endpoint)
      val updated = existing.copy(onlineTime = Some(System.currentTimeMillis), state = newState)
      update(updated, existing)
    } else {
      if (existing.offlineTime == None) markOffline(endpoint)
      val updated = existing.copy(offlineTime = Some(System.currentTimeMillis), onlineTime = None, state = newState)
      update(updated, existing)
    }

  }
}

object CommunicationEndpointConnectionConversion extends CommunicationEndpointConnectionConversion

trait CommunicationEndpointConnectionConversion
    extends MessageModelConversion[ConnProto, FrontEndAssignment]
    with UniqueAndSearchQueryable[ConnProto, FrontEndAssignment] {

  val table = ApplicationSchema.frontEndAssignments

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.frontEnd.uid ::
      req.uid ::
      Nil
  }

  def searchQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    Nil
  }

  def uniqueQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    proto.uid.asParam(sql.id === _.toLong) ::
      proto.frontEnd.appConfig.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(app, { _.id })) ::
      Nil
  }

  def isModified(entry: FrontEndAssignment, existing: FrontEndAssignment): Boolean = {
    entry.applicationId != existing.applicationId || entry.serviceRoutingKey != existing.serviceRoutingKey || entry.state != existing.state
  }

  def createModelEntry(proto: ConnProto): FrontEndAssignment = {
    throw new Exception("bad interface")
  }

  def convertToProto(entry: FrontEndAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setUid(entry.id.toString)

    entry.applicationId.foreach(appId => b.setFrontEnd(FrontEndProcessor.newBuilder.setUid(appId.toString).setAppConfig(ApplicationConfig.newBuilder.setInstanceName(entry.application.value.get.instanceName))))
    entry.endpoint.value.foreach(endpoint => b.setEndpoint(CommEndpointConfig.newBuilder.setUid(endpoint.entity.value.id.toString).setName(endpoint.entity.value.name)))
    entry.serviceRoutingKey.foreach(k => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(k)))
    b.setState(ConnProto.State.valueOf(entry.state))

    // get the most recent change
    val times = entry.onlineTime :: entry.offlineTime :: entry.assignedTime :: Nil
    b.setLastUpdate(times.flatten.max)

    b.build
  }
}
