/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.japi.request.impl

import org.totalgrid.reef.sapi.service.AsyncServiceBase
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.sapi.client.Response
import org.totalgrid.reef.japi.request.command.{ CommandResultCallback, CommandRequestHandler }
import org.totalgrid.reef.proto.Commands.{ CommandStatus, UserCommandRequest }
import org.totalgrid.reef.sapi.client.Success
import org.totalgrid.reef.japi.Envelope

class EndpointCommandHandlerImpl(handler: CommandRequestHandler) extends AsyncServiceBase[UserCommandRequest] {

  val descriptor = Descriptors.userCommandRequest

  override def putAsync(req: UserCommandRequest, env: RequestEnv)(callback: Response[UserCommandRequest] => Unit): Unit = {

    val request = req.getCommandRequest

    val rspPublisher = new CommandResultCallback {

      def setCommandResult(status: CommandStatus) {
        val response = UserCommandRequest.newBuilder(req).setStatus(status).build()
        callback(Success(Envelope.Status.OK, List(response)))
      }
    }

    handler.handleCommandRequest(request, rspPublisher)
  }

}