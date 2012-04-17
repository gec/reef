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

import presentation.{ ApplicationView, EndpointView }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.client.exception.BadRequestException
import org.apache.felix.gogo.commands.{ Argument, Option => GogoOption, Command }

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

@Command(scope = "endpoint", name = "assign", description = "Force an endpoint into a different state (experts only!)")
class EndpointAssignCommand extends ReefCommandSupport {

  @Argument(index = 0, name = "name", description = "Endpoint name. ", required = true, multiValued = false)
  var endpointName: String = null

  @GogoOption(name = "--auto", description = "Switch endpoint to be autoAssigned", required = false, multiValued = false)
  var switchToAutoAssign: Boolean = false

  @GogoOption(name = "--manual", description = "Switch endpoint to be manually assigned", required = false, multiValued = false)
  var switchToManualAssign: Boolean = false

  @GogoOption(name = "--fep", description = "Name of the application we want to be the FEP.", required = false, multiValued = false)
  var fepName: String = null

  def doCommand() = {
    if (switchToAutoAssign && fepName != null) throw new Exception("Cannot use both --auto and set an fep at same time.")
    if (fepName != null) switchToManualAssign = true
    if (switchToAutoAssign && switchToManualAssign) throw new Exception("Cannot use both --auto and --manual options")

    val endpoint = services.getEndpointByName(endpointName)

    if (switchToAutoAssign && !endpoint.getAutoAssigned) {
      services.setEndpointAutoAssigned(endpoint.getUuid, true)
    }
    if (switchToManualAssign && endpoint.getAutoAssigned) {
      services.setEndpointAutoAssigned(endpoint.getUuid, false)
    }
    if (fepName != null) {
      val fep = services.getApplicationByName(fepName)
      ApplicationView.printInspect(fep)
      services.setEndpointConnectionAssignedProtocolAdapter(endpoint.getUuid, fep.getUuid)
    }
    val connection = services.getEndpointConnectionByUuid(endpoint.getUuid)
    EndpointView.printTable(List(connection))
  }
}

@Command(scope = "endpoint", name = "adapters", description = "List protocol adapters")
class EndpointAdaptersCommand extends ReefCommandSupport {

  def doCommand() = {
    EndpointView.printProtocolAdapters(services.getProtocolAdapters.toList)
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