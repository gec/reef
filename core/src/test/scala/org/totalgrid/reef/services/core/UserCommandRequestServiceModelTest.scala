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
import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, CommandAccess }
import CommandAccess._

import org.totalgrid.reef.messaging.serviceprovider.SilentServiceSubscriptionHandler
import org.totalgrid.reef.japi.{ Envelope, ReefServiceException }
import org.totalgrid.reef.models.{ DatabaseUsingTestBase, RunTestsInsideTransaction, ApplicationSchema, Command => FepCommandModel, UserCommandModel, CommandAccessModel, CommandBlockJoin }

@RunWith(classOf[JUnitRunner])
class UserCommandRequestServiceModelTest extends DatabaseUsingTestBase with RunTestsInsideTransaction {

  trait UserCommandTestRig extends CommandTestRig with AccessTestRig {
    val userCommands = new UserCommandRequestServiceModel(new SilentServiceSubscriptionHandler, commandModel, accessModel)

  }
  class TestRig extends UserCommandTestRig with AccessTestRig {
    val model = userCommands
    val cmd = seed("cmd01")

    def scenario(mode: AccessMode, time: Long, user: String) {
      val selectId = seed(new CommandAccessModel(mode.getNumber, Some(time), Some(user))).id
      ApplicationSchema.commandToBlocks.insert(new CommandBlockJoin(cmd.id, selectId))
      cmd.lastSelectId = Some(selectId)
      ApplicationSchema.commands.update(cmd)
    }
  }

  def markCompleted(status: CommandStatus) {
    val r = new TestRig

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build
    val inserted = r.model.table.insert(new UserCommandModel(r.cmd.id, "", "user01", CommandStatus.EXECUTING.getNumber, 5000 + System.currentTimeMillis, cmdReq.toByteString.toByteArray))

    r.model.markCompleted(inserted, status)

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
    val inserted = r.model.table.insert(new UserCommandModel(r.cmd.id, "", "user01", CommandStatus.EXECUTING.getNumber, System.currentTimeMillis - 5000, cmdReq.toByteString.toByteArray))

    r.model.findAndMarkExpired

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
      r.model.issueCommand("cmd01", "", "user01", 5000, cmdReq.toByteString.toByteArray)
    }
  }

  test("Request") {
    val r = new TestRig
    val time = System.currentTimeMillis + 40000
    r.scenario(AccessMode.ALLOWED, time, "user01")

    val cmdReq = CommandRequest.newBuilder.setName("cmd01").build

    r.model.issueCommand("cmd01", "", "user01", 5000, cmdReq.toByteString.toByteArray)

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

}