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
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.service.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest, CommandLock }
import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointConnection, EndpointOwnership }

import CommandLock._

import org.totalgrid.reef.services._
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.sapi.service.SyncServiceBase
import org.totalgrid.reef.client.service.proto.Model.{ CommandType, Command }
import org.totalgrid.reef.client.sapi.client.{ BasicRequestHeaders, Response }
import org.totalgrid.reef.client.sapi.client.rest.Connection

@RunWith(classOf[JUnitRunner])
class CommandRequestServicesIntegration
    extends EndpointRelatedTestBase {

  import ServiceResponseTestingHelpers._

  class CommandFixture(amqp: Connection) extends CoordinatorFixture(amqp) {

    val command = new SyncService(new CommandService(modelFac.cmds), contextSource)
    val commandRequest = new SyncService(new UserCommandRequestService(modelFac.userRequests), contextSource)
    val endpointService = new SyncService(new CommunicationEndpointService(modelFac.endpoints), contextSource)
    val access = new SyncService(new CommandLockService(modelFac.accesses), contextSource)

    def addFepAndMeasProc() {
      addFep("fep", List("benchmark"))
      addMeasProc("meas")
    }

    def addCommands(commands: List[String]) {

      val owns = EndpointOwnership.newBuilder
      commands.foreach { c =>
        val cmdProto = Command.newBuilder().setName(c).setDisplayName(c).setType(CommandType.CONTROL).build
        command.put(cmdProto).expectOne()
        owns.addCommands(c)
      }

      val send = Endpoint.newBuilder()
        .setName("endpoint1").setProtocol("benchmark").setOwnerships(owns).build
      endpointService.put(send).expectOne()
    }

  }

  /*
  class TestRig {
    val events = mutable.Queue[(Envelope.Event, GeneratedMessage)]()
    val rawRequests = mutable.Queue[CommandRequest]()

    val subHandler = new CallbackServiceSubscriptionHandler((event, msg) => events.enqueue((event, msg)))
    val pub = new SingleEventPublisher(subHandler)
    val modelFac = new core.ModelFactories(pub)

    val endpointService = new CommunicationEndpointService(modelFac.endpoints)

    val access = new CommandLockService(modelFac.accesses)

    val mock = new MockConnection {}
    val pool = new SessionPool(mock)

    val userReqs = new UserCommandRequestService(modelFac.userRequests, pool)

    val command = new CommandService(modelFac.cmds)

    def addCommands(commands: List[String]) {

      val owns = EndpointOwnership.newBuilder
      commands.foreach { c => owns.addCommands(c) }

      val send = CommunicationEndpointConfig.newBuilder()
        .setName("endpoint1").setProtocol("benchmark").setOwnerships(owns).build
      one(endpointService.put(send))

      /*
      // Seed with command point
      val device = transaction {
        EQ.findOrCreateEntity("dev1", "LogicalNode")
      }
      commands.foreach { cmdName =>
        val cmd = one(command.put(FepCommand.newBuilder.setName(cmdName).build))
        //println(cmd)
        transaction {
          val cmd_entity = EQ.findEntity(cmd.getEntity).get
          EQ.addEdge(device, cmd_entity, "source")
        }
      }
      */

      many(commands.size, command.get(FepCommand.newBuilder.setName("*").build))
      events.clear()
    }
  }
  */

  def commandAccess(
    name: String = "cmd01",
    mode: AccessMode = AccessMode.ALLOWED,
    time: Long = System.currentTimeMillis + 40000 /*user: String = "user01"*/ ) = {

    CommandLock.newBuilder
      .addCommands(Command.newBuilder.setName(name))
      .setAccess(mode)
      .setExpireTime(time)
      .build
  }

  def userRequest(
    commandName: String = "cmd01",
    correlationId: Option[String] = None,
    status: Option[CommandStatus] = None): UserCommandRequest = {

    val c = CommandRequest.newBuilder.setCommand(Command.newBuilder.setName(commandName))
    correlationId.foreach(c.setCorrelationId(_))
    val b = UserCommandRequest.newBuilder
      .setCommandRequest(c)

    status.foreach(b.setStatus(_))

    b.build
  }

  def testCommandSequence(fixture: CommandFixture, runNum: Int) = {

    // Send a select (access request)
    val select = commandAccess()
    val selectResult = fixture.access.put(select).expectOne()
    val selectId = selectResult.getId

    // the 'remote' service that will handle the call
    val service = new SyncServiceBase[UserCommandRequest] {

      val descriptor = Descriptors.userCommandRequest

      override def put(req: UserCommandRequest, env: BasicRequestHeaders): Response[UserCommandRequest] =
        Response(Envelope.Status.OK, UserCommandRequest.newBuilder(req).setStatus(CommandStatus.SUCCESS).setErrorMessage("RunNum: " + runNum).build :: Nil)
    }

    val conn = fixture.frontEndConnection.get(EndpointConnection.newBuilder.setId("*").build).expectOne()

    // act like the FEP and mark the endpoint as comms_up
    fixture.setEndpointState(conn, EndpointConnection.State.COMMS_UP)

    //bind the 'proxied' service that will handle the call
    fixture.bindCommandHandler(service, conn.getRouting.getServiceRoutingKey)

    // Send the user command request
    val cmdReq = userRequest()

    fixture.commandRequest.get(cmdReq).expectMany(runNum)

    val result = fixture.commandRequest.put(cmdReq).expectOne()

    result.getStatus should equal(CommandStatus.SUCCESS)

    // make sure if we ask for the result of the command later it is correctly set
    val storedResults = fixture.commandRequest.get(cmdReq).expectMany(runNum + 1)
    storedResults.foreach { _.getStatus should equal(CommandStatus.SUCCESS) }
    storedResults.foreach { _.getErrorMessage should include("RunNum: ") }

    fixture.access.delete(selectResult).expectOne()
  }

  test("Full") {
    ConnectionFixture.mock() { amqp =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))
      fixture.addFepAndMeasProc()

      // TODO: figure out "Unexpected request response" errors

      // Run multiple times, state should be reset
      testCommandSequence(fixture, 0)
      testCommandSequence(fixture, 1)
      testCommandSequence(fixture, 2)
    }
  }

}