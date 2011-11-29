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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.clientapi.sapi.client.rest.impl.RestHelpers
import org.totalgrid.reef.clientapi.proto.Envelope
import org.totalgrid.reef.clientapi.proto.Envelope.Status._
import org.totalgrid.reef.clientapi.proto.Envelope.Verb._
import org.totalgrid.reef.proto.Descriptors
import java.util.UUID
import org.totalgrid.reef.clientapi.proto.Envelope.{ SelfIdentityingServiceRequest, BatchServiceRequest }
import org.totalgrid.reef.clientapi.types.TypeDescriptor
import org.totalgrid.reef.clientapi.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.persistence.squeryl.{ DbInfo, DbConnector }
import org.totalgrid.reef.services.ServiceBootstrap
import org.totalgrid.reef.proto.Model.{ CommandType, Command }
import org.totalgrid.reef.proto.Commands.CommandAccess

import scala.collection.JavaConversions._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterEach, BeforeAndAfterAll, FunSuite }
import org.totalgrid.reef.clientapi.exceptions.BadRequestException

@RunWith(classOf[JUnitRunner])
class BatchServiceRequestServiceTest extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  override def beforeAll() {
    DbConnector.connect(DbInfo.loadInfo("../org.totalgrid.reef.test.cfg"))
  }
  override def beforeEach() {
    ServiceBootstrap.resetDb
  }

  val env = BasicRequestHeaders.empty.setUserName("user")
  val deps = new ServiceDependenciesDefaults()
  val contextSource = new MockRequestContextSource(deps, env)

  val modelFac = new ModelFactories(deps)
  val services = List(
    new CommandService(modelFac.cmds),
    new CommandAccessService(modelFac.accesses))

  val service = new SyncService(new BatchServiceRequestService(services), contextSource)

  test("Put and Get works") {
    val putAndGet = getStatuses(makeBatch(command(), command(GET)), OK)
    putAndGet should equal(List(CREATED, OK))

    val getNextTransaction = getStatuses(makeBatch(command(GET)), OK)
    getNextTransaction should equal(List(OK))
  }

  test("Failure rolls back the whole transaction") {
    intercept[BadRequestException] {
      service.post(makeBatch(command(), commandAccess(PUT, "fakeCommand"))).expectOne
    }

    intercept[BadRequestException] {
      service.post(makeBatch(commandAccess(PUT))).expectOne
    }
  }

  def getStatuses(batch: BatchServiceRequest, status: Envelope.Status) = {
    val response = service.post(batch).expectOne(status)

    response.getRequestsList.toList.map { _.getResponse.getStatus }
  }

  def command(verb: Envelope.Verb = PUT, commandName: String = "cmd01") = {
    val c = Command.newBuilder.setName(commandName).setType(CommandType.CONTROL).setDisplayName(commandName)
    makeRequest(verb, c.build, Descriptors.command)
  }

  def commandAccess(verb: Envelope.Verb = PUT, name: String = "cmd01") = {
    val ca = CommandAccess.newBuilder.addCommands(Command.newBuilder.setName(name)).setAccess(CommandAccess.AccessMode.ALLOWED)
      .setExpireTime(System.currentTimeMillis + 40000)
    makeRequest(verb, ca.build, Descriptors.commandAccess)
  }

  def makeRequest[A](verb: Envelope.Verb, usr: A, descriptor: TypeDescriptor[A]): SelfIdentityingServiceRequest = {

    val uuid = UUID.randomUUID().toString

    val request1 = RestHelpers.buildServiceRequest(verb, usr, descriptor, uuid, env)

    SelfIdentityingServiceRequest.newBuilder.setExchange(descriptor.id).setRequest(request1).build
  }

  def makeBatch(parts: SelfIdentityingServiceRequest*) = {
    val b = BatchServiceRequest.newBuilder
    parts.foreach { b.addRequests(_) }
    b.build
  }
}