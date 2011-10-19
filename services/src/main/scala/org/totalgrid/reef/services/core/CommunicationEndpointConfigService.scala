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

import org.totalgrid.reef.api.japi.BadRequestException
import org.totalgrid.reef.models.{ CommunicationEndpoint, ApplicationSchema, Entity }
import org.totalgrid.reef.api.proto.FEP.{ CommEndpointConnection => ConnProto, CommEndpointConfig => CommEndCfgProto, EndpointOwnership, CommChannel }
import org.totalgrid.reef.api.proto.Model.{ ReefUUID, Entity => EntityProto, ConfigFile }
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.api.sapi.Optional._
import org.totalgrid.reef.api.sapi.impl.OptionalProtos._

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.sapi.impl.Descriptors
import org.totalgrid.reef.services.coordinators.{ MeasurementStreamCoordinator }
import org.totalgrid.reef.services.ProtoRoutingKeys

class CommunicationEndpointService(protected val model: CommEndCfgServiceModel)
    extends SyncModeledServiceBase[CommEndCfgProto, CommunicationEndpoint, CommEndCfgServiceModel]
    with DefaultSyncBehaviors {

  override val descriptor = Descriptors.commEndpointConfig
}

class CommEndCfgServiceModel(
  commandModel: CommandServiceModel,
  configModel: ConfigFileServiceModel,
  pointModel: PointServiceModel,
  portModel: FrontEndPortServiceModel,
  coordinator: MeasurementStreamCoordinator)
    extends SquerylServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with EventedServiceModel[CommEndCfgProto, CommunicationEndpoint]
    with CommEndCfgServiceConversion {

  override def createFromProto(context: RequestContext, proto: CommEndCfgProto): CommunicationEndpoint = {
    import org.totalgrid.reef.services.core.util.UUIDConversions._
    val ent = EntityQueryManager.findOrCreateEntity(proto.getName, "CommunicationEndpoint", proto.uuid)
    EntityQueryManager.addTypeToEntity(ent, "LogicalNode")
    val sql = create(context, createModelEntry(context, proto, ent))
    setLinkedObjects(context, sql, proto, ent)
    coordinator.onEndpointCreated(context, sql)
    sql
  }

  override def updateFromProto(context: RequestContext, proto: CommEndCfgProto, existing: CommunicationEndpoint): Tuple2[CommunicationEndpoint, Boolean] = {
    val (sql, changed) = update(context, createModelEntry(context, proto, existing.entity.value), existing)
    setLinkedObjects(context, sql, proto, existing.entity.value)
    coordinator.onEndpointUpdated(context, sql)
    (sql, changed)
  }

  override def preDelete(context: RequestContext, sql: CommunicationEndpoint) {

    val frontEndAssignment = sql.frontEndAssignment.value
    if (frontEndAssignment.enabled)
      throw new BadRequestException("Cannot delete endpoint that is still enabled, disable before deleting.  Try running karaf command: endpoint:disable *")

    if (frontEndAssignment.state != ConnProto.State.COMMS_DOWN.getNumber)
      throw new BadRequestException("Cannot delete endpoint that is not in COMMS_DOWN state; currently: " + ConnProto.State.valueOf(frontEndAssignment.state))

    sql.entity.value // preload lazy entity since it will be deleted by the time event is rendered
    coordinator.onEndpointDeleted(context, sql)
  }

  override def postDelete(context: RequestContext, sql: CommunicationEndpoint) {
    EntityQueryManager.deleteEntity(sql.entity.value) // delete entity which will also sever all "source" and "uses" links
  }

  private def findEntites(names: List[String], typ: String): List[Entity] = {
    if (!names.isEmpty) {
      val entities = EntityQueryManager.findEntities(names, typ :: Nil).toList
      val missing = names.diff(entities.map(_.name))
      if (!missing.isEmpty) throw new BadRequestException("Trying to set endpoint for unknown " + typ + ": " + missing)
      entities
    } else {
      Nil
    }
  }

  import org.totalgrid.reef.api.sapi.impl.OptionalProtos._
  def setLinkedObjects(context: RequestContext, sql: CommunicationEndpoint, request: CommEndCfgProto, entity: Entity) {

    val pointEntities = findEntites(request.ownerships.points.getOrElse(Nil), "Point")
    val commandEntities = findEntites(request.ownerships.commands.getOrElse(Nil), "Command")

    val (relationship, exclusive) = if (sql.dataSource) ("source", true) else ("sink", false)
    EntityQueryManager.addEdges(entity, pointEntities ::: commandEntities, relationship, exclusive)

    configModel.addOwningEntity(context, request.getConfigFilesList.toList, entity)
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
      linkedPort.map { _.entityId },
      proto.dataSource.getOrElse(true))
  }
}

trait CommEndCfgServiceConversion extends UniqueAndSearchQueryable[CommEndCfgProto, CommunicationEndpoint] {

  import org.squeryl.PrimitiveTypeMode._

  import SquerylModel._

  val table = ApplicationSchema.endpoints

  def getRoutingKey(proto: CommEndCfgProto) = ProtoRoutingKeys.generateRoutingKey {
    proto.uuid.uuid :: proto.name :: Nil
  }

  def uniqueQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = {
    List(
      proto.uuid.uuid.asParam(uid => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setUuid(ReefUUID.newBuilder.setUuid(uid)).build, { _.id })),
      proto.name.asParam(name => sql.entityId in EntitySearches.searchQueryForId(EntityProto.newBuilder.setName(name).build, { _.id })))
  }

  def searchQuery(proto: CommEndCfgProto, sql: CommunicationEndpoint) = {
    List(
      proto.channel.map { channel => sql.frontEndPortId in FrontEndPortConversion.searchQueryForId(channel, { _.entityId }) })
  }

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
