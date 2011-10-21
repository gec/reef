package org.totalgrid.reef.client.sapi.rpc.impl

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
import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.OptionalProtos._
import org.totalgrid.reef.proto.Auth._
import org.totalgrid.reef.proto.Model.ReefUUID

import org.totalgrid.reef.client.sapi.rpc.AgentService
import org.totalgrid.reef.api.sapi.client.rpc.framework.HasAnnotatedOperations

trait AgentServiceImpl extends HasAnnotatedOperations with AgentService {
  override def getAgent(name: String) = ops.operation("Couldn't get agent with name: " + name) {
    _.get(Agent.newBuilder.setName(name).build).map(_.one)
  }

  override def getAgents() = ops.operation("Couldn't get list of all agents") {
    _.get(Agent.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).map(_.many)
  }

  override def getPermissionSets() = ops.operation("Couldn't get list of all permission sets") {
    _.get(PermissionSet.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).map(_.many)
  }

  override def getPermissionSet(name: String) = ops.operation("Couldn't get permission set with name: " + name) {
    _.get(PermissionSet.newBuilder.setName(name).build).map(_.one)
  }

  override def createPermissionSet(name: String, permissions: List[Permission]) = {
    ops.operation("Couldn't create permission set with name: " + name + " and " + permissions.size + " permissions") {
      _.put(PermissionSet.newBuilder.setName(name).addAllPermissions(permissions).build).map(_.one)
    }
  }

  override def deletePermissionSet(permissionSet: PermissionSet) = {
    ops.operation("Couldn't delete permission set: " + permissionSet.name) {
      _.delete(permissionSet).map(_.one)
    }
  }

  override def createNewAgent(name: String, password: String, permissionSets: List[String]) = {
    ops.operation("Couldn't create agent with name: " + name + " permission set names: " + permissionSets) { session =>
      val agent = Agent.newBuilder.setName(name).setPassword(password)
      permissionSets.toList.foreach { pName => agent.addPermissionSets(PermissionSet.newBuilder.setName(pName).build) }
      session.put(agent.build).map(_.one)
    }
  }

  override def deleteAgent(agent: Agent) = {
    ops.operation("Couldn't delete agent with name: " + agent.name + " uuid: " + agent.uuid) {
      _.delete(agent).map(_.one)
    }
  }

  override def setAgentPassword(agent: Agent, newPassword: String) = {
    ops.operation("Couldn't change password for agent name: " + agent.name + " uuid: " + agent.uuid) {
      _.put(agent.toBuilder.setPassword(newPassword).build).map(_.one)
    }
  }
}