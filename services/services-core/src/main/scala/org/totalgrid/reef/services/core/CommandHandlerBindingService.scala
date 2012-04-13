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

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.FEP.CommandHandlerBinding
import org.totalgrid.reef.client.sapi.client._
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.proto.Envelope

class CommandHandlerBindingService extends ServiceEntryPoint[CommandHandlerBinding] {

  private val endpointConnectionService = new CommunicationEndpointConnectionServiceModel

  override val descriptor = Descriptors.commandHandlerBinding

  override def postAsync(contextSource: RequestContextSource, req: CommandHandlerBinding)(callback: Response[CommandHandlerBinding] => Unit) {
    callback(contextSource.transaction { context =>

      if (!req.hasEndpointConnection || !req.hasCommandQueue) throw new BadRequestException("Must include both command_queue and endpoint_connection")

      val foundConnection = endpointConnectionService.findRecord(context, req.getEndpointConnection)

      val connection = foundConnection.getOrElse(throw new BadRequestException("EndpointConnection not found."))

      val klass = Descriptors.userCommandRequest().getKlass()
      val commandQueueName = req.getCommandQueue
      val routingKey = connection.serviceRoutingKey.getOrElse(throw new BadRequestException("Endpoint not running"))

      context.client.bindServiceQueue(commandQueueName, routingKey, klass)

      Response(Envelope.Status.OK, req)
    })
  }

}