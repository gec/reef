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

import org.totalgrid.reef.models.{ ApplicationSchema, ChannelStatus }
import org.totalgrid.reef.proto.Communications.{ChannelState, ChannelStatus => ChannelStatusProto}

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.services.ProtoRoutingKeys

import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

class ChannelStatusService(protected val modelTrans: ServiceTransactable[ChannelStatusServiceModel])
    extends BasicSyncModeledService[ChannelStatusProto, ChannelStatus, ChannelStatusServiceModel] {

  override val descriptor = Descriptors.channelStatus
}

class ChannelStatusServiceModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[ChannelStatusProto, PointServiceModel](pub, classOf[ChannelStatusProto]) {

  def model = new PointServiceModel(subHandler)
}

class ChannelStatusServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[ChannelStatusProto, ChannelStatus]
    with EventedServiceModel[ChannelStatusProto, ChannelStatus]
    with ChannelStatusServiceConversion


trait ChannelStatusServiceConversion
  extends MessageModelConversion[ChannelStatusProto, ChannelStatus]
  with UniqueAndSearchQueryable[ChannelStatusProto, ChannelStatus] {

  import org.squeryl.PrimitiveTypeMode._
  import SquerylModel._
  import org.totalgrid.reef.proto.OptionalProtos._

  val table = ApplicationSchema.channelStatuses

  def searchQuery(proto: ChannelStatusProto, sql: ChannelStatus) =
    List(proto.state.enum.asParam(sql.state === _.getNumber))

  def uniqueQuery(proto: ChannelStatusProto, sql: ChannelStatus) =
     List(proto.uid.asParam(sql.id === _.toLong))

  def getRoutingKey(req: ChannelStatusProto) =
    ProtoRoutingKeys.generateRoutingKey(req.uid :: req.state.enum :: Nil)

  def convertToProto(sql: ChannelStatus): ChannelStatusProto = {
    val state = ChannelState.newBuilder.setEnum(ChannelState.State.valueOf(sql.state))
    ChannelStatusProto.newBuilder.setUid(sql.id.toString).setState(state).build
  }

  def createModelEntry(proto: ChannelStatusProto): ChannelStatus =
    ChannelStatus(proto.getUid, proto.getState.getEnum)

  def isModified(a: ChannelStatus, b: ChannelStatus): Boolean = a.state != b.state

}
