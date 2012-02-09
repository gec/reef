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

import presentation.EndpointView

import org.apache.felix.gogo.commands.{ Argument, Command }

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection

@Command(scope = "endpoint", name = "list", description = "Prints endpoint connection information")
class EndpointListCommand extends ReefCommandSupport {

  def doCommand() = {
    EndpointView.printTable(services.getEndpointConnections.toList)
  }
}

@Command(scope = "endpoint", name = "disable", description = "Disables an endpoint")
class EndpointDisableCommand extends ReefCommandSupport with EndpointRetrieval {

  @Argument(index = 0, name = "name", description = "Endpoint name. Use \"*\" for all endpoints.", required = true, multiValued = false)
  var endpointName: String = null

  def doCommand() = {
    EndpointView.printTable(endpoints(endpointName).map { c => services.disableEndpointConnection(c.getUuid) })
  }
}

@Command(scope = "endpoint", name = "enable", description = "Enables an endpoint")
class EndpointEnableCommand extends ReefCommandSupport with EndpointRetrieval {

  @Argument(index = 0, name = "name", description = "Endpoint name. Use \"*\" for all endpoints.", required = true, multiValued = false)
  var endpointName: String = null

  def doCommand() = {
    EndpointView.printTable(endpoints(endpointName).map { c => services.enableEndpointConnection(c.getUuid) })
  }
}

// TODO: remove endpoint:changestate command
@Command(scope = "endpoint", name = "changestate", description = "Force an endpoint into a different state (experts only!)")
class EndpointChangeStateCommand extends ReefCommandSupport with EndpointRetrieval {

  @Argument(index = 0, name = "name", description = "Endpoint name. Use \"*\" for all endpoints.", required = true, multiValued = false)
  var endpointName: String = null

  @Argument(index = 1, name = "state", description = "The desired state we want the endpoint in. COMMS_UP, COMMS_DOWN or COMMS_ERROR", required = false, multiValued = false)
  var stateStr: String = "COMMS_DOWN"

  def doCommand() = {

    val state = EndpointConnection.State.valueOf(stateStr)
    if (state == null) println("State must be one of: COMMS_UP, COMMS_DOWN or COMMS_ERROR")
    else {
      EndpointView.printTable(endpoints(endpointName).map { c =>
        val connection = services.getEndpointConnectionByUuid(c.getUuid)
        services.alterEndpointConnectionState(connection.getId, state)
      })
    }
  }
}

trait EndpointRetrieval { self: ReefCommandSupport =>
  def endpoints(endpointName: String) = {
    endpointName match {
      case "*" => services.getEndpoints().toList
      case _ => services.getEndpointByName(endpointName) :: Nil
    }
  }
}