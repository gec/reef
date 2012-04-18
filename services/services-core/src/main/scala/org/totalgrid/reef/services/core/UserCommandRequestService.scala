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

import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.sapi.service.ServiceTypeIs
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.services.framework._
import ServiceBehaviors._
import org.totalgrid.reef.models.{ Command, UserCommandModel }
import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus, UserCommandRequest }
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Response }
import org.totalgrid.reef.client.Client
import org.totalgrid.reef.client.{ AddressableDestination, Routable }

class UserCommandRequestService(
  protected val model: UserCommandRequestServiceModel)
    extends AsyncModeledServiceBase[UserCommandRequest, UserCommandModel, UserCommandRequestServiceModel]
    with UserCommandRequestValidation
    with AsyncGetEnabled
    with AsyncPutCreatesOrUpdates
    with SubscribeEnabled
    with Logging {

  override val descriptor = Descriptors.userCommandRequest

  override def doAsyncPutPost(contextSource: RequestContextSource, rsp: Response[UserCommandRequest], callback: Response[UserCommandRequest] => Unit) = {
    val request = rsp.expectOne

    contextSource.transaction { context =>

      val command = Command.findByNames(request.getCommandRequest.getCommand.getName :: Nil).single

      val address = command.endpoint.value match {
        case Some(ep) =>
          val frontEndAssignment = ep.frontEndAssignment.value

          val endpointState = EndpointConnection.State.valueOf(frontEndAssignment.state)

          if (endpointState != EndpointConnection.State.COMMS_UP) {
            throw new BadRequestException("Endpoint: " + ep.entityName + " is not COMMS_UP, current state: " + endpointState)
          }

          frontEndAssignment.serviceRoutingKey match {
            case Some(key) => new AddressableDestination(key)
            case None => throw new BadRequestException("No routing info for endpoint: " + ep.entityName)
          }
        case None => throw new BadRequestException("Command has no endpoint set: " + request)
      }
      requestCommand(context.client, request, address, contextSource, callback)
    }
  }

  private def requestCommand(client: Client, request: UserCommandRequest, address: Routable, contextSource: RequestContextSource, callback: Response[UserCommandRequest] => Unit) {
    client.getInternal.getOperations.put(request, BasicRequestHeaders.empty.setDestination(address)).listen { response =>
      try {
        contextSource.transaction { context =>
          model.findRecord(context, request) match {
            case Some(record) =>
              val (updatedStatus, errorMessage) = if (response.success) {
                val commandResponse = response.list.head
                import org.totalgrid.reef.client.service.proto.OptionalProtos._
                (commandResponse.getStatus, commandResponse.errorMessage)
              } else {
                val msg = "Got non successful response to command request: " + request + " dest: " + address + " status: " + response.status + " error: " + response.error
                logger.warn { msg }
                (CommandStatus.UNDEFINED, Some(msg))
              }
              model.update(context, record.copy(status = updatedStatus.getNumber, errorMessage = errorMessage), record)
            case None =>
              logger.warn { "Couldn't find command request record to update" }
          }
        }
      } catch {
        case ex: Exception =>
          logger.error("Error handling command response callback: " + ex.getMessage, ex)
      } finally {
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

  override protected def preCreate(context: RequestContext, proto: UserCommandRequest) = {

    if (!proto.getCommandRequest.getCommand.hasName)
      throw new BadRequestException("Request must specify command name", Envelope.Status.BAD_REQUEST)

    if (proto.hasStatus || proto.hasResult)
      throw new BadRequestException("Create must not specify result", Envelope.Status.BAD_REQUEST)

    super.preCreate(context, this.doCommonValidation(proto))
  }

  override protected def preUpdate(context: RequestContext, proto: UserCommandRequest, existing: UserCommandModel) = {

    if (!(proto.hasStatus || proto.hasResult))
      throw new BadRequestException("Update must specify result status", Envelope.Status.BAD_REQUEST)

    super.preUpdate(context, doCommonValidation(proto), existing)
  }
}

object UserCommandRequestService {
  val defaultTimeout = 30000
}
