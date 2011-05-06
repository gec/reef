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

import org.totalgrid.reef.api.scalaclient.ISessionPool

import org.totalgrid.reef.proto.Commands
import Commands.UserCommandRequest

import org.totalgrid.reef.models.UserCommandModel

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.{ Envelope, BadRequestException, AddressableService }

import org.totalgrid.reef.api.ServiceTypes.{ Failure, SingleSuccess, Response }
import org.totalgrid.reef.api.service.ServiceTypeIs

import org.totalgrid.reef.models.{ ApplicationSchema, Command }

import org.totalgrid.reef.services.framework._
import org.squeryl.PrimitiveTypeMode._
import ServiceBehaviors._

class UserCommandRequestService(
  protected val modelTrans: ServiceTransactable[UserCommandRequestServiceModel], pool: ISessionPool)
    extends AsyncModeledServiceBase[UserCommandRequest, UserCommandModel, UserCommandRequestServiceModel]
    with AsyncGetEnabled
    with AsyncPutEnabled
    with SubscribeEnabled
    with UserCommandRequestValidation {

  override val descriptor = Descriptors.userCommandRequest

  override def doAsyncPutPost(rsp: Response[UserCommandRequest], callback: Response[UserCommandRequest] => Unit) = {

    val request = rsp.result.head

    val command = ApplicationSchema.commands.where(cmd => cmd.name === request.getCommandRequest.getName).single

    val address = command.endpoint.value match {
      case Some(ep) =>
        ep.frontEndAssignment.value.serviceRoutingKey match {
          case Some(key) => AddressableService(key)
          case None => throw new BadRequestException("No routing info for endpoint: " + ep.name.value)
        }
      case None => throw new BadRequestException("Command has no endpoint set " + request)
    }

    pool.borrow { session =>
      session.asyncPutOne(request, dest = address) { result =>
        val response: Response[UserCommandRequest] = result match {
          case SingleSuccess(status, cmd) => Response(status, UserCommandRequest.newBuilder(request).setStatus(cmd.getStatus).build :: Nil)
          case Failure(status, msg) => Response(status, error = msg)
        }
        callback(response)
      }
    }
  }

}

trait UserCommandRequestValidation extends HasCreate with HasUpdate {

  self: ServiceTypeIs[UserCommandRequest] with ModelTypeIs[UserCommandModel] =>

  private def doCommonValidation(proto: UserCommandRequest) = {

    if (!proto.hasCommandRequest)
      throw new BadRequestException("Request must specify command information", Envelope.Status.BAD_REQUEST)

    proto
  }

  override protected def preCreate(proto: UserCommandRequest) = {

    if (!proto.getCommandRequest.hasName)
      throw new BadRequestException("Request must specify command name", Envelope.Status.BAD_REQUEST)

    if (proto.hasStatus)
      throw new BadRequestException("Update must not specify status", Envelope.Status.BAD_REQUEST)

    super.preCreate(this.doCommonValidation(proto))
  }

  override protected def preUpdate(proto: UserCommandRequest, existing: UserCommandModel) = {

    if (!proto.hasStatus)
      throw new BadRequestException("Update must specify status", Envelope.Status.BAD_REQUEST)

    super.preUpdate(doCommonValidation(proto), existing)
  }
}

object UserCommandRequestService {
  val defaultTimeout = 30000
}
