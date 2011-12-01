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

import org.totalgrid.reef.proto.FEP.{ EndpointConnection => ConnProto }
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.models.{ ApplicationSchema, FrontEndAssignment, CommunicationEndpoint, ApplicationInstance, MeasProcAssignment }

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Descriptors
import ServiceBehaviors._
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.event.{ SystemEventSink, EventType }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess.ExclusiveAccessException

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._

class CommunicationEndpointConnectionService(protected val model: CommunicationEndpointConnectionServiceModel)
    extends SyncModeledServiceBase[ConnProto, FrontEndAssignment, CommunicationEndpointConnectionServiceModel]
    with GetEnabled
    with PutCreatesOrUpdates
    with DeleteEnabled
    with PostPartialUpdate
    with SubscribeEnabled {

  override val descriptor = Descriptors.commEndpointConnection

  // we will manually merge by checking to see what fields are set and using exclusive acccess blocks
  override def merge(context: RequestContext, req: ConnProto, current: FrontEndAssignment) = req
}

import org.totalgrid.reef.services.coordinators._
class CommunicationEndpointConnectionServiceModel
    extends SquerylServiceModel[ConnProto, FrontEndAssignment]
    with EventedServiceModel[ConnProto, FrontEndAssignment]
    with CommunicationEndpointConnectionConversion
    with ServiceModelSystemEventPublisher {

  var coordinator: MeasurementStreamCoordinator = null
  def setCoordinator(cr: MeasurementStreamCoordinator, linkModels: Boolean = true) = {
    coordinator = cr
  }

  def createFromProto(context: RequestContext, req: ConnProto): FrontEndAssignment =
    throw new BadRequestException("Cannot create frontend connections via the public interface")

  override def updateFromProto(context: RequestContext, proto: ConnProto, existing: FrontEndAssignment): (FrontEndAssignment, Boolean) = {
    try {
      attemptUpdate(context, proto, existing)
    } catch {
      case e: ExclusiveAccessException =>
        logger.warn("ExclusiveAcess collision during update, retrying")
        attemptUpdate(context, proto, table.lookup(existing.id).getOrElse(existing))
    }
  }

  private def attemptUpdate(context: RequestContext, proto: ConnProto, existing: FrontEndAssignment) = {

    lazy val endpoint = existing.endpoint.value.get
    lazy val eventArgs = "name" -> endpoint.entityName :: Nil
    lazy val eventFunc = postSystemEvent(context, _: String, args = eventArgs, entity = Some(endpoint.entity.value))

    // changing enabled flag has precedence, then connection state changes
    val currentlyEnabled = existing.enabled
    val currentState = existing.state

    def isSame(entry: FrontEndAssignment) = entry.enabled == currentlyEnabled && entry.state == currentState

    if (proto.hasEnabled && proto.getEnabled != currentlyEnabled) {

      exclusiveUpdate(context, existing, isSame _) { toBeUpdated =>
        val code = if (currentlyEnabled) EventType.Scada.CommEndpointDisabled else EventType.Scada.CommEndpointEnabled
        eventFunc(code)

        toBeUpdated.copy(enabled = proto.getEnabled)
      }
    } else if (proto.hasState && proto.getState.getNumber != currentState) {
      val newState = proto.getState.getNumber
      val online = newState == ConnProto.State.COMMS_UP.getNumber
      exclusiveUpdate(context, existing, isSame _) { toBeUpdated =>
        if (online) {
          eventFunc(EventType.Scada.CommEndpointOnline)
          toBeUpdated.copy(onlineTime = Some(System.currentTimeMillis), state = newState)
        } else {
          eventFunc(EventType.Scada.CommEndpointOffline)
          toBeUpdated.copy(offlineTime = Some(System.currentTimeMillis), onlineTime = None, state = newState)
        }
      }
    } else {
      // state and enabled weren't altered, return NOT_MODIFIED
      (existing, false)
    }
  }

  override def postUpdate(context: RequestContext, sql: FrontEndAssignment, existing: FrontEndAssignment) {
    logger.info("EndpointConnection UPDATED: " + sql.endpoint.value.map { _.entityName } + " id " + existing.id + " e: " + sql.enabled + " s: " + ConnProto.State.valueOf(sql.state) + " fep: " + sql.applicationId)
    coordinator.onFepConnectionChange(context, sql, existing)
  }
}

object CommunicationEndpointConnectionConversion extends CommunicationEndpointConnectionConversion

trait CommunicationEndpointConnectionConversion
    extends UniqueAndSearchQueryable[ConnProto, FrontEndAssignment] {

  val table = ApplicationSchema.frontEndAssignments

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.frontEnd.uuid.value :: req.id.value :: Nil
  }

  def searchQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    proto.frontEnd.appConfig.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(app, { _.id })) ::
      proto.state.asParam(sql.state === _.getNumber) ::
      proto.enabled.asParam(sql.enabled === _) ::
      Nil
  }

  def uniqueQuery(proto: ConnProto, sql: FrontEndAssignment) = {
    proto.id.value.asParam(sql.id === _.toLong) ::
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

  def convertToProto(entry: FrontEndAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setId(makeId(entry))

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
    val b = Endpoint.newBuilder
      .setUuid(makeUuid(endpoint.entity.value))
      .setName(endpoint.entity.value.name)
      .setProtocol(endpoint.protocol)

    endpoint.port.value.foreach(p => b.setChannel(FrontEndPortConversion.convertToProto(p)))

    b
  }
}
