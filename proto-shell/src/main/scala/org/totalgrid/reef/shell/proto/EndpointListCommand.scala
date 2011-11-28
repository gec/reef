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

trait EndpointRetrieval { self: ReefCommandSupport =>
  def endpoints(endpointName: String) = {
    endpointName match {
      case "*" => services.getEndpoints().toList
      case _ => services.getEndpointByName(endpointName) :: Nil
    }
  }
}