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

import org.totalgrid.reef.japi.BadRequestException
import org.totalgrid.reef.models.{ CommunicationEndpoint, ApplicationSchema, Entity }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto, CommEndpointConfig => CommEndCfgProto, EndpointOwnership, CommChannel }
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, ConfigFile }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.util.Optional._

import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinatorFactory, MeasurementStreamCoordinator }
import org.totalgrid.reef.services.{ ServiceDependencies, ProtoRoutingKeys }

class CommunicationEndpointService(protected val modelTrans: ServiceTransactable[CommEndCfgServiceModel])
    extends SyncModeledServiceBase[CommEndCfgProto, CommunicationEndpoint, CommEndCfgServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.commEndpointConfig
}

class CommEndCfgServiceModelFactory(
  dependencies: ServiceDependencies,
  commandFac: ModelFactory[CommandServiceModel],
  configFac: ModelFactory[ConfigFileServiceModel],
  pointFac: ModelFactory[PointServiceModel],
  portModelFac: ModelFactory[FrontEndPortServiceModel],
  coordinatorFac: MeasurementStreamCoordinatorFactory)
    extends BasicModelFactory[CommEndCfgProto, CommEndCfgServiceModel](dependencies, classOf[CommEndCfgProto]) {

  def model = new CommEndCfgServiceModel(subHandler, commandFac.model, configFac.model, pointFac.model, portModelFac.model, coordinatorFac.model)
}

class CommEndCfgServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  commandModel: CommandServiceModel,
  configModel: ConfigFileServiceModel,
  pointModel: PointServiceModel,
  portModel: FrontEndPortServiceModel,
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with EventedServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with CommEndCfgServiceConversion {

  override def createFromProto(context: RequestContext, proto: CommEndCfgProto): CommunicationEndpoint = {
    checkProto(proto)
    val ent = EQ.findOrCreateEntity(proto.getName, "CommunicationEndpoint")
    EQ.addTypeToEntity(ent, "LogicalNode")
    val sql = create(context, createModelEntry(context, proto, ent))
    setLinkedObjects(context, sql, proto, ent)
    coordinator.onEndpointCreated(context, sql)
    sql
  }

  override def updateFromProto(context: RequestContext, proto: CommEndCfgProto, existing: CommunicationEndpoint): Tuple2[CommunicationEndpoint, Boolean] = {
    checkProto(proto)
    val (sql, changed) = update(context, createModelEntry(context, proto, existing.entity.value), existing)
    setLinkedObjects(context, sql, proto, existing.entity.value)
    coordinator.onEndpointUpdated(context, sql)
    (sql, changed)
  }

  private def checkProto(proto: CommEndCfgProto) {
    if (proto.getOwnerships.getPointsCount == 0 && proto.getOwnerships.getCommandsCount == 0)
      throw new BadRequestException("Endpoint must be source (ownership) for atleast one point or command, if unneeded delete instead")
  }

  override def preDelete(context: RequestContext, sql: CommunicationEndpoint) {

    val frontEndAssignment = sql.frontEndAssignment.value
    if (frontEndAssignment.enabled)
      throw new BadRequestException("Cannot delete endpoint that is still enabled, disable before deleting.")

    if (frontEndAssignment.state != ConnProto.State.COMMS_DOWN.getNumber)
      throw new BadRequestException("Cannot delete endpoint that is not in COMMS_DOWN state; currently: " + ConnProto.State.valueOf(frontEndAssignment.state))

    sql.entity.value // preload lazy entity since it will be deleted by the time event is rendered
    coordinator.onEndpointDeleted(context, sql)
  }

  override def postDelete(context: RequestContext, sql: CommunicationEndpoint) {
    EQ.deleteEntity(sql.entity.value) // delete entity which will also sever all "source" and "uses" links
  }

  import org.totalgrid.reef.proto.OptionalProtos._
  def setLinkedObjects(context: RequestContext, sql: CommunicationEndpoint, request: CommEndCfgProto, ent: Entity) {
    pointModel.createAndSetOwningNode(context, request.ownerships.points.getOrElse(Nil), ent)

    commandModel.createAndSetOwningNode(context, request.ownerships.commands.getOrElse(Nil), ent)

    configModel.addOwningEntity(context, request.getConfigFilesList.toList, ent)
  }

  def createModelEntry(context: RequestContext, proto: CommEndCfgProto, entity: Entity): CommunicationEndpoint = {

    val linkedPort = proto.channel.map { portProto =>
      portModel.findRecord(context, portProto) match {
        case Some(p) => p
        case None => portModel.createFromProto(context, portProto)
      }
    }
    // TODO: create "using" edge between port and endpoint

    new CommunicationEndpoint(
      entity.id,
      proto.getProtocol(),
      linkedPort.map { _.entityId })
  }
}

trait CommEndCfgServiceConversion extends MessageModelConversion[CommEndCfgProto, CommunicationEndpoint] with UniqueAndSearchQueryable[CommEndCfgProto, CommunicationEndpoint] {

  import org.squeryl.PrimitiveTypeMode._
  import org.totalgrid.reef.proto.OptionalProtos._
  import SquerylModel._

  val table = ApplicationSchema.endpoints

  def getRoutingKey(proto: CommEndCfgProto) = ProtoRoutingKeys.generateRoutingKey {
    proto.uuid.uuid :: proto.name :: Nil
  }

  def uniqueQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = {
    List(
      proto.uuid.uuid.asParam(uid => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setUuid(proto.uuid.get).build, { _.id })),
      proto.name.asParam(name => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setName(name).build, { _.id })))
  }

  def searchQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = Nil

  def createModelEntry(proto: CommEndCfgProto): CommunicationEndpoint = throw new Exception("Not using this interface")

  def isModified(entry: CommunicationEndpoint, existing: CommunicationEndpoint) = {
    true // we always consider it to have changed to force coordinator to re-check fep assignment
  }

  def convertToProto(sql: CommunicationEndpoint): CommEndCfgProto = {
    val b = CommEndCfgProto.newBuilder()

    b.setUuid(makeUuid(sql.entity.value))
    b.setName(sql.entity.value.name)
    b.setProtocol(sql.protocol)
    sql.frontEndPortId.foreach(id => b.setChannel(CommChannel.newBuilder().setUuid(makeUuid(id)).build))

    sql.configFiles.value.foreach(cf => b.addConfigFiles(ConfigFile.newBuilder().setUuid(makeUuid(cf)).build))

    val o = EndpointOwnership.newBuilder
    sql.points.value.foreach(p => o.addPoints(p.entityName))
    sql.commands.value.foreach(p => o.addCommands(p.entityName))

    b.setOwnerships(o)

    b.build
  }
}
object CommEndCfgServiceConversion extends CommEndCfgServiceConversion
