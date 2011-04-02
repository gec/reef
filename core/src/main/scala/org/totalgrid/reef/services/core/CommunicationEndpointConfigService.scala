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

import org.totalgrid.reef.api.BadRequestException
import org.totalgrid.reef.models.{ CommunicationEndpoint, ApplicationSchema, Entity }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig => CommEndCfgProto, EndpointOwnership, CommChannel }
import org.totalgrid.reef.proto.Model.{ Entity => EntityProto, ConfigFile }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.util.Optional._

import org.totalgrid.reef.services.ProtoRoutingKeys

import scala.collection.JavaConversions._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }
import org.totalgrid.reef.proto.Descriptors

class CommunicationEndpointService(protected val modelTrans: ServiceTransactable[CommEndCfgServiceModel])
    extends BasicSyncModeledService[CommEndCfgProto, CommunicationEndpoint, CommEndCfgServiceModel] {

  override val descriptor = Descriptors.commEndpointConfig
}

class CommEndCfgServiceModelFactory(
  pub: ServiceEventPublishers,
  commandFac: ModelFactory[CommandServiceModel],
  configFac: ModelFactory[ConfigFileServiceModel],
  pointFac: ModelFactory[PointServiceModel],
  measProcFac: ModelFactory[MeasurementProcessingConnectionServiceModel],
  fepModelFac: ModelFactory[CommunicationEndpointConnectionServiceModel],
  portModelFac: ModelFactory[FrontEndPortServiceModel])
    extends BasicModelFactory[CommEndCfgProto, CommEndCfgServiceModel](pub, classOf[CommEndCfgProto]) {

  def model = new CommEndCfgServiceModel(subHandler, commandFac.model, configFac.model, pointFac.model, measProcFac.model, fepModelFac.model, portModelFac.model)
}

class CommEndCfgServiceModel(
  protected val subHandler: ServiceSubscriptionHandler,
  commandModel: CommandServiceModel,
  configModel: ConfigFileServiceModel,
  pointModel: PointServiceModel,
  measProcModel: MeasurementProcessingConnectionServiceModel,
  fepModel: CommunicationEndpointConnectionServiceModel,
  portModel: FrontEndPortServiceModel)
    extends SquerylServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with EventedServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with CommEndCfgServiceConversion {

  link(commandModel)
  link(pointModel)
  link(configModel)
  link(measProcModel)
  link(fepModel)
  link(portModel)

  override def createFromProto(proto: CommEndCfgProto): CommunicationEndpoint = {
    checkProto(proto)
    val ent = EQ.findOrCreateEntity(proto.getName, "CommunicationEndpoint")
    EQ.addTypeToEntity(ent, "LogicalNode")
    val sql = create(createModelEntry(proto, ent))
    setLinkedObjects(sql, proto, ent)
    measProcModel.onEndpointCreated(sql)
    fepModel.onEndpointCreated(sql)
    sql
  }

  override def updateFromProto(proto: CommEndCfgProto, existing: CommunicationEndpoint): Tuple2[CommunicationEndpoint, Boolean] = {
    checkProto(proto)
    val (sql, changed) = update(createModelEntry(proto, existing.entity.value), existing)
    setLinkedObjects(sql, proto, existing.entity.value)
    measProcModel.onEndpointUpdated(sql)
    fepModel.onEndpointUpdated(sql)
    (sql, changed)
  }

  private def checkProto(proto: CommEndCfgProto) {
    if (proto.getOwnerships.getPointsCount == 0 && proto.getOwnerships.getCommandsCount == 0)
      throw new BadRequestException("Endpoint must be source (ownership) for atleast one point or command, if unneeded delete instead")
  }

  override def preDelete(sql: CommunicationEndpoint) {
    measProcModel.onEndpointDeleted(sql)
    fepModel.onEndpointDeleted(sql)
  }
  import org.totalgrid.reef.proto.OptionalProtos._
  def setLinkedObjects(sql: CommunicationEndpoint, request: CommEndCfgProto, ent: Entity) {
    pointModel.createAndSetOwningNode(request.ownerships.points.getOrElse(Nil), ent)

    commandModel.createAndSetOwningNode(request.ownerships.commands.getOrElse(Nil), ent)

    configModel.addOwningEntity(request.getConfigFilesList.toList, ent)
  }

  def createModelEntry(proto: CommEndCfgProto, entity: Entity): CommunicationEndpoint = {

    val linkedPort = proto.channel.map { portProto =>
      portModel.findRecord(portProto) match {
        case Some(p) => p
        case None => portModel.createFromProto(portProto)
      }
    }

    new CommunicationEndpoint(
      entity.id,
      proto.getProtocol(),
      linkedPort.map { _.id })
  }
}

trait CommEndCfgServiceConversion extends MessageModelConversion[CommEndCfgProto, CommunicationEndpoint] with UniqueAndSearchQueryable[CommEndCfgProto, CommunicationEndpoint] {

  import org.squeryl.PrimitiveTypeMode._
  import org.totalgrid.reef.proto.OptionalProtos._
  import SquerylModel._

  val table = ApplicationSchema.endpoints

  def getRoutingKey(proto: CommEndCfgProto) = ProtoRoutingKeys.generateRoutingKey {
    hasGet(proto.hasUid, proto.getUid) ::
      hasGet(proto.hasName, proto.getName) :: Nil
  }

  def uniqueQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = {
    List(
      proto.uid.asParam(uid => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setUid(uid).build, { _.id })),
      proto.name.asParam(name => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setName(name).build, { _.id })))
  }

  def searchQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = Nil

  def createModelEntry(proto: CommEndCfgProto): CommunicationEndpoint = throw new Exception("Not using this interface")

  def isModified(entry: CommunicationEndpoint, existing: CommunicationEndpoint) = {
    true
  }

  def convertToProto(sql: CommunicationEndpoint): CommEndCfgProto = {
    val b = CommEndCfgProto.newBuilder()

    b.setUid(sql.entity.value.id.toString)
    b.setName(sql.entity.value.name)
    b.setProtocol(sql.protocol)
    sql.frontEndPortId.foreach(id => b.setChannel(CommChannel.newBuilder().setUid(id.toString).build))

    sql.configFiles.value.foreach(cf => b.addConfigFiles(ConfigFile.newBuilder().setUid(cf.id.toString).build))

    val o = EndpointOwnership.newBuilder
    sql.points.value.foreach(p => o.addPoints(p.name))
    sql.commands.value.foreach(p => o.addCommands(p.name))

    b.setOwnerships(o)

    b.build
  }
}
