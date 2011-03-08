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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.proto.Commands.{ CommandAccess, CommandRequest, UserCommandRequest }
import org.totalgrid.reef.api.ReefServiceException

@RunWith(classOf[JUnitRunner])
class CommandAccessRequestTest
    extends ServiceClientSuite("CommandAccess.xml", "CommandAccess",
      <div>
        <p>
          Represents the "access table" for the system. Access entries have one or two
  modes, "allowed" and "blocked". Commands cannot be issued unless they have an
  "allowed" entry. This "selects" the command for operation by a single user, for
  as long as access is held. "Block" allows selects to be disallowed for commands;
  meaning no users can access/issue the commands.
        </p>
        <p>
          Multiple commands can be referenced (by name) in the same access entry. User is
  determined by the request header.
        </p>
        <p>If not provided, expire_time will be a server-specified default.</p>
      </div>)
    with ShouldMatchers {

  test("Search") {
    val firstReq = CommandAccessRequestBuilders.allowAccessForCommand("StaticSubstation.Breaker02.Trip")
    val firstResp = client.putOneOrThrow(firstReq)

    val secondReq = CommandAccessRequestBuilders.allowAccessForCommand("StaticSubstation.Breaker02.Close")
    val secondResp = client.putOneOrThrow(secondReq)

    val cmdNames = "SimulatedSubstation.Breaker01.Trip" :: "SimulatedSubstation.Breaker01.Close" :: Nil
    val thirdReq = CommandAccessRequestBuilders.blockCommands(cmdNames)
    val thirdResp = client.putOneOrThrow(thirdReq)

    val getReq = CommandAccessRequestBuilders.getForUid(firstResp.getUid)
    val getResp = client.getOneOrThrow(getReq)

    doc.addCase("Get by UID", "Get", "Search for an access entry by UID.", getReq, getResp)

    val getAllReq = CommandAccessRequestBuilders.allAccessEntries
    val getAllResp = client.getOrThrow(getAllReq)

    doc.addCase("Get all", "Get", "Search for all access entries.", getAllReq, getAllResp)

    val getUserReq = CommandAccessRequestBuilders.getForUser(firstResp.getUser)
    val getUserResp = client.getOrThrow(getUserReq)

    doc.addCase("Get for user", "Get", "Search for all access entries for the given user.", getUserReq, getUserResp)

    client.deleteOneOrThrow(firstResp)
    client.deleteOneOrThrow(secondResp)
    client.deleteOneOrThrow(thirdResp)
  }

  test("Allowed") {

    val cmdName = "StaticSubstation.Breaker02.Trip"
    val create = CommandAccessRequestBuilders.allowAccessForCommand(cmdName)
    val createResp = client.putOneOrThrow(create)

    doc.addCase("Create allow access", "Put", "Create ALLOWED access entry for a command.", create, createResp)

    val deleteResp = client.deleteOneOrThrow(createResp)

    val delDesc = "The same proto object that was used to create an entry can be used to delete it. " +
      "The service identifies the object by uid, other fields are ignored."

    doc.addCase("Delete access using original object", "Delete", delDesc, createResp, deleteResp)
  }

  test("Block") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil
    val create = CommandAccessRequestBuilders.blockCommands(cmdNames)
    val createResp = client.putOneOrThrow(create)

    doc.addCase("Block commands", "Put", "Create BLOCKED access for multiple commands.", create, createResp)

    val deleteReq = CommandAccess.newBuilder.setUid(createResp.getUid).build
    val deleteResp = client.deleteOneOrThrow(deleteReq)

    doc.addCase("Delete access using UID", "Delete", "Delete a command access object by UID only.", deleteReq, deleteResp)
  }

  test("ReBlock") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil

    client.addExplanation("Block commands", "Create ALLOWED access for multiple commands.")
    val create = CommandAccessRequestBuilders.allowAccessForCommands(cmdNames)
    val createResp = client.putOneOrThrow(create)

    client.addExplanation("ReBlock commands", "Trying to reselect the command fails with a non success status code. (Note that this will normally mean a ReefServiceException will be thrown by client code)")
    intercept[ReefServiceException] {
      client.putOneOrThrow(CommandAccessRequestBuilders.allowAccessForCommands("StaticSubstation.Breaker02.Close" :: Nil))
    }

    val deleteReq = CommandAccess.newBuilder.setUid(createResp.getUid).build
    val deleteResp = client.deleteOneOrThrow(deleteReq)

    doc.addCase("Delete access using UID", "Delete", "Delete a command access object by UID only.", deleteReq, deleteResp)
  }
}