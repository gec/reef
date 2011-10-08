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

import org.totalgrid.reef.sapi.client.{ Response, Success }
import org.totalgrid.reef.sapi.service.AsyncServiceBase
import org.totalgrid.reef.proto.Commands.{ UserCommandRequest => Command, CommandResponse }
import org.totalgrid.reef.proto.Descriptors

import org.totalgrid.reef.protocol.api.CommandHandler
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.promise.FixedPromise

class SingleEndpointCommandService(handler: CommandHandler) extends AsyncServiceBase[Command] {

  import org.totalgrid.reef.protocol.api.Protocol.ResponsePublisher

  val descriptor = Descriptors.userCommandRequest

  override def putAsync(req: Command, env: BasicRequestHeaders)(callback: Response[Command] => Unit): Unit = {

    val rspPublisher = new ResponsePublisher {
      def publish(rsp: CommandResponse) = {
        val response = Command.newBuilder(req).setStatus(rsp.getStatus).build()
        callback(Success(Envelope.Status.OK, List(response)))
        new FixedPromise(true)
      }
    }

    handler.issue(req.getCommandRequest, rspPublisher)
  }

}