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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.api.proto.Commands.{ UserCommandRequest => Command, CommandResponse }

import org.totalgrid.reef.api.protocol.api.CommandHandler
import org.totalgrid.reef.api.sapi.impl.Descriptors
import org.totalgrid.reef.api.sapi.service.AsyncServiceBase
import org.totalgrid.reef.api.japi.Envelope
import org.totalgrid.reef.api.sapi.client.{ SuccessResponse, BasicRequestHeaders, Response }

class SingleEndpointCommandService(handler: CommandHandler) extends AsyncServiceBase[Command] {

  import org.totalgrid.reef.api.protocol.api.Protocol.ResponsePublisher

  val descriptor = Descriptors.userCommandRequest

  override def putAsync(req: Command, env: BasicRequestHeaders)(callback: Response[Command] => Unit): Unit = {

    val rspPublisher = new ResponsePublisher {
      def publish(rsp: CommandResponse) = {
        val response = Command.newBuilder(req).setStatus(rsp.getStatus).build()
        callback(SuccessResponse(Envelope.Status.OK, List(response)))
      }
    }

    handler.issue(req.getCommandRequest, rspPublisher)
  }

}