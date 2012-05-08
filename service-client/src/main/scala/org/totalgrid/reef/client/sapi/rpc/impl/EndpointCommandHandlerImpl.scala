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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.client.sapi.service.AsyncServiceBase
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.command.{ CommandResultCallback, CommandRequestHandler }
import org.totalgrid.reef.client.service.proto.Commands.{ CommandResult, CommandStatus, UserCommandRequest }
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.RequestHeaders
import org.totalgrid.reef.client.operations.scl.ScalaResponse

class EndpointCommandHandlerImpl(handler: CommandRequestHandler) extends AsyncServiceBase[UserCommandRequest] {

  val descriptor = Descriptors.userCommandRequest

  override def putAsync(req: UserCommandRequest, env: RequestHeaders)(callback: Response[UserCommandRequest] => Unit): Unit = {

    val request = req.getCommandRequest

    val rspPublisher = new CommandResultCallback {
      var alreadySet = false
      def setCommandResult(status: CommandStatus) {
        setCommandResult(status, "")
      }

      def setCommandResult(status: CommandStatus, errorMessage: String) {
        if (alreadySet) throw new IllegalArgumentException("Command result already set.")
        alreadySet = true
        val response = UserCommandRequest.newBuilder(req).setStatus(status)

        val result = CommandResult.newBuilder().setStatus(status)
        // dont set the errorMessage unless there is an interesting message
        if (errorMessage != null && errorMessage != "") {
          response.setErrorMessage(errorMessage)
          result.setErrorMessage(errorMessage)
        }
        response.setResult(result)

        callback(ScalaResponse.success(Envelope.Status.OK, response.build()))
      }
    }

    handler.handleCommandRequest(request, rspPublisher)
  }

}