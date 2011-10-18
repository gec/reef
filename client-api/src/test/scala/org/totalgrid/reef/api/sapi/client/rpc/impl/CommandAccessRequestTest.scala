package org.totalgrid.reef.sapi.client.rpc.impl

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
import org.totalgrid.reef.api.japi.client.rpc.impl.builder.CommandAccessRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.api.japi.ReefServiceException


@RunWith(classOf[JUnitRunner])
class CommandAccessRequestTest
    extends ClientSessionSuite("CommandAccess.xml", "CommandAccess",
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
    val cmdNames = "SimulatedSubstation.Breaker01.Trip" :: "SimulatedSubstation.Breaker01.Close" :: Nil
    val firstResp = session.put(CommandAccessRequestBuilders.allowAccessForCommandName(cmdNames.get(0))).await.expectOne
    val secondResp = session.put(CommandAccessRequestBuilders.allowAccessForCommandName(cmdNames.get(1))).await.expectOne
    val thirdResp = session.put(CommandAccessRequestBuilders.blockAccessForCommandNames(cmdNames)).await.expectOne

    recorder.addExplanation("Get by UID", "Search for an access entry by UID.")
    session.get(CommandAccessRequestBuilders.getForUid(firstResp.getUid)).await.expectMany()

    recorder.addExplanation("Get all", "Search for all access entries.")
    session.get(CommandAccessRequestBuilders.getAll).await.expectMany()

    recorder.addExplanation("Get for user", "Search for all access entries for the given user.")
    session.get(CommandAccessRequestBuilders.getForUser(firstResp.getUser)).await.expectMany()

    session.delete(firstResp).await.expectOne
    session.delete(secondResp).await.expectOne
    session.delete(thirdResp).await.expectOne
  }

  test("Allowed") {

    val cmdName = "StaticSubstation.Breaker02.Trip"
    recorder.addExplanation("Create allow access", "Create ALLOWED access entry for a command.")
    val createResp = session.put(CommandAccessRequestBuilders.allowAccessForCommandName(cmdName)).await.expectOne

    val delDesc = "The same proto object that was used to create an entry can be used to delete it. " +
      "The service identifies the object by uid, other fields are ignored."

    recorder.addExplanation("Delete access using original object", delDesc)
    session.delete(createResp).await.expectOne
  }

  test("Block") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil
    recorder.addExplanation("Block commands", "Create BLOCKED access for multiple commands.")
    val createdResp = session.put(CommandAccessRequestBuilders.blockAccessForCommandNames(cmdNames)).await.expectOne

    recorder.addExplanation("Delete access using UID", "Delete a command access object by UID only.")
    session.delete(CommandAccessRequestBuilders.delete(createdResp)).await.expectOne
  }

  test("ReBlock") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil

    recorder.addExplanation("Select commands", "Create ALLOWED access for multiple commands.")
    val createdResp = session.put(CommandAccessRequestBuilders.allowAccessForCommandNames(cmdNames)).await.expectOne

    recorder.addExplanation("Selecing Selected Commands fails", "Trying to reselect the command fails with a non success status code. (Note that this will normally mean a ReefServiceException will be thrown by client code)")
    intercept[ReefServiceException] {
      session.put(CommandAccessRequestBuilders.allowAccessForCommandName("StaticSubstation.Breaker02.Close")).await.expectOne
    }

    session.delete(CommandAccessRequestBuilders.delete(createdResp)).await.expectOne
  }
}