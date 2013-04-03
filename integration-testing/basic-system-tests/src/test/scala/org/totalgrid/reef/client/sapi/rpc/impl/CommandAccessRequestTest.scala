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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.exception.ReefServiceException

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite

@RunWith(classOf[JUnitRunner])
class CommandAccessRequestTest extends ServiceClientSuite {

  test("Search") {
    val cmdNames = "SimulatedSubstation.Breaker01.Trip" :: "SimulatedSubstation.Breaker01.Close" :: Nil

    val cmd0 = client.getCommandByName(cmdNames.get(0))
    val cmd1 = client.getCommandByName(cmdNames.get(1))

    val firstResp = client.createCommandExecutionLock(cmd0)
    val secondResp = client.createCommandExecutionLock(cmd1)
    val thirdResp = client.createCommandDenialLock(cmd0 :: cmd1 :: Nil)

    client.getCommandLockById(firstResp.getId)

    client.getCommandLocks()

    // TODO: add getCommandLocksByUser call
    //recorder.addExplanation("Get for user", "Search for all access entries for the given user.")
    //session.get(CommandLockRequestBuilders.getForUser(firstResp.getUser)).expectMany()

    client.deleteCommandLock(firstResp)
    client.deleteCommandLock(secondResp)
    client.deleteCommandLock(thirdResp)

  }

  test("Allowed") {

    val cmd = client.getCommandByName("StaticSubstation.Breaker02.Trip")

    val createResp = client.createCommandExecutionLock(cmd)

    client.deleteCommandLock(createResp)
  }

  test("Block") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil
    val cmds = cmdNames.map { client.getCommandByName(_) }

    val createdResp = client.createCommandExecutionLock(cmds)

    client.deleteCommandLock(createdResp)
  }

  test("ReBlock") {

    val cmdNames = "StaticSubstation.Breaker02.Trip" :: "StaticSubstation.Breaker02.Close" :: Nil
    val cmds = cmdNames.map { client.getCommandByName(_) }

    val createdResp = client.createCommandExecutionLock(cmds)

    intercept[ReefServiceException] {
      client.createCommandExecutionLock(cmds.get(0))
    }

    client.deleteCommandLock(createdResp)
  }
}