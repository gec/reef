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
import scala.collection.JavaConversions._

import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest, CommandAccess }
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommEndpointConnection, EndpointOwnership }

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.messaging.mock.AMQPFixture
import org.totalgrid.reef.executor.mock.InstantExecutor
import org.totalgrid.reef.util.EmptySyncVar

import CommandAccess._

import org.totalgrid.reef.services._
import org.totalgrid.reef.messaging.BasicSessionPool
import org.totalgrid.reef.sapi.{ RequestEnv, client, AddressableDestination }
import org.totalgrid.reef.japi.Envelope

import client.Response

import org.totalgrid.reef.sapi.service.SyncServiceBase
import org.totalgrid.reef.proto.Model.{ CommandType, Command }

import org.totalgrid.reef.services.framework.HeadersRequestContext

@RunWith(classOf[JUnitRunner])
class CommandRequestServicesIntegration
    extends EndpointRelatedTestBase {

  import ServiceResponseTestingHelpers._
  val env = new RequestEnv()
  env.setUserName("user01")
  implicit val context = new HeadersRequestContext(env)
  import org.totalgrid.reef.services.core.CustomServiceShims._

  class CommandFixture(amqp: AMQPProtoFactory) extends CoordinatorFixture(amqp) {

    val command = new CommandService(modelFac.cmds)
    val commandRequest = new UserCommandRequestService(modelFac.userRequests, new BasicSessionPool(connection))
    val endpointService = new CommunicationEndpointService(modelFac.endpoints)
    val access = new CommandAccessService(modelFac.accesses)

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

      val send = CommEndpointConfig.newBuilder()
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

    val access = new CommandAccessService(modelFac.accesses)

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

  def commandAccessSearch(names: String*) = CommandAccess.newBuilder.addAllCommands(names).build
  def commandAccess(
    name: String = "cmd01",
    mode: AccessMode = AccessMode.ALLOWED,
    time: Long = System.currentTimeMillis + 40000 /*user: String = "user01"*/ ) = {

    CommandAccess.newBuilder
      .addCommands(name)
      .setAccess(mode)
      .setExpireTime(time)
      .build
  }

  def userRequest(
    commandName: String = "cmd01",
    correlationId: Option[String] = None,
    status: Option[CommandStatus] = None): UserCommandRequest = {

    val c = CommandRequest.newBuilder.setName(commandName)
    correlationId.foreach(c.setCorrelationId(_))
    val b = UserCommandRequest.newBuilder
      .setCommandRequest(c)

    status.foreach(b.setStatus(_))

    b.build
  }

  def testCommandSequence(fixture: CommandFixture) = {

    // Send a select (access request)
    val select = commandAccess()
    val selectResult = fixture.access.put(select).expectOne()
    val selectId = selectResult.getUid

    // the 'remote' service that will handle the call
    val service = new SyncServiceBase[UserCommandRequest] {

      val descriptor = Descriptors.userCommandRequest

      override def put(req: UserCommandRequest, env: RequestEnv): Response[UserCommandRequest] =
        Response(Envelope.Status.OK, UserCommandRequest.newBuilder(req).setStatus(CommandStatus.SUCCESS).build :: Nil)
    }

    val conn = fixture.frontEndConnection.get(CommEndpointConnection.newBuilder.setUid("*").build).expectOne()

    // act like the FEP and mark the endpoint as comms_up
    fixture.setEndpointState(conn, CommEndpointConnection.State.COMMS_UP)

    //bind the 'proxied' service that will handle the call
    fixture.connection.bindService(service, AddressableDestination(conn.getRouting.getServiceRoutingKey), reactor = Some(new InstantExecutor {}))

    // Send the user command request
    val cmdReq = userRequest()
    val result = new EmptySyncVar[Response[UserCommandRequest]]
    fixture.commandRequest.putAsync(context, cmdReq) { x => result.update(x) }

    val correctResponse = Response(
      Envelope.Status.OK,
      UserCommandRequest.newBuilder(cmdReq).setStatus(CommandStatus.SUCCESS).build :: Nil)

    result.waitFor { rsp =>
      rsp.status == Envelope.Status.OK &&
        rsp.list.size == 1 &&
        rsp.list.head.getStatus == CommandStatus.SUCCESS
    }

    fixture.access.delete(selectResult).expectOne()
  }

  test("Full") {
    AMQPFixture.mock(true) { amqp: AMQPProtoFactory =>
      val fixture = new CommandFixture(amqp)

      fixture.addCommands(List("cmd01"))
      fixture.addFepAndMeasProc()

      // Run multiple times, state should be reset
      testCommandSequence(fixture)
      testCommandSequence(fixture)
      testCommandSequence(fixture)
    }
  }

}