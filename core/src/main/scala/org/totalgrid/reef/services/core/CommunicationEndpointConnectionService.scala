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

import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.models.{ ApplicationSchema, FrontEndAssignment, CommunicationEndpoint, ApplicationInstance, MeasProcAssignment }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors
import ServiceBehaviors._
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinatorFactory }
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }
import org.totalgrid.reef.event.{ SystemEventSink, EventType }

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class CommunicationEndpointConnectionService(protected val modelTrans: ServiceTransactable[CommunicationEndpointConnectionServiceModel])
    extends SyncModeledServiceBase[ConnProto, FrontEndAssignment, CommunicationEndpointConnectionServiceModel]
    with GetEnabled
    with PutCreatesOrUpdates
    with DeleteEnabled
    with PostPartialUpdate
    with SubscribeEnabled {

  override val descriptor = Descriptors.commEndpointConnection

  override def merge(context: RequestContext[_], req: ServiceType, current: ModelType): ServiceType = {
    import org.totalgrid.reef.proto.OptionalProtos._

    val builder = CommunicationEndpointConnectionConversion.convertToProto(current).toBuilder
    req.state.foreach { builder.setState(_) }
    req.enabled.foreach { builder.setEnabled(_) }
    builder.build
  }
}

class CommunicationEndpointConnectionModelFactory(
  dependencies: ServiceDependencies,
  coordinatorFac: MeasurementStreamCoordinatorFactory)
    extends BasicModelFactory[ConnProto, CommunicationEndpointConnectionServiceModel](dependencies, classOf[ConnProto]) {

  def model = {
    val csm = new CommunicationEndpointConnectionServiceModel(subHandler, dependencies.eventSink)
    csm.setCoordinator(coordinatorFac.model)
    csm
  }
}

import org.totalgrid.reef.services.coordinators._
class CommunicationEndpointConnectionServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  val eventSink: SystemEventSink)
    extends SquerylServiceModel[ConnProto, FrontEndAssignment]
    with EventedServiceModel[ConnProto, FrontEndAssignment]
    with CommunicationEndpointConnectionConversion
    with ServiceModelSystemEventPublisher {

  var coordinator: MeasurementStreamCoordinator = null
  def setCoordinator(cr: MeasurementStreamCoordinator, linkModels: Boolean = true) = {
    coordinator = cr
    if (linkModels) link(coordinator)
  }

  override def updateFromProto(context: RequestContext[_], proto: ConnProto, existing: FrontEndAssignment): (FrontEndAssignment, Boolean) = {

    lazy val endpoint = existing.endpoint.value.get
    lazy val eventArgs = "name" -> endpoint.entityName :: Nil
    lazy val eventFunc = postSystemEvent(_: String, args = eventArgs, entity = Some(endpoint.entity.value))

    // changing enabled flag has precedence, then connection state changes
    val currentlyEnabled = existing.enabled
    if (proto.hasEnabled && proto.getEnabled != currentlyEnabled) {

      val code = if (currentlyEnabled) EventType.Scada.CommEndpointDisabled else EventType.Scada.CommEndpointEnabled
      eventFunc(code)

      update(context, existing.copy(enabled = proto.getEnabled), existing)
    } else if (proto.hasState && proto.getState.getNumber != existing.state) {
      val newState = proto.getState.getNumber
      val online = newState == ConnProto.State.COMMS_UP.getNumber
      val updated = if (online) {
        eventFunc(EventType.Scada.CommEndpointOnline)
        existing.copy(onlineTime = Some(System.currentTimeMillis), state = newState)
      } else {
        eventFunc(EventType.Scada.CommEndpointOffline)
        existing.copy(offlineTime = Some(System.currentTimeMillis), onlineTime = None, state = newState)
      }
      update(context, updated, existing)
    } else {
      // state and enabled weren't altered, return NOT_MODIFIED
      (existing, false)
    }
  }

  override def postUpdate(context: RequestContext[_], sql: FrontEndAssignment, existing: FrontEndAssignment) {
    coordinator.onFepConnectionChange(context, sql, existing)
  }
}

object CommunicationEndpointConnectionConversion extends CommunicationEndpointConnectionConversion

trait CommunicationEndpointConnectionConversion
    extends MessageModelConversion[ConnProto, FrontEndAssignment]
    with UniqueAndSearchQueryable[ConnProto, FrontEndAssignment] {

  val table = ApplicationSchema.frontEndAssignments

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.frontEnd.uuid.uuid :: req.uid :: Nil
  }

  def searchQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    proto.frontEnd.appConfig.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(app, { _.id })) ::
      proto.state.asParam(sql.state === _.getNumber) ::
      proto.enabled.asParam(sql.enabled === _) ::
      Nil
  }

  def uniqueQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    proto.uid.asParam(sql.id === _.toLong) ::
      proto.endpoint.map(endpoint => sql.endpointId in CommEndCfgServiceConversion.uniqueQueryForId(endpoint, { _.id })) ::
      Nil
  }

  def isModified(entry: FrontEndAssignment, existing: FrontEndAssignment): Boolean = {
    entry.applicationId != existing.applicationId ||
      entry.serviceRoutingKey != existing.serviceRoutingKey ||
      entry.state != existing.state ||
      entry.assignedTime != existing.assignedTime ||
      entry.offlineTime != existing.offlineTime ||
      entry.onlineTime != existing.onlineTime ||
      entry.enabled != existing.enabled
  }

  def createModelEntry(proto: ConnProto): FrontEndAssignment = {
    throw new Exception("bad interface")
  }

  def convertToProto(entry: FrontEndAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setUid(makeUid(entry))

    entry.application.value.foreach(app => b.setFrontEnd(FrontEndProcessor.newBuilder.setUuid(makeUuid(app)).setAppConfig(ApplicationConfig.newBuilder.setInstanceName(app.instanceName))))
    entry.endpoint.value.foreach(endpoint => b.setEndpoint(makeSparseEndpointProto(endpoint)))
    entry.serviceRoutingKey.foreach(k => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(k)))
    b.setState(ConnProto.State.valueOf(entry.state))
    b.setEnabled(entry.enabled)

    // get the most recent change
    val times = entry.onlineTime :: entry.offlineTime :: entry.assignedTime :: Nil
    b.setLastUpdate(times.flatten.max)

    b.build
  }

  // we add some interesting data about the endpoint in the connection proto but we dont want to
  // include the list of all points/commands in these communication related protos
  private def makeSparseEndpointProto(endpoint: CommunicationEndpoint) = {
    val b = CommEndpointConfig.newBuilder
      .setUuid(makeUuid(endpoint.entity.value))
      .setName(endpoint.entity.value.name)
      .setProtocol(endpoint.protocol)

    endpoint.port.value.foreach(p => b.setChannel(FrontEndPortConversion.convertToProto(p)))

    b
  }
}
