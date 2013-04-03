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

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.totalgrid.reef.client.service.proto.Commands.CommandStatus

@RunWith(classOf[JUnitRunner])
class UserCommandRequestTest extends ServiceClientSuite {

  test("Issue command") {

    val cmdName = "StaticSubstation.Breaker02.Trip"
    val cmd = client.getCommandByName(cmdName)

    val lock = client.createCommandExecutionLock(cmd)
    try {
      val commandStatus = client.executeCommandAsControl(cmd)

      commandStatus.getStatus should equal(CommandStatus.SUCCESS)
    } finally {
      client.deleteCommandLock(lock)
    }
  }

}