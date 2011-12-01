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

import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto, MeasurementProcessingRouting }
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.models.{ ApplicationSchema, MeasProcAssignment }

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.services.coordinators._
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.proto.Descriptors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._

class MeasurementProcessingConnectionService(protected val model: MeasurementProcessingConnectionServiceModel)
    extends SyncModeledServiceBase[ConnProto, MeasProcAssignment, MeasurementProcessingConnectionServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.measurementProcessingConnection
}

class MeasurementProcessingConnectionServiceModel
    extends SquerylServiceModel[ConnProto, MeasProcAssignment]
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

  override def postUpdate(context: RequestContext, sql: MeasProcAssignment, existing: MeasProcAssignment) {
    coordinator.onMeasProcAssignmentChanged(context, sql)
  }

}

trait MeasurementProcessingConnectionConversion
    extends UniqueAndSearchQueryable[ConnProto, MeasProcAssignment] {

  val table = ApplicationSchema.measProcAssignments

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.measProc.uuid.value :: req.id.value :: Nil
  }

  def searchQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    Nil
  }

  def uniqueQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    proto.id.value.asParam(sql.id === _.toLong) ::
      proto.measProc.map(app => sql.applicationId in ApplicationConfigConversion.uniqueQueryForId(app, { _.id })) ::
      Nil
  }

  def isModified(entry: MeasProcAssignment, existing: MeasProcAssignment): Boolean = {
    true
  }

  def createModelEntry(proto: ConnProto): MeasProcAssignment = {
    throw new Exception("wrong interface")
  }

  def convertToProto(entry: MeasProcAssignment): ConnProto = {

    val b = ConnProto.newBuilder.setId(makeId(entry))

    entry.endpoint.value.foreach(endpoint => b.setLogicalNode(EntityQueryManager.entityToProto(endpoint.entity.value)))
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
