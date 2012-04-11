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
package org.totalgrid.reef.shell.proto

import org.totalgrid.reef.shell.proto.presentation.AuthTokenView

import scala.collection.JavaConversions._
import org.apache.felix.gogo.commands.{ Argument, Option => GogoOption, Command }
import org.totalgrid.reef.client.service.proto.Model.ReefID

@Command(scope = "login", name = "list", description = "List active logins")
class AuthTokenListCommand extends ReefCommandSupport {

  @GogoOption(name = "--own", description = "Search for our own tokens", required = false, multiValued = false)
  var ownTokens: Boolean = false

  @GogoOption(name = "--agent", description = "Search by agent name", required = false, multiValued = false)
  var agentName: String = null

  @GogoOption(name = "--version", description = "Search by clientVersion", required = false, multiValued = false)
  var clientVersion: String = null

  //  @GogoOption(name = "--location", description = "Search by login location", required = false, multiValued = false)
  //  var location: String = null

  @GogoOption(name = "--stat", description = "So per-agent summary", required = false, multiValued = false)
  var stats: Boolean = false

  @GogoOption(name = "--revoked", description = "Include revoked tokens", required = false, multiValued = false)
  var includeRevoked: Boolean = false

  override def doCommand(): Unit = {

    val results = (Option(agentName), Option(clientVersion)) match {
      case (Some(_), Some(_)) => throw new Exception("Can't search by clientVersion and agentName at same time")
      case (Some(agent), None) => services.getLoginsByAgent(includeRevoked, agent)
      case (None, Some(version)) => services.getLoginsByClientVersion(includeRevoked, version)
      case _ => services.getLogins(includeRevoked)
    }

    if (stats) {
      AuthTokenView.printAuthTokenStats(results.toList)
    } else {
      AuthTokenView.printAuthTokens(results.toList)
    }
  }
}

@Command(scope = "login", name = "revoke", description = "Revoke an auth token")
class AuthTokenRevokeCommand extends ReefCommandSupport {

  @GogoOption(name = "--others", description = "Revoke all of this agents other tokens", required = false, multiValued = false)
  var revokeOthers: Boolean = false

  @GogoOption(name = "--id", description = "Revoke a specific auth token", required = false, multiValued = false)
  var id: String = null

  @GogoOption(name = "--agent", description = "Revoke all tokens for a specific agent", required = false, multiValued = false)
  var agentName: String = null

  override def doCommand(): Unit = {

    val results = (revokeOthers, Option(id), Option(agentName)) match {
      case (true, None, None) => services.revokeOwnLogins().toList
      case (false, Some(delId), None) => List(services.revokeLoginById(ReefID.newBuilder.setValue(delId).build))
      case (false, None, Some(agent)) => services.revokeLoginByAgent(agent).toList
      case _ => throw new Exception("must use --others , --id or --agent options")
    }

    AuthTokenView.printAuthTokens(results)
  }
}

