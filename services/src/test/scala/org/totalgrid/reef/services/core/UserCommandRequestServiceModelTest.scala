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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, CommandAccess }
import CommandAccess._

import org.totalgrid.reef.models._
import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.japi.{ BadRequestException, ReefServiceException }
import org.totalgrid.reef.services.HeadersRequestContext

@RunWith(classOf[JUnitRunner])
class UserCommandRequestServiceModelTest extends DatabaseUsingTestBase with RunTestsInsideTransaction {

  class TestRig extends CommandTestRig {

    val cid = seed("cmd01").id
    def cmd = ApplicationSchema.commands.where(c => c.id === cid).single

    def scenario(mode: AccessMode, time: Long, user: String) = {

      val updated = cmd
      val select = seed(new CommandAccessModel(mode.getNumber, Some(time), Some(user)))
      ApplicationSchema.commandToBlocks.insert(new CommandBlockJoin(updated.id, select.id))

      updated.lastSelectId = Some(select.id)
      ApplicationSchema.commands.update(updated)
      select
    }

  }

  val env = new RequestEnv
  env.setUserName("user01")
  val context = new HeadersRequestContext(env)

  def markCompleted(status: CommandStatus) {
    val r = new TestRig

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build
    val inserted = r.userRequests.table.insert(new UserCommandModel(r.cmd.id, "", "user01", CommandStatus.EXECUTING.getNumber, 5000 + System.currentTimeMillis, cmdReq.toByteString.toByteArray))

    r.userRequests.markCompleted(context, inserted, status)

    val entries = ApplicationSchema.userRequests.where(t => true === true).toList
    entries.length should equal(1)
    val entry = entries.head
    entry.commandId should equal(r.cmd.id)
    entry.agent should equal("user01")
    entry.status should equal(status.getNumber)
  }

  test("Mark completed success") {
    markCompleted(CommandStatus.SUCCESS)
  }
  test("Mark completed error") {
    markCompleted(CommandStatus.HARDWARE_ERROR)
  }

  test("Mark expired") {
    val r = new TestRig

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build
    val inserted = r.userRequests.table.insert(new UserCommandModel(r.cmd.id, "", "user01", CommandStatus.EXECUTING.getNumber, System.currentTimeMillis - 5000, cmdReq.toByteString.toByteArray))

    r.userRequests.findAndMarkExpired(context)

    val entries = ApplicationSchema.userRequests.where(t => true === true).toList
    entries.length should equal(1)
    val entry = entries.head
    entry.commandId should equal(r.cmd.id)
    entry.agent should equal("user01")
    entry.status should equal(CommandStatus.TIMEOUT.getNumber)
  }

  def failScenario(mode: AccessMode, time: Long, user: String) {
    val r = new TestRig
    r.scenario(mode, time, user)

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build

    intercept[ReefServiceException] {
      r.userRequests.issueCommand(context, "cmd01", "", "user01", 5000, cmdReq)
    }
  }

  test("Request") {
    val r = new TestRig
    val time = System.currentTimeMillis + 40000
    r.scenario(AccessMode.ALLOWED, time, "user01")

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build

    r.userRequests.issueCommand(context, "cmd01", "", "user01", 5000, cmdReq)

    val entries = ApplicationSchema.userRequests.where(t => true === true).toList
    entries.length should equal(1)
    val entry = entries.head
    entry.commandId should equal(r.cmd.id)
    entry.agent should equal("user01")
    entry.status should equal(CommandStatus.EXECUTING.getNumber)
  }

  test("Fail, blocked") {
    failScenario(AccessMode.BLOCKED, System.currentTimeMillis + 40000, "user01")
  }

  test("Fail, expired") {
    failScenario(AccessMode.ALLOWED, System.currentTimeMillis - 40000, "user01")
  }

  test("Fail, wrong user") {
    failScenario(AccessMode.ALLOWED, System.currentTimeMillis + 40000, "user02")
  }

  test("Cannot delete command with outstanding select") {
    val r = new TestRig
    val time = System.currentTimeMillis + 40000
    val select = r.scenario(AccessMode.ALLOWED, time, "user01")

    intercept[BadRequestException] {
      r.commands.delete(context, r.cmd)
    }

    r.accesses.removeAccess(context, select)

    r.commands.delete(context, r.cmd)
  }

  test("Can delete command with expired select") {
    val r = new TestRig
    val time = System.currentTimeMillis - 40000
    val select = r.scenario(AccessMode.ALLOWED, time, "user01")

    r.commands.delete(context, r.cmd)
  }

  test("Deleting command removes history and select") {
    val r = new TestRig
    val now = System.currentTimeMillis

    val select1 = r.scenario(AccessMode.ALLOWED, now - 40000, "user01")
    val select2 = r.scenario(AccessMode.ALLOWED, now - 20000, "user01")
    val select3 = r.scenario(AccessMode.ALLOWED, now + 40000, "user01")

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build

    r.userRequests.issueCommand(context, "cmd01", "", "user01", 5000, cmdReq)
    r.userRequests.issueCommand(context, "cmd01", "", "user01", 5000, cmdReq)
    r.userRequests.issueCommand(context, "cmd01", "", "user01", 5000, cmdReq)
    r.accesses.removeAccess(context, select3)

    val requests = ApplicationSchema.userRequests.where(t => true === true).toList
    requests.length should equal(3)

    val selects = ApplicationSchema.commandAccess.where(t => true === true).toList
    selects.length should equal(2)

    r.commands.delete(context, r.cmd)

    ApplicationSchema.userRequests.where(t => true === true).toList.size should equal(0)
    ApplicationSchema.commandAccess.where(t => true === true).toList.size should equal(0)
  }
}