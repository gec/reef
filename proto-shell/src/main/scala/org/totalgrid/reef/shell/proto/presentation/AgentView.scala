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
package org.totalgrid.reef.shell.proto.presentation

import org.totalgrid.reef.client.service.proto.Auth._

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Table

object AgentView {
  def printAgents(agents: List[Agent]) = {
    Table.printTable(agentHeader, agents.map(agentRow(_)))
  }

  def agentHeader = {
    "Id" :: "Name" :: "PermissionSets" :: Nil
  }

  def agentRow(a: Agent) = {
    a.getUuid.getValue :: a.getName :: a.getPermissionSetsList.toList.map { _.getName }.mkString(",") :: Nil
  }

  def printPermissionSets(permissions: List[PermissionSet]) = {
    Table.printTable(permissionSetHeader, permissions.map(permissionSetRow(_)))
  }

  def permissionSetHeader = {
    "Id" :: "Name" :: "Allows" :: "Denies" :: Nil
  }

  def permissionSetRow(a: PermissionSet) = {
    val permissions = a.getPermissionsList.toList
    val allows = permissions.filter(_.getAllow == true).map { p => p.getVerbList.mkString("[", ",", "]") + " , " + p.getResourceList.mkString("[", ",", "]") }
    val denies = permissions.filter(_.getAllow == false).map { p => p.getVerbList.mkString("[", ",", "]") + " , " + p.getResourceList.mkString("[", ",", "]") }
    a.getUuid.getValue :: a.getName :: allows.mkString(";") :: denies.mkString(";") :: Nil
  }

  def filterResultHeader = {
    "Status" :: "Entity Name" :: "Reason" :: Nil
  }

  def filterResult(result: AuthFilterResult) = {
    val str = if (result.getAllowed) "Allowed" else "Denied"
    str :: result.getEntity.getName :: result.getReason :: Nil
  }

  def printFilterResults(results: List[AuthFilterResult]) = {
    Table.printTable(filterResultHeader, results.map(filterResult))
  }
}