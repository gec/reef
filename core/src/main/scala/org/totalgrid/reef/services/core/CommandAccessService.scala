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

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Commands.{ CommandAccess => AccessProto }
import org.totalgrid.reef.models.{ CommandAccessModel => AccessModel }
import org.totalgrid.reef.protoapi.ServiceException

import org.squeryl.PrimitiveTypeMode._
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.proto.Descriptors

import BaseProtoService._

class CommandAccessService(protected val modelTrans: ServiceTransactable[CommandAccessServiceModel])
    extends BaseProtoService[AccessProto, AccessModel, CommandAccessServiceModel]
    with GetEnabled
    with SubscribeEnabled
    with PostLikeEnabled
    with DeleteEnabled {

  import AccessProto._

  val defaultSelectTime = 30000

  override val descriptor = Descriptors.commandAccess

  def deserialize(bytes: Array[Byte]) = AccessProto.parseFrom(bytes)

  override protected def preCreate(proto: AccessProto): AccessProto = {
    // Simple proto validity check
    if (proto.getCommandsList.length == 0)
      throw new ServiceException("Must specify at least one command", Envelope.Status.BAD_REQUEST)
    if (!proto.hasAccess)
      throw new ServiceException("Must specify access mode", Envelope.Status.BAD_REQUEST)

    // Being a select (allowed) implies you have user and expiry
    if (proto.getAccess == AccessMode.ALLOWED) {

      // Set expire time to default or else use proto as-is
      if (!proto.hasExpireTime) AccessProto.newBuilder(proto).setExpireTime(defaultSelectTime).build
      else proto
    } else proto
  }

  override protected def doDelete(model: CommandAccessServiceModel, req: AccessProto): List[AccessProto] = {
    val existing = model.findRecords(req)
    existing.foreach(model.removeAccess(_))
    existing.map(model.convertToProto(_))
  }
}
