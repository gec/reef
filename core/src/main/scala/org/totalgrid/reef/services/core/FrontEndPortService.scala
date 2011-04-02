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

import org.totalgrid.reef.proto.FEP.{ CommChannel => ChannelProto }
import org.totalgrid.reef.models.{ ApplicationSchema, FrontEndPort }

import org.totalgrid.reef.services.framework._

import org.totalgrid.reef.services.ProtoRoutingKeys
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.proto.OptionalProtos._
import org.totalgrid.reef.messaging.serviceprovider.{ ServiceEventPublishers, ServiceSubscriptionHandler }

// implicit proto properties
import SquerylModel._ // implict asParam
import org.totalgrid.reef.util.Optional._
import org.squeryl.PrimitiveTypeMode._

class FrontEndPortService(protected val modelTrans: ServiceTransactable[FrontEndPortServiceModel])
    extends BasicSyncModeledService[ChannelProto, FrontEndPort, FrontEndPortServiceModel] {

  override val descriptor = Descriptors.commChannel
}

class FrontEndPortModelFactory(pub: ServiceEventPublishers)
    extends BasicModelFactory[ChannelProto, FrontEndPortServiceModel](pub, classOf[ChannelProto]) {

  def model = new FrontEndPortServiceModel(subHandler)
}

class FrontEndPortServiceModel(protected val subHandler: ServiceSubscriptionHandler)
    extends SquerylServiceModel[ChannelProto, FrontEndPort]
    with EventedServiceModel[ChannelProto, FrontEndPort]
    with FrontEndPortConversion {
}

trait FrontEndPortConversion
    extends MessageModelConversion[ChannelProto, FrontEndPort]
    with UniqueAndSearchQueryable[ChannelProto, FrontEndPort] {

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
    proto.uid.asParam(sql.id === _.toLong) ::
      proto.name.asParam(sql.name === _) ::
      Nil
  }

  def isModified(entry: FrontEndPort, existing: FrontEndPort): Boolean = {
    true
  }

  def createModelEntry(proto: ChannelProto): FrontEndPort = {
    new FrontEndPort(
      proto.getName,
      proto.ip.network,
      proto.serial.location,
      proto.toByteString.toByteArray)
  }

  def convertToProto(entry: FrontEndPort): ChannelProto = {
    ChannelProto.parseFrom(entry.proto).toBuilder
      .setUid(entry.id.toString)
      .build
  }
}
