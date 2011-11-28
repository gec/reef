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

import org.totalgrid.reef.proto.Commands.CommandAccess
import org.totalgrid.reef.proto.Model.{ ReefID, ReefUUID, Command }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.sapi.rpc.impl.builders._
import org.totalgrid.reef.client.sapi.rpc.CommandService
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.rpc.commands.CommandRequestHandler

trait CommandServiceImpl extends HasAnnotatedOperations with CommandService {

  override def createCommandExecutionLock(id: Command) = createCommandExecutionLock(id :: Nil)

  override def createCommandExecutionLock(id: Command, expirationTimeMilli: Long) = createCommandExecutionLock(id :: Nil, expirationTimeMilli)

  override def createCommandExecutionLock(ids: List[Command]) = {
    ops.operation("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids)).map(_.one)
    }
  }

  override def createCommandExecutionLock(ids: List[Command], expirationTimeMilli: Long) = {
    ops.operation("Couldn't get command execution lock for: " + ids.map { _.getName }) {
      _.put(CommandAccessRequestBuilders.allowAccessForCommands(ids, Option(expirationTimeMilli))).map(_.one)
    }
  }

  override def deleteCommandLock(uid: ReefID) = ops.operation("Couldn't delete command lock with uid: " + uid) {
    _.delete(CommandAccessRequestBuilders.getForUid(uid)).map(_.one)
  }

  override def deleteCommandLock(ca: CommandAccess) = ops.operation("Couldn't delete command lock: " + ca) {
    _.delete(CommandAccessRequestBuilders.getForUid(ca.getUid)).map(_.one)

  }

  override def clearCommandLocks() = ops.operation("Couldn't delete all command locks in system.") {
    _.delete(CommandAccessRequestBuilders.getAll).map(_.many)
  }

  override def executeCommandAsControl(id: Command) = ops.operation("Couldn't execute control: " + id) {
    _.put(UserCommandRequestBuilders.executeControl(id)).map(_.one.map(_.getStatus))
  }

  override def executeCommandAsSetpoint(id: Command, value: Double) = {
    ops.operation("Couldn't execute setpoint: " + id + " with double value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map(_.one.map(_.getStatus))
    }
  }

  override def executeCommandAsSetpoint(id: Command, value: Int) = {
    ops.operation("Couldn't execute setpoint: " + id + " with integer value: " + value) {
      _.put(UserCommandRequestBuilders.executeSetpoint(id, value)).map(_.one.map(_.getStatus))
    }
  }

  override def createCommandDenialLock(ids: List[Command]) = {
    ops.operation("Couldn't create denial lock on ids: " + ids) {
      _.put(CommandAccessRequestBuilders.blockAccessForCommands(ids)).map(_.one)
    }
  }

  override def getCommandLocks() = ops.operation("Couldn't get all command locks in system") {
    _.get(CommandAccessRequestBuilders.getAll).map(_.many)
  }

  override def getCommandLock(uid: ReefID) = ops.operation("Couldn't get command lock with uid: " + uid) {
    _.get(CommandAccessRequestBuilders.getForUid(uid)).map(_.one)
  }

  override def findCommandLockOnCommand(id: Command) = {
    ops.operation("couldn't find command lock for command: " + id) {
      _.get(CommandAccessRequestBuilders.getByCommand(id)).map { _.oneOrNone }
    }
  }

  override def getCommandLocksOnCommands(ids: List[Command]) = {
    ops.operation("Couldn't get command locks for: " + ids) {
      _.get(CommandAccessRequestBuilders.getByCommands(ids)).map(_.many)
    }
  }

  override def getCommandHistory() = ops.operation("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForUid("*")).map(_.many)
  }

  override def getCommandHistory(cmd: Command) = ops.operation("Couldn't get command history") {
    _.get(UserCommandRequestBuilders.getForName(cmd.getName)).map(_.many)
  }

  override def getCommands() = ops.operation("Couldn't get all commands") {
    _.get(CommandRequestBuilders.getAll).map(_.many)
  }

  override def getCommandByName(name: String) = ops.operation("Couldn't get command with name: " + name) {
    _.get(CommandRequestBuilders.getByEntityName(name)).map(_.one)
  }

  override def getCommandsOwnedByEntity(parentUuid: ReefUUID) = {
    ops.operation("Couldn't find commands owned by parent entity: " + parentUuid.getUuid) {
      _.get(CommandRequestBuilders.getOwnedByEntityWithUuid(parentUuid)).map(_.many)
    }
  }

  override def getCommandsBelongingToEndpoint(endpointUuid: ReefUUID) = {
    ops.operation("Couldn't find commands sourced by endpoint: " + endpointUuid.getUuid) {
      _.get(CommandRequestBuilders.getSourcedByEndpoint(endpointUuid)).map(_.many)
    }
  }

  override def getCommandsThatFeedbackToPoint(pointUuid: ReefUUID) = {
    ops.operation("Couldn't find commands that feedback to point: " + pointUuid.getUuid) { client =>

      val entity = EntityRequestBuilders.getPointsFeedbackCommands(pointUuid)
      val entityList = client.get(entity).map { _.one.map { EntityRequestBuilders.extractChildrenUuids(_) } }

      def getCommandWithUuid(uuid: ReefUUID) = client.get(CommandRequestBuilders.getByEntityUid(uuid)).map(_.one)

      MultiRequestHelper.scatterGatherQuery(entityList, getCommandWithUuid _)
    }
  }

  override def bindCommandHandler(endpointUuid: ReefUUID, handler: CommandRequestHandler) = {
    ops.operation("Couldn't find endpoint connection for endpoint: " + endpointUuid.getUuid) { session =>
      import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommEndpointConnection }
      import org.totalgrid.reef.clientapi.AddressableDestination
      import net.agileautomata.executor4s._

      val connectionFuture = session.get(CommEndpointConnection.newBuilder.setEndpoint(CommEndpointConfig.newBuilder.setUuid(endpointUuid)).build)

      connectionFuture.flatMap {
        _.one match {
          case Success(connection) =>
            val destination = new AddressableDestination(connection.getRouting.getServiceRoutingKey)
            val service = new EndpointCommandHandlerImpl(handler)

            // TODO: use defined future
            val f2 = client.future[Result[Cancelable]]
            f2.set(Success(client.bindService(service, client, destination, true)))
            f2
          case fail: Failure =>
            connectionFuture.asInstanceOf[Future[Result[Cancelable]]]
        }
      }
    }
  }
}