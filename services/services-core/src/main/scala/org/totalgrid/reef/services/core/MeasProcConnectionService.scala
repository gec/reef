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

import org.totalgrid.reef.client.service.proto.Processing.{ MeasurementProcessingConnection => ConnProto, MeasurementProcessingRouting }
import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.services.coordinators._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.models.UUIDConversions._
import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.squeryl.Query
import java.util.UUID
import org.totalgrid.reef.models.{ FrontEndAssignment, ApplicationSchema, MeasProcAssignment, EntityQuery }
import org.totalgrid.reef.authz.VisibilityMap
import org.squeryl.dsl.ast.LogicalBoolean

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._

class MeasurementProcessingConnectionService(protected val model: MeasurementProcessingConnectionServiceModel)
    extends SyncModeledServiceBase[ConnProto, MeasProcAssignment, MeasurementProcessingConnectionServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.measurementProcessingConnection
}

class MeasurementProcessingConnectionServiceModel
    extends SquerylServiceModel[Long, ConnProto, MeasProcAssignment]
    with EventedServiceModel[ConnProto, MeasProcAssignment]
    with SimpleModelEntryCreation[ConnProto, MeasProcAssignment]
    with MeasurementProcessingConnectionConversion {

  var coordinator: MeasurementStreamCoordinator = null
  def setCoordinator(cr: MeasurementStreamCoordinator, linkModels: Boolean = true) = {
    coordinator = cr

  }

  override def updateFromProto(context: RequestContext, proto: ConnProto, existing: MeasProcAssignment): (MeasProcAssignment, Boolean) = {

    if (!proto.hasReadyTime) throw new BadRequestException("Measurement processor being updated without ready set!")

    if (existing.readyTime.isDefined) logger.warn("Measurement processor already marked as ready!")

    // only update we should get is from the measproc when it is ready to handle measurements

    val updated = existing.copy(readyTime = Some(proto.getReadyTime))
    update(context, updated, existing)
  }

  def createModelEntry(context: RequestContext, proto: ConnProto): MeasProcAssignment = {
    throw new Exception("wrong interface")
  }

  override def postUpdate(context: RequestContext, sql: MeasProcAssignment, existing: MeasProcAssignment) {
    coordinator.onMeasProcAssignmentChanged(context, sql)
  }

}

trait MeasurementProcessingConnectionConversion
    extends UniqueAndSearchQueryable[ConnProto, MeasProcAssignment] {

  val table = ApplicationSchema.measProcAssignments

  def sortResults(list: List[ConnProto]) = list.sortBy(f => (f.measProc.instanceName, f.getLogicalNode.getName))

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.measProc.uuid.value :: req.id.value :: Nil
  }

  def relatedEntities(entries: List[MeasProcAssignment]) = {
    entries.map { _.application.value.map { _.entityId } }.flatten
  }
  private def resourceId = Descriptors.measurementProcessingConnection.id

  private def visibilitySelector(entitySelector: Query[UUID], sql: MeasProcAssignment) = {
    sql.id in from(table, ApplicationSchema.endpoints)((assign, endpoint) =>
      where(
        (assign.endpointId === endpoint.id) and
          (endpoint.entityId in entitySelector))
        select (assign.id))
  }

  override def selector(map: VisibilityMap, sql: MeasProcAssignment) = {
    map.selector(resourceId) { visibilitySelector(_, sql) }
  }

  def searchQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    Nil
  }

  def uniqueQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    List(
      proto.id.value.asParam(sql.id === _.toLong).unique,
      proto.measProc.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(app, { _.id })))
  }

  def isModified(entry: MeasProcAssignment, existing: MeasProcAssignment): Boolean = {
    true
  }

  def convertToProto(entry: MeasProcAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setId(makeId(entry))

    entry.endpoint.value.foreach(endpoint => b.setLogicalNode(EntityQuery.entityToProto(endpoint.entity.value)))
    entry.application.value.map(app => b.setMeasProc(ApplicationConfig.newBuilder.setUuid(makeUuid(app))))
    entry.serviceRoutingKey.foreach(k => {
      val r = MeasurementProcessingRouting.newBuilder
        .setServiceRoutingKey(k)
        .setProcessedMeasDest("measurement")
        .setRawEventDest("raw_events")
      b.setRouting(r)
    })
    entry.assignedTime.foreach(t => b.setAssignedTime(t))
    entry.readyTime.foreach(t => b.setReadyTime(t))

    b.build
  }
}
