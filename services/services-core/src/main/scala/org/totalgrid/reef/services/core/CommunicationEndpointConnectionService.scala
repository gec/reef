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

import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection => ConnProto }
import org.totalgrid.reef.client.service.proto.FEP._

import org.totalgrid.reef.services.framework._

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors
import ServiceBehaviors._
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.event.{ SystemEventSink, EventType }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.persistence.squeryl.ExclusiveAccess.ExclusiveAccessException
import org.squeryl.Query
import java.util.UUID
import org.totalgrid.reef.models._
import org.totalgrid.reef.authz.VisibilityMap
import org.squeryl.dsl.ast.LogicalBoolean

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import org.totalgrid.reef.models.UUIDConversions._

class CommunicationEndpointConnectionService(protected val model: CommunicationEndpointConnectionServiceModel)
    extends SyncModeledServiceBase[ConnProto, FrontEndAssignment, CommunicationEndpointConnectionServiceModel]
    with GetEnabled
    with PutCreatesOrUpdates
    with DeleteEnabled
    with PostPartialUpdate
    with SubscribeEnabled {

  override val descriptor = Descriptors.endpointConnection

  // we will manually merge by checking to see what fields are set and using exclusive acccess blocks
  override def merge(context: RequestContext, req: ConnProto, current: FrontEndAssignment) = req

  override protected def performUpdate(context: RequestContext, model: ServiceModelType, request: ServiceType, existing: ModelType): (ModelType, Boolean) = {
    model.updateFromProto(context, request, existing)
  }
}

import org.totalgrid.reef.services.coordinators._
class CommunicationEndpointConnectionServiceModel
    extends SquerylServiceModel[Long, ConnProto, FrontEndAssignment]
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
    val currentFep = existing.applicationId

    def loadFepId(fep: FrontEndProcessor): Option[Long] = FrontEndProcessorConversion.findRecord(context, fep).map { _.id }
    lazy val requestedFepId = loadFepId(proto.getFrontEnd)

    def isSame(entry: FrontEndAssignment) = entry.enabled == currentlyEnabled && entry.state == currentState && entry.applicationId == currentFep

    if (proto.hasEnabled && proto.getEnabled != currentlyEnabled) {
      context.auth.authorize(context, "endpoint_enabled", "update", List(endpoint.entityId))
      exclusiveUpdate(context, existing, isSame _) { toBeUpdated =>
        val code = if (currentlyEnabled) EventType.Scada.CommEndpointDisabled else EventType.Scada.CommEndpointEnabled
        eventFunc(code)

        toBeUpdated.copy(enabled = proto.getEnabled)
      }
    } else if (proto.hasState && proto.getState.getNumber != currentState) {
      context.auth.authorize(context, "endpoint_state", "update", List(endpoint.entityId))
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
    } else if (proto.hasFrontEnd && currentFep != requestedFepId) {
      context.auth.authorize(context, "endpoint_connection", "update", List(endpoint.entityId))
      if (endpoint.autoAssigned) throw new BadRequestException("Cannot claim endpoint that is autoAssigned")
      exclusiveUpdate(context, existing, isSame _) { toBeUpdated =>
        toBeUpdated.copy(applicationId = requestedFepId)
      }
    } else {
      // If we don't do an auth check AT ALL, this is a sneaky way to read without permissions
      // TODO: magic string
      context.auth.authorize(context, "endpoint_connection", "update", List(endpoint.entityId))

      // state and enabled weren't altered, return NOT_MODIFIED
      (existing, false)
    }
  }

  override def postUpdate(context: RequestContext, sql: FrontEndAssignment, existing: FrontEndAssignment) {
    logger.info("EndpointConnection UPDATED: " + sql.endpoint.value.map { _.entityName } + " id " + existing.id + " e: " + sql.enabled + " s: " + ConnProto.State.valueOf(sql.state) + " fep: " + sql.applicationId)
    coordinator.onFepConnectionChange(context, sql, existing)
  }

  // don't ever actually remove the assignments, keep them around as an audit log
  override def delete(context: RequestContext, entry: FrontEndAssignment) = {

    // downside of using case classes and copy constructor is id is not copied
    val deleted = entry.copy(active = false)
    deleted.id = entry.id

    table.update(deleted)

    onDeleted(context, deleted)
    postDelete(context, deleted)
    deleted
  }

  def deleteAllAssignmentsForEndpoint(sql: CommunicationEndpoint) {
    // when we delete the endpoint we need to delete all of the assignments it ever had
    table.deleteWhere(_.endpointId === sql.id)
  }
}

object CommunicationEndpointConnectionConversion extends CommunicationEndpointConnectionConversion

trait CommunicationEndpointConnectionConversion
    extends UniqueAndSearchQueryable[ConnProto, FrontEndAssignment] {

  val table = ApplicationSchema.frontEndAssignments

  // match sorting of endpoints
  def sortResults(list: List[ConnProto]) = list.sortBy(f => f.getEndpoint.getName)

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.frontEnd.uuid.value :: req.id.value :: req.endpoint.uuid.value :: req.endpoint.name :: Nil
  }

  def relatedEntities(entries: List[FrontEndAssignment]) = {
    entries.map { _.endpoint.value.map { _.entityId } }.flatten
  }

  private def resourceId = Descriptors.endpointConnection.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: FrontEndAssignment) = {
    sql.id in from(table, ApplicationSchema.endpoints)((assign, endpoint) =>
      where(
        (assign.endpointId === endpoint.id) and
          (endpoint.entityId in entitySelector))
        select (assign.id))
  }

  override def selector(map: VisibilityMap, sql: FrontEndAssignment): LogicalBoolean = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }

  override def searchQuery(context: RequestContext, proto: ConnProto, sql: FrontEndAssignment) = {
    List(
      proto.frontEnd.appConfig.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(context, app, { _.id })),
      proto.state.asParam(sql.state === _.getNumber),
      proto.enabled.asParam(sql.enabled === _))
  }

  override def uniqueQuery(context: RequestContext, proto: ConnProto, sql: FrontEndAssignment) = {
    List(
      // TODO: after 0.5.0 we can make this a searchable parameter and expose the history to clients
      Some(sql.active === true),
      proto.id.value.asParam(sql.id === _.toLong).unique,
      proto.endpoint.map(endpoint => sql.endpointId in CommEndCfgServiceConversion.uniqueQueryForId(context, endpoint, { _.id })))
  }

  def isModified(entry: FrontEndAssignment, existing: FrontEndAssignment): Boolean = {
    entry.applicationId != existing.applicationId ||
      entry.serviceRoutingKey != existing.serviceRoutingKey ||
      entry.state != existing.state ||
      entry.assignedTime != existing.assignedTime ||
      entry.offlineTime != existing.offlineTime ||
      entry.onlineTime != existing.onlineTime ||
      entry.enabled != existing.enabled ||
      entry.applicationId != existing.applicationId ||
      entry.active != existing.active
  }

  def convertToProto(entry: FrontEndAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setId(makeId(entry))

    entry.application.value.foreach(app => b.setFrontEnd(FrontEndProcessor.newBuilder.setUuid(makeUuid(app)).setAppConfig(ApplicationConfig.newBuilder.setInstanceName(app.instanceName))))
    entry.endpoint.value.foreach(endpoint => b.setEndpoint(makeSparseEndpointProto(endpoint)))
    entry.serviceRoutingKey.foreach(k => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(k)))
    b.setState(ConnProto.State.valueOf(entry.state))
    b.setEnabled(entry.enabled)
    b.setActive(entry.active)

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
      .setAutoAssigned(endpoint.autoAssigned)

    endpoint.port.value.foreach(p => b.setChannel(FrontEndPortConversion.convertToProto(p)))

    b
  }
}
