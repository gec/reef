/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request.impl

import scala.collection.JavaConversions._

import org.totalgrid.reef.api.request.AgentService
import org.totalgrid.reef.proto.Auth._

trait AgentServiceImpl extends ReefServiceBaseClass with AgentService {
  def getAgent(name: String) = {
    ops.getOneOrThrow(Agent.newBuilder.setName(name).build)
  }

  def getAgents() = {
    ops.getOrThrow(Agent.newBuilder.setUid("*").build)
  }

  def getPermissionSets() = {
    ops.getOrThrow(PermissionSet.newBuilder.setUid("*").build)
  }

  def createNewAgent(name: String, password: String, permissionSets: java.util.List[String]) = {
    val agent = Agent.newBuilder.setName(name).setPassword(password)
    permissionSets.toList.foreach { pName => agent.addPermissionSets(PermissionSet.newBuilder.setName(pName).build) }
    ops.putOneOrThrow(agent.build)
  }

  def deleteAgent(agent: Agent) = {
    ops.deleteOneOrThrow(agent)
  }

  def setAgentPassword(agent: Agent, newPassword: String) = {
    ops.putOneOrThrow(agent.toBuilder.setPassword(newPassword).build)
  }
}