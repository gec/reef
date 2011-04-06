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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.proto.Auth._

import scala.collection.JavaConversions._

object AgentView {
  def printAgents(agents: List[Agent]) = {
    Table.printTable(agentHeader, agents.map(agentRow(_)))
  }

  def agentHeader = {
    "Id" :: "Name" :: "PermissionSets" :: Nil
  }

  def agentRow(a: Agent) = {
    a.getUid :: a.getName :: a.getPermissionSetsList.toList.mkString(",") :: Nil
  }

  def printPermissionSets(agents: List[PermissionSet]) = {
    Table.printTable(permissionSetHeader, agents.map(permissionSetRow(_)))
  }

  def permissionSetHeader = {
    "Id" :: "Name" :: "Permissions" :: Nil
  }

  def permissionSetRow(a: PermissionSet) = {
    a.getUid :: a.getName :: a.getPermissionsList.toList.mkString(",") :: Nil
  }
}