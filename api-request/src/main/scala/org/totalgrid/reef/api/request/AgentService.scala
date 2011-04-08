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
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Auth._
import org.totalgrid.reef.api.ReefServiceException

trait AgentService {

  /**
   * @param name of agent to find
   * @return the agent requested or throws exception
   */
  @throws(classOf[ReefServiceException])
  def getAgent(name: String): Agent

  /**
   * @return list of all agents
   */
  @throws(classOf[ReefServiceException])
  def getAgents(): java.util.List[Agent]

  /**
   * Creates (or overwrites) an agent and grants them access to the named PermissionSets
   *
   * @param name agent name
   * @param password password for agent, must obey systems password rules
   * @param permissionSetNames list of permissions sets we want to assign to the user
   * @return the newly created agent object
   */
  @throws(classOf[ReefServiceException])
  def createNewAgent(name: String, password: String, permissionSetNames: java.util.List[String]): Agent

  /**
   * @param agent the agent to delete
   * @return the deleted agent
   */
  @throws(classOf[ReefServiceException])
  def deleteAgent(agent: Agent): Agent

  /**
   * Updates the agent password
   *
   * @param agent the agent to update
   * @param newPassword the new password, must obey systems password rules
   */
  @throws(classOf[ReefServiceException])
  def setAgentPassword(agent: Agent, newPassword: String)

  /**
   * @return list of all of the possible permission sets
   */
  @throws(classOf[ReefServiceException])
  def getPermissionSets(): java.util.List[PermissionSet]

  /**
   * @param name of PermissionSet
   * @return the permissionset with matching name or an exception is thrown
   */
  @throws(classOf[ReefServiceException])
  def getPermissionSet(name: String): PermissionSet

  /**
   * @param name descriptive name for the PermissionSet
   * @param permissions list of allows and denies we want to create
   * @return the created PermissionSet or throws an exception
   */
  @throws(classOf[ReefServiceException])
  def createPermissionSet(name: String, permissions: java.util.List[Permission]): PermissionSet

  /**
   * @param permission the PermissionSet to delete
   * @return the deleted PermissionSet
   */
  @throws(classOf[ReefServiceException])
  def deletePermissionSet(permission: PermissionSet): PermissionSet
}