/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services.core

import org.scalatest.{ FunSuite, BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.squeryl.{ Schema, Table, KeyedEntity }
import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.proto.Envelope
import org.totalgrid.reef.models.RunTestsInsideTransaction
import com.google.protobuf.GeneratedMessage
import org.totalgrid.reef.services.framework._
import org.totalgrid.reef.proto.Model.{ Command => FepCommand }
import org.totalgrid.reef.proto.Commands.{ CommandStatus, CommandRequest, UserCommandRequest }
import org.totalgrid.reef.proto.Commands.{ CommandResponse, CommandAccess }
import org.totalgrid.reef.models.{ ApplicationSchema, Command => FepCommandModel }
import org.totalgrid.reef.models.{ UserCommandModel, CommandAccessModel }
import org.totalgrid.reef.persistence.squeryl.{ DbConnector, DbInfo }
import CommandAccess._
import Envelope._
import org.totalgrid.reef.protoapi.RequestEnv
import scala.collection.mutable
import org.totalgrid.reef.services._
import org.totalgrid.reef.messaging.serviceprovider.{ SilentEventPublishers, ServiceEventPublishers, ServiceSubscriptionHandler }

class CallbackServiceSubscriptionHandler(f: (Envelope.Event, GeneratedMessage) => Unit) extends ServiceSubscriptionHandler {
  def publish(event: Envelope.Event, resp: GeneratedMessage, key: String) = f(event, resp)

  def bind(subQueue: String, key: String) = {}
}

class SingleEventPublisher(subHandler: ServiceSubscriptionHandler) extends ServiceEventPublishers {
  def getEventSink[T <: GeneratedMessage](klass: Class[T]): ServiceSubscriptionHandler = subHandler
}

@RunWith(classOf[JUnitRunner])
class CommandRequestServicesIntegration
    extends FunSuite
    with ShouldMatchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with RunTestsInsideTransaction {

  import ServiceResponseTestingHelpers._

  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("test"))
  }
  override def beforeEach() {
    transaction { ApplicationSchema.reset }
  }

  class TestRig {
    val events = mutable.Queue[(Envelope.Event, GeneratedMessage)]()
    val rawRequests = mutable.Queue[CommandRequest]()

    val subHandler = new CallbackServiceSubscriptionHandler((event, msg) => events.enqueue((event, msg)))
    val pub = new SingleEventPublisher(subHandler)

    val commandFac = new CommandServiceModelFactory(pub)
    val accessFac = new CommandAccessServiceModelFactory(pub, commandFac)
    val userFac = new UserCommandRequestServiceModelFactory(pub, commandFac, accessFac)

    val access = new CommandAccessService(accessFac)
    val userReqs = new UserCommandRequestService(userFac)

    val command = new CommandService(new CommandServiceModelFactory(new SilentEventPublishers()))
  }
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

  def workingRequest(r: TestRig) = {
    val reqEnv = new RequestEnv(Map("USER" -> List("user01")))

    // Send a select (access request)
    val select = commandAccess()
    val selectResult = r.access.put(select, reqEnv)
    selectResult.status should equal(Envelope.Status.CREATED)

    val selectId = selectResult.result.head.getUid

    // We should receive a new select event and a modify on the command
    r.events.dequeue match {
      case (Event.ADDED, msg: CommandAccess) => msg.getUid should equal(selectId)
      case _ => assert(false)
    }
    r.events.dequeue match {
      case (Event.MODIFIED, msg: FepCommand) => // TODO: modified but not reflected in proto
      case _ => assert(false)
    }

    // Send the user command request, we should get a new executing command event
    val cmdReq = userRequest()
    r.userReqs.put(cmdReq, reqEnv).status should equal(Envelope.Status.CREATED)
    r.events.dequeue match {
      case (Event.ADDED, msg: UserCommandRequest) =>
        msg.getStatus should equal(CommandStatus.EXECUTING)
        r.rawRequests.enqueue(msg.getCommandRequest)
      case x => println(x); assert(false)
    }

    // Request should have been sent to the "FEP"
    r.rawRequests.length should equal(1)
    val cmdToFep = r.rawRequests.dequeue
    cmdToFep.getName should equal("cmd01")
    cmdToFep.hasCorrelationId should equal(true)

    r.userReqs.put(UserCommandRequest.newBuilder.setCommandRequest(cmdToFep).setStatus(CommandStatus.SUCCESS).build)

    // Command request should be a success
    r.events.dequeue match {
      case (Event.MODIFIED, msg: UserCommandRequest) => msg.getStatus should equal(CommandStatus.SUCCESS)
      case x => println(x); assert(false)
    }

    r.access.delete(CommandAccess.newBuilder.setUid(selectId).build).status should equal(Envelope.Status.DELETED)

    r.events.dequeue match {
      case (Event.REMOVED, msg: CommandAccess) => msg.getUid should equal(selectId)
      case _ => assert(false)
    }
    r.events.dequeue match {
      case (Event.MODIFIED, msg: FepCommand) => // TODO: modified but not reflected in proto
      case _ => assert(false)
    }
  }

  test("Full") {
    val r = new TestRig

    // Seed with command point
    val device = transaction {
      EQ.findOrCreateEntity("dev1", "LogicalNode")
    }
    val cmd = one(r.command.put(FepCommand.newBuilder.setName("cmd01").build))
    transaction {
      val cmd_entity = EQ.findEntity(cmd.getEntity).get
      EQ.addEdge(device, cmd_entity, "source")
    }

    // Run multiple times, state should be reset
    workingRequest(r)
    workingRequest(r)
    workingRequest(r)
  }

}