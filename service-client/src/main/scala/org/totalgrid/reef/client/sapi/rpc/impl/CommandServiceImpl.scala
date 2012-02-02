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

import org.totalgrid.reef.client.service.proto.Commands.CommandLock
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID, Command }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.sapi.rpc.impl.builders._
import org.totalgrid.reef.client.sapi.rpc.CommandService
import org.totalgrid.reef.client.sapi.client.rpc.framework.{ MultiRequestHelper, HasAnnotatedOperations }
import org.totalgrid.reef.client.service.command.CommandRequestHandler
import org.totalgrid.reef.client.SubscriptionBinding
import org.totalgrid.reef.client.sapi.client.rest.impl.BatchServiceRestOperations

trait CommandServiceImpl extends HasAnnotatedOperations with CommandService {

  override def createCommandExecutionLock(id: Command) = createCommandExecutionLock(id :: Nil)

  override def createCommandExecutionLock(id: Command, expirationTimeMilli: Long) = createCommandExecutionLock(id :: Nil, expirationTimeMilli)

  override def createCommandExecutionLock(ids: List[Command]) = {
    ops.operation("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandLockRequestBuilders.allowAccessForCommands(ids)).map(_.one)
    }
  }

  override def createCommandExecutionLock(ids: List[Command], expirationTimeMilli: Long) = {
    ops.operation("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandLockRequestBuilders.allowAccessForCommands(ids, Option(expirationTimeMilli))).map(_.one)
    }
  }

  override def deleteCommandLock(id: ReefID) = ops.operation("Couldn't delete command lock with id: " + id) {
    _.delete(CommandLockRequestBuilders.getForId(id)).map(_.one)
  }

  override def deleteCommandLock(ca: CommandLock) = ops.operation("Couldn't delete command lock: " + ca) {
    _.delete(CommandLockRequestBuilders.getForId(ca.getId)).map(_.one)

  }

  override def clearCommandLocks() = ops.operation("Couldn't delete all command locks in system.") {
    _.delete(CommandLockRequestBuilders.getAll).map(_.many)
  }

  override def executeCommandAsControl(id: Command) = ops.operation("Couldn't execute control: " + id.getName) {
    _.put(UserCommandRequestBuilders.executeControl(id)).map(_.one.map(_.getResult))
  }

  override def executeCommandAsSetpoint(id: Command, value: Double) = {
    ops.operation("Couldn't execute setpoint: " + id.getName + " with double value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map(_.one.map(_.getResult))
    }
  }

  override def executeCommandAsSetpoint(id: Command, value: Int) = {
    ops.operation("Couldn't execute setpoint: " + id.getName + " with integer value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map(_.one.map(_.getResult))
    }
  }

  override def executeCommandAsSetpoint(id: Command, value: String) = {
    ops.operation("Couldn't execute setpoint: " + id.getName + " with string value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map(_.one.map(_.getResult))
    }
  }

  override def createCommandDenialLock(ids: List[Command]) = {
    ops.operation("Couldn't create denial lock on ids: " + ids.map { _.getName }) {
      _.put(CommandLockRequestBuilders.blockAccessForCommands(ids)).map(_.one)
    }
  }

  override def getCommandLocks() = ops.operation("Couldn't get all command locks in system") {
    _.get(CommandLockRequestBuilders.getAll).map(_.many)
  }

  override def getCommandLockById(id: ReefID) = ops.operation("Couldn't get command lock with id: " + id) {
    _.get(CommandLockRequestBuilders.getForId(id)).map(_.one)
  }

  override def findCommandLockOnCommand(id: Command) = {
    ops.operation("couldn't find command lock for command: " + id.getName) {
      _.get(CommandLockRequestBuilders.getByCommand(id)).map { _.oneOrNone }
    }
  }

  override def getCommandLocksOnCommands(ids: List[Command]) = {
    ops.operation("Couldn't get command locks for: " + ids.map { _.getName }) {
      _.get(CommandLockRequestBuilders.getByCommands(ids)).map(_.many)
    }
  }

  override def getCommandHistory() = ops.operation("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForId("*")).map(_.many)
  }

  override def getCommandHistory(cmd: Command) = ops.operation("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForCommand(cmd)).map(_.many)
  }

  override def getCommands() = ops.operation("Couldn't get all commands") {
    _.get(CommandRequestBuilders.getAll).map(_.many)
  }

  override def getCommandByName(name: String) = ops.operation("Couldn't get command with name: " + name) {
    _.get(CommandRequestBuilders.getByEntityName(name)).map(_.one)
  }

  override def getCommandByUuid(uuid: ReefUUID) = ops.operation("Couldn't get command with uuid: " + uuid) {
    _.get(CommandRequestBuilders.getByEntityId(uuid)).map(_.one)
  }

  override def getCommandsByNames(names: List[String]) = ops.operation("Couldn't get commands with names: " + names) { _ =>
    batchGets(names.map { CommandRequestBuilders.getByEntityName(_) })
  }

  override def getCommandsByUuids(uuids: List[ReefUUID]) = ops.operation("Couldn't get commands with uuids: " + uuids) { _ =>
    batchGets(uuids.map { CommandRequestBuilders.getByEntityId(_) })
  }

  override def getCommandsOwnedByEntity(parentUuid: ReefUUID) = {
    ops.operation("Couldn't find commands owned by parent entity: " + parentUuid.getValue) {
      _.get(CommandRequestBuilders.getOwnedByEntityWithUuid(parentUuid)).map(_.many)
    }
  }

  override def getCommandsBelongingToEndpoint(endpointUuid: ReefUUID) = {
    ops.operation("Couldn't find commands sourced by endpoint: " + endpointUuid.getValue) {
      _.get(CommandRequestBuilders.getSourcedByEndpoint(endpointUuid)).map(_.many)
    }
  }

  override def getCommandsThatFeedbackToPoint(pointUuid: ReefUUID) = {
    ops.operation("Couldn't find commands that feedback to point: " + pointUuid.getValue) { session =>

      val entity = EntityRequestBuilders.getPointsFeedbackCommands(pointUuid)
      val entityList = session.get(entity).map { _.one.map { EntityRequestBuilders.extractChildrenUuids(_) } }

      val batchClient = new BatchServiceRestOperations(client)
      def getCommandWithUuid(uuid: ReefUUID) = batchClient.get(CommandRequestBuilders.getByEntityId(uuid)).map(_.one)
      MultiRequestHelper.batchScatterGatherQuery(client, entityList, getCommandWithUuid _, batchClient.flush _)
    }
  }

  override def bindCommandHandler(endpointUuid: ReefUUID, handler: CommandRequestHandler) = {
    ops.operation("Couldn't find endpoint connection for endpoint: " + endpointUuid.getValue) { session =>
      import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointConnection }
      import org.totalgrid.reef.client.AddressableDestination
      import net.agileautomata.executor4s._

      val connectionFuture = session.get(EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setUuid(endpointUuid)).build)

      connectionFuture.flatMap {
        _.one match {
          case Success(connection) =>
            val destination = new AddressableDestination(connection.getRouting.getServiceRoutingKey)
            val service = new EndpointCommandHandlerImpl(handler)
            connectionFuture.replicate[Result[SubscriptionBinding]](Success(client.bindService(service, client, destination, false)))
          case fail: Failure =>
            connectionFuture.asInstanceOf[Future[Result[SubscriptionBinding]]]
        }
      }
    }
  }
}