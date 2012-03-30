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

import org.totalgrid.reef.client.sapi.rpc.LoginService
import org.totalgrid.reef.client.sapi.client.rpc.framework.HasAnnotatedOperations
import org.totalgrid.reef.client.service.proto.Model.ReefID
import org.totalgrid.reef.client.service.proto.Auth.{ Agent, AuthToken }

trait LoginServiceImpl extends HasAnnotatedOperations with LoginService {

  private def builder(includeRevoked: Boolean) = {
    val b = AuthToken.newBuilder
    if (!includeRevoked) b.setRevoked(false)
    b
  }
  private def agent(name: String) = {
    Agent.newBuilder.setName(name).build
  }

  override def getOwnLogins(includeRevoked: Boolean) = ops.operation("Couldn't get own logins.") {
    _.get(builder(includeRevoked).build).map(_.many)
  }

  override def revokeOwnLogins() = ops.operation("Couldn't delete own logins.") {
    _.delete(builder(true).setRevoked(false).build).map(_.many)
  }

  override def getLogins(includeRevoked: Boolean) = ops.operation("Couldn't get all logins.") {
    _.get(builder(includeRevoked).setAgent(agent("*")).build).map(_.many)
  }

  override def getLoginsByAgent(includeRevoked: Boolean, agentName: String) = ops.operation("Couldn't get logins for: " + agentName) {
    _.get(builder(includeRevoked).setAgent(agent(agentName)).build).map(_.many)
  }

  override def getLoginsByClientVersion(includeRevoked: Boolean, clientVersion: String) = ops.operation("Couldn't get logins for clients: " + clientVersion) {
    _.get(builder(includeRevoked).setAgent(agent("*")).setClientVersion(clientVersion).build).map(_.many)
  }

  override def revokeLoginById(id: ReefID) = ops.operation("Couldn't revoke login: " + id.getValue) {
    _.get(AuthToken.newBuilder.setId(id).build).map(_.one)
  }

  override def revokeLoginByAgent(agentName: String) = ops.operation("Couldn't revoke logins for: " + agentName) {
    _.delete(AuthToken.newBuilder.setRevoked(false).setAgent(agent(agentName)).build).map(_.many)
  }
}