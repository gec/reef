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

import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection => ConnProto, MeasurementProcessingRouting }
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.models.{ ApplicationSchema, MeasProcAssignment, ApplicationInstance, CommunicationEndpoint }

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.api.BadRequestException

import org.totalgrid.reef.services.ProtoRoutingKeys

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._

class MeasurementProcessingConnectionService(protected val modelTrans: ServiceTransactable[MeasurementProcessingConnectionServiceModel])
    extends BasicSyncModeledService[ConnProto, MeasProcAssignment, MeasurementProcessingConnectionServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.measurementProcessingConnection
}

class MeasurementProcessingConnectionModelFactory(
  pub: ServiceEventPublishers,
  fepModelFac: ModelFactory[CommunicationEndpointConnectionServiceModel])
    extends BasicModelFactory[ConnProto, MeasurementProcessingConnectionServiceModel](pub, classOf[ConnProto]) {

  def model = new MeasurementProcessingConnectionServiceModel(subHandler, fepModelFac.model)
}
import org.totalgrid.reef.services.coordinators._
class MeasurementProcessingConnectionServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  fepModel: CommunicationEndpointConnectionServiceModel)
    extends SquerylServiceModel[ConnProto, MeasProcAssignment]
    with EventedServiceModel[ConnProto, MeasProcAssignment]
    with MeasurementProcessingConnectionConversion
    with MeasurementCoordinationQueries {

  link(fepModel)

  def onEndpointCreated(ce: CommunicationEndpoint) {

    val now = System.currentTimeMillis

    val applicationId = getMeasProc().map { _.id }
    val assignedTime = applicationId.map { x => now }
    val serviceRoutingKey = applicationId.map { x => "meas_batch_" + ce.entityName }

    create(new MeasProcAssignment(ce.id, serviceRoutingKey, applicationId, assignedTime, None))
  }

  def onEndpointUpdated(ce: CommunicationEndpoint) {
    val assign = table.where(measProc => measProc.endpointId === ce.id).single
    checkAssignment(assign)
  }

  def onEndpointDeleted(ce: CommunicationEndpoint) {
    val assign = table.where(measProc => measProc.endpointId === ce.id).single
    delete(assign)
  }

  def onAppChanged(app: ApplicationInstance, added: Boolean) {
    val rechecks = if (added) {
      table.where(measProc => measProc.applicationId.isNull).toList
    } else {
      table.where(measProc => measProc.applicationId === app.id).toList
    }
    info { "Meas Proc: " + app.instanceName + " added: " + added + " rechecking: " + rechecks.map { _.endpoint.value.get.entityName } }
    rechecks.foreach { checkAssignment(_) }
  }

  private def checkAssignment(assign: MeasProcAssignment) {
    val applicationId = getMeasProc().map { _.id }
    info { assign.endpoint.value.get.entityName + " assigned MeasProc: " + applicationId }
    val assignedTime = applicationId.map { x => System.currentTimeMillis }
    val serviceRoutingKey = applicationId.map { x => "meas_batch_" + assign.endpoint.value.get.entityName }
    if (assign.applicationId != applicationId) {
      val newAssign = assign.copy(applicationId = applicationId, assignedTime = assignedTime, serviceRoutingKey = serviceRoutingKey, readyTime = None)
      update(newAssign, assign)
    }
  }

  override def updateFromProto(proto: ConnProto, existing: MeasProcAssignment): (MeasProcAssignment, Boolean) = {

    if (!proto.hasReadyTime) throw new BadRequestException("Measurement processor being updated without ready set!")

    if (existing.readyTime.isDefined) warn("Measurement processor already marked as ready!")

    // only update we should get is from the measproc when it is ready to handle measurements

    val updated = existing.copy(readyTime = Some(proto.getReadyTime))
    update(updated, existing)
  }

  override def postCreate(sql: MeasProcAssignment) {
    fepModel.onMeasProcAssignmentChanged(sql, sql.assignedTime.isDefined && sql.readyTime.isDefined)
  }
  override def postUpdate(sql: MeasProcAssignment, existing: MeasProcAssignment) {
    fepModel.onMeasProcAssignmentChanged(sql, sql.assignedTime.isDefined && sql.readyTime.isDefined)
  }
  override def postDelete(sql: MeasProcAssignment) {
    fepModel.onMeasProcAssignmentChanged(sql, false)
  }

}

trait MeasurementProcessingConnectionConversion
    extends MessageModelConversion[ConnProto, MeasProcAssignment]
    with UniqueAndSearchQueryable[ConnProto, MeasProcAssignment] {

  val table = ApplicationSchema.measProcAssignments

  def getRoutingKey(req: ConnProto) = ProtoRoutingKeys.generateRoutingKey {
    req.measProc.uuid.uuid ::
      req.uuid.uuid ::
      Nil
  }

  def searchQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    Nil
  }

  def uniqueQuery(proto: ConnProto, sql: MeasProcAssignment) = {
    proto.uuid.uuid.asParam(sql.id === _.toLong) ::
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

    val b = ConnProto.newBuilder.setUuid(makeUuid(entry))

    entry.endpoint.value.foreach(endpoint => b.setLogicalNode(EQ.entityToProto(endpoint.entity.value)))
    entry.applicationId.foreach(appId => b.setMeasProc(ApplicationConfig.newBuilder.setUuid(makeUuid(appId))))
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
