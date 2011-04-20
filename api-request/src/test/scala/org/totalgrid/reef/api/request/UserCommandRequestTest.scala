/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import builders.{ UserCommandRequestBuilders, CommandAccessRequestBuilders }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.{ CommandRequest, UserCommandRequest }
import org.totalgrid.reef.proto.Commands

@RunWith(classOf[JUnitRunner])
class UserCommandRequestTest
    extends ClientSessionSuite("UserCommandRequest.xml", "UserCommandRequest",
      <div>
        <p>
          Clients use UserCommandRequest to issue a command. The CommandRequest object describes the command
        to be executed, and timeout can be specified by the client code.
        </p>
        <p>Status and user are not specified in put. User is identified from the request header.</p>
      </div>)
    with ShouldMatchers {

  test("Issue command") {

    val cmdName = "StaticSubstation.Breaker02.Trip"
    val acc = CommandAccessRequestBuilders.allowAccessForCommandName(cmdName)
    val accResp = client.putOneOrThrow(acc)

    client.addExplanation("UserCommandRequestBuilders.executeCommand", "Issue a command request for the specified point.")
    val executingCommand = client.putOneOrThrow(UserCommandRequestBuilders.executeControl(cmdName))

    client.addExplanation("Current status of request", "Get the status of a already-issued command request.")
    client.getOneOrThrow(UserCommandRequestBuilders.getStatus(executingCommand))

    client.deleteOneOrThrow(accResp)
  }

}