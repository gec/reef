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
import org.totalgrid.reef.proto.Model.ReefUUID

trait AgentServiceImpl extends ReefServiceBaseClass with AgentService {
  override def getAgent(name: String) = ops {
    _.get(Agent.newBuilder.setName(name).build).await().expectOne
  }

  override def getAgents() = ops {
    _.get(Agent.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).await().expectMany()
  }

  override def getPermissionSets() = ops {
    _.get(PermissionSet.newBuilder.setUuid(ReefUUID.newBuilder.setUuid("*")).build).await().expectMany()
  }

  override def getPermissionSet(name: String) = ops {
    _.get(PermissionSet.newBuilder.setName(name).build).await().expectOne
  }

  override def createPermissionSet(name: String, permissions: java.util.List[Permission]) = ops {
    _.put(PermissionSet.newBuilder.setName(name).addAllPermissions(permissions).build).await().expectOne
  }

  override def deletePermissionSet(permissionSet: PermissionSet) = ops {
    _.delete(permissionSet).await().expectOne
  }

  override def createNewAgent(name: String, password: String, permissionSets: java.util.List[String]) = ops { session =>
    val agent = Agent.newBuilder.setName(name).setPassword(password)
    permissionSets.toList.foreach { pName => agent.addPermissionSets(PermissionSet.newBuilder.setName(pName).build) }
    session.put(agent.build).await().expectOne
  }

  override def deleteAgent(agent: Agent) = ops {
    _.delete(agent).await().expectOne
  }

  override def setAgentPassword(agent: Agent, newPassword: String) = ops {
    _.put(agent.toBuilder.setPassword(newPassword).build).await().expectOne
  }
}