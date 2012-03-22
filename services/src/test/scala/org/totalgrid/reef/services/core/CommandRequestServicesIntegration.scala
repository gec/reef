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
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest, CommandLock }
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointConnection, EndpointOwnership }
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, CommandType, Command }

import CommandLock._

import org.totalgrid.reef.services._
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.sapi.service.SyncServiceBase
import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Response }
import org.totalgrid.reef.client.sapi.client.rest.Connection
import org.totalgrid.reef.client.exception.BadRequestException

@RunWith(classOf[JUnitRunner])
class CommandRequestServicesIntegration
    extends EndpointRelatedTestBase {

  import ServiceResponseTestingHelpers._

  class CommandFixture(amqp: Connection) extends CoordinatorFixture(amqp) {

    val command = new SyncService(new CommandService(modelFac.cmds), contextSource)
    val commandRequest = new SyncService(new UserCommandRequestService(modelFac.userRequests), contextSource)
    val endpointService = new SyncService(new CommunicationEndpointService(modelFac.endpoints), contextSource)
    val access = new SyncService(new CommandLockService(modelFac.accesses, false), contextSource)

    def enableCommandRequests() {
      val commandNames = List("cmd01")
      addCommands(commandNames)
      addEndpoint(commandNames)
      addMeasProc("meas")

      startCommandResponder()
    }

    def addEndpoint(commands: List[String], endpointName: String = "endpoint1") {
      val owns = EndpointOwnership.newBuilder
      commands.map { owns.addCommands(_) }
      val send = Endpoint.newBuilder()
        .setName(endpointName).setProtocol("benchmark").setOwnerships(owns).build
      endpointService.put(send).expectOne()
    }
    def removeEndpoint(endpointName: String = "endpoint1") {
      val conn = frontEndConnection.get(EndpointConnection.newBuilder.setId("*").build).expectOne()

      setEndpointState(conn, EndpointConnection.State.COMMS_DOWN)
      setEndpointEnabled(conn, false)

      endpointService.delete(Endpoint.newBuilder.setName(endpointName).build).expectOne()
    }

    def addCommands(commands: List[String]) {
      commands.foreach { c =>
        val cmdProto = Command.newBuilder().setName(c).setDisplayName(c).setType(CommandType.CONTROL).build
        command.put(cmdProto).expectOne()
      }
    }

    def deleteCommand(commandName: String) {

      val cmdProto = Command.newBuilder().setName(commandName).build
      command.delete(cmdProto).expectOne()
    }

    def startCommandResponder(status: CommandStatus = CommandStatus.SUCCESS) {

      var commandsRecived = 0
      // the 'remote' service that will handle the call
      val service = new SyncServiceBase[UserCommandRequest] {

        val descriptor = Descriptors.userCommandRequest

        override def put(req: UserCommandRequest, env: BasicRequestHeaders): Response[UserCommandRequest] = {
          val response = UserCommandRequest.newBuilder(req).setStatus(status).setErrorMessage("Command: " + commandsRecived).build
          commandsRecived += 1

          Response(Envelope.Status.OK, response :: Nil)
        }
      }

      val conn = frontEndConnection.get(EndpointConnection.newBuilder.setId("*").build).expectOne()

      // act like the FEP and mark the endpoint as comms_up
      setEndpointState(conn, EndpointConnection.State.COMMS_UP)

      //bind the 'proxied' service that will handle the call
      bindCommandHandler(service, conn.getRouting.getServiceRoutingKey)
    }

    def selectExecuteDelete(cmdName: String = "cmd01", cmdValue: Option[Int] = None) = {

      val selectResult = access.put(selectRequest(List(cmdName))).expectOne()

      val result = commandRequest.put(issueCommand(cmdName, value = cmdValue)).expectOne()
      result.getStatus should equal(CommandStatus.SUCCESS)

      access.delete(selectResult).expectOne()
    }
  }

  def selectRequest(
    names: List[String] = List("cmd01"),
    mode: AccessMode = AccessMode.ALLOWED,
    time: Long = 40000) = {

    CommandLock.newBuilder
      .addAllCommands(names.map { Command.newBuilder.setName(_).build })
      .setAccess(mode)
      .setExpireTime(time)
      .build
  }

  def issueCommand(
    commandName: String = "cmd01",
    correlationId: Option[String] = None,
    value: Option[Int] = None): UserCommandRequest = {

    val c = CommandRequest.newBuilder.setCommand(Command.newBuilder.setName(commandName))
    correlationId.foreach(c.setCorrelationId(_))
    value.foreach(c.setIntVal(_))
    val b = UserCommandRequest.newBuilder
      .setCommandRequest(c)

    b.build
  }

  test("Select Command multiple times") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      val commandNames = List("cmd01")
      fixture.addCommands(commandNames)

      val cmdSelect = fixture.access.put(selectRequest()).expectOne()

      cmdSelect.getCommandsList.toList.map { _.getName } should equal(commandNames)

      val errorMessage = intercept[BadRequestException] {
        fixture.access.put(selectRequest()).expectOne()
      }.getMessage

      errorMessage should include("Some commands are blocked")

      fixture.access.delete(cmdSelect).expectOne()

      val select2 = fixture.access.put(selectRequest()).expectOne()
      select2.getId should not equal (cmdSelect.getId)
    }
  }

  test("Select Multiple Commands") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      val commandNames = List("cmd01", "cmd02", "cmd03")
      fixture.addCommands(commandNames)

      val cmdSelect = fixture.access.put(selectRequest(commandNames)).expectOne()

      cmdSelect.getCommandsList.toList.map { _.getName } should equal(commandNames)

      commandNames.foreach { cname =>
        intercept[BadRequestException] {
          fixture.access.put(selectRequest(List(cname))).expectOne()
        }
      }
    }
  }

  test("Command Block stops selects") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))

      val cmdBlock = fixture.access.put(selectRequest(mode = AccessMode.BLOCKED)).expectOne()

      val errorMessage = intercept[BadRequestException] {
        fixture.access.put(selectRequest()).expectOne()
      }.getMessage

      errorMessage should include("Some commands are blocked")

      fixture.access.delete(cmdBlock).expectOne()
    }
  }

  test("Select will timeout allowing a new select") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))

      // get a select 100 ms in the past (normally this is rejected by server)
      fixture.access.put(selectRequest(time = -100)).expectOne()

      fixture.access.put(selectRequest()).expectOne()
    }
  }

  test("Normal Select/Execute/Delete") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.enableCommandRequests()

      val select = fixture.access.put(selectRequest()).expectOne()

      val commandResult = fixture.commandRequest.put(issueCommand()).expectOne()
      commandResult.getStatus should equal(CommandStatus.SUCCESS)

      fixture.access.delete(select).expectOne()
    }
  }

  test("Command Block stops execution") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))

      val cmdBlock = fixture.access.put(selectRequest(mode = AccessMode.BLOCKED)).expectOne()

      val errorMessage = intercept[BadRequestException] {
        fixture.commandRequest.put(issueCommand()).expectOne()
      }.getMessage

      errorMessage should include("Command not selected")
    }
  }

  test("Command Locks have correct user assigned") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01", "cmd02"))

      fixture.contextSource.userName = "user_01"

      val user1Block = fixture.access.put(selectRequest(List("cmd01"))).expectOne()
      user1Block.getUser should equal("user_01")

      fixture.contextSource.userName = "user_02"

      val user2Block = fixture.access.put(selectRequest(List("cmd02"))).expectOne()
      user2Block.getUser should equal("user_02")

      fixture.access.get(selectRequest(List("cmd01"))).expectOne().getUser should equal("user_01")
      fixture.access.get(selectRequest(List("cmd02"))).expectOne().getUser should equal("user_02")
    }
  }

  test("Command Locks block other users executing cmd") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))

      fixture.contextSource.userName = "user_01"

      fixture.access.put(selectRequest()).expectOne()

      fixture.contextSource.userName = "user_02"

      intercept[BadRequestException] {
        fixture.commandRequest.put(issueCommand()).expectOne()
      }
    }
  }

  test("Multiple Execution Cycles") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.enableCommandRequests()

      // Run multiple times, state should be reset
      fixture.selectExecuteDelete()
      fixture.selectExecuteDelete()
      fixture.selectExecuteDelete()
    }
  }

  val allLocksRequest = CommandLock.newBuilder.setId(ReefID.newBuilder.setValue("*")).build
  val allCommandsRequest = UserCommandRequest.newBuilder.setId(ReefID.newBuilder.setValue("*")).build

  ignore("CommandHistory and AccessHistory") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.enableCommandRequests()

      val users = List("user_01", "user_02", "user_03")
      val values = List(100, 120, 88)

      users.zip(values).foreach {
        case (userName, value) =>
          fixture.contextSource.userName = userName
          fixture.selectExecuteDelete(cmdValue = Some(value))
      }

      val locks = fixture.access.get(allLocksRequest).expectMany(users.size)
      locks.map { _.getUser } should equal(users)
      val requests = fixture.commandRequest.get(allCommandsRequest).expectMany(values.size)
      requests.map { _.getCommandRequest.getIntVal } should equal(values)

      // make sure that when there are more result_limit results we get the most recent ones
      val headers = BasicRequestHeaders.empty.setResultLimit(1)
      val mostRecentLock = fixture.access.get(allLocksRequest, headers).expectOne()
      mostRecentLock.getUser should equal(users.last)
      val mostRecentRequest = fixture.commandRequest.get(allCommandsRequest, headers).expectOne()
      mostRecentRequest.getCommandRequest.getIntVal should equal(values.last)
    }
  }

  test("Can't delete command with outstanding select") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))

      val cmdLock = fixture.access.put(selectRequest()).expectOne()

      val errorMessage = intercept[BadRequestException] {
        fixture.deleteCommand("cmd01")
      }.getMessage
      errorMessage should include("active select")

      fixture.access.delete(cmdLock).expectOne()

      fixture.deleteCommand("cmd01")
    }
  }

  ignore("CommandHistory and Locks are cleaned up if command is deleted") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.enableCommandRequests()

      val values = List(100, 120, 88)
      values.foreach { v => fixture.selectExecuteDelete(cmdValue = Some(v)) }
      fixture.removeEndpoint()

      fixture.access.get(allLocksRequest).expectMany(values.size)
      fixture.commandRequest.get(allCommandsRequest).expectMany(values.size)

      fixture.deleteCommand("cmd01")

      fixture.access.get(allLocksRequest).expectMany() should equal(Nil)
      fixture.commandRequest.get(allCommandsRequest).expectMany() should equal(Nil)
    }
  }
}