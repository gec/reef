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

import org.totalgrid.reef.client.service.proto.FEP.{ CommChannel => ChannelProto }
import org.totalgrid.reef.models.{ ApplicationSchema, FrontEndPort }

import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.client.service.proto.Descriptors

import org.totalgrid.reef.client.service.proto.OptionalProtos._
import org.totalgrid.reef.services.core.util.UUIDConversions._

import org.totalgrid.reef.services.framework.ServiceBehaviors._

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.client.sapi.types.Optional._
import org.squeryl.PrimitiveTypeMode._

class FrontEndPortService(protected val model: FrontEndPortServiceModel)
    extends SyncModeledServiceBase[ChannelProto, FrontEndPort, FrontEndPortServiceModel]
    with GetEnabled
    with PutCreatesOrUpdates
    with DeleteEnabled
    with PostPartialUpdate
    with SubscribeEnabled {

  override def merge(context: RequestContext, req: ServiceType, current: ModelType): ServiceType = {

    import org.totalgrid.reef.client.service.proto.OptionalProtos._

    val builder = FrontEndPortConversion.convertToProto(current).toBuilder
    req.state.foreach { builder.setState(_) }
    req.ip.foreach { builder.setIp(_) }
    req.serial.foreach { builder.setSerial(_) }
    builder.build
  }

  override val descriptor = Descriptors.commChannel
}

class FrontEndPortServiceModel
    extends SquerylServiceModel[ChannelProto, FrontEndPort]
    with EventedServiceModel[ChannelProto, FrontEndPort]
    with SimpleModelEntryCreation[ChannelProto, FrontEndPort]
    with FrontEndPortConversion {

  override def preDelete(context: RequestContext, sql: FrontEndPort) {
    if (!sql.endpoints.value.isEmpty)
      throw new BadRequestException("Cannot delete communication channel that is used by: " + sql.endpoints.value.map { _.entityName })
  }

  override def postDelete(context: RequestContext, sql: FrontEndPort) {
    EntityQueryManager.deleteEntity(sql.entity.value)
  }
}

object FrontEndPortConversion extends FrontEndPortConversion

trait FrontEndPortConversion
    extends UniqueAndSearchQueryable[ChannelProto, FrontEndPort] {

  val table = ApplicationSchema.frontEndPorts

  def getRoutingKey(req: ChannelProto) = ProtoRoutingKeys.generateRoutingKey {
    req.name ::
      req.ip.network ::
      req.serial.location :: Nil
  }

  def searchQuery(proto: ChannelProto, sql: FrontEndPort) = {
    Nil
  }

  def uniqueQuery(proto: ChannelProto, sql: FrontEndPort) = {
    val eSearch = EntitySearch(proto.uuid.value, proto.name, proto.name.map(x => List("Channel")))
    List(
      eSearch.map(es => sql.entityId in EntityPartsSearches.searchQueryForId(es, { _.id })))
  }

  def isModified(entry: FrontEndPort, existing: FrontEndPort): Boolean = {
    true
  }

  def createModelEntry(proto: ChannelProto): FrontEndPort = {
    FrontEndPort.newInstance(proto.getName, proto.ip.network, proto.serial.location, proto.getState.getNumber, proto.toByteString.toByteArray, proto.uuid)
  }

  def convertToProto(entry: FrontEndPort): ChannelProto = {
    ChannelProto.parseFrom(entry.proto).toBuilder
      .setUuid(makeUuid(entry))
      .setName(entry.entityName)
      .setState(ChannelProto.State.valueOf(entry.state))
      .build
  }
}
