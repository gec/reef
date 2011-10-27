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

import org.totalgrid.reef.proto.FEP.CommEndpointConnection
import org.totalgrid.reef.proto.OptionalProtos._

import org.totalgrid.reef.util.Table

object EndpointView {
  def printTable(endpoints: List[CommEndpointConnection]) = {
    Table.printTable(header, endpoints.map(row(_)))
  }

  def header = {
    "Endpoint" :: "Protocol" :: "State" :: "Enabled" :: "FrontEnd" :: "Port" :: "Port State" :: Nil
  }

  def row(a: CommEndpointConnection) = {
    a.endpoint.name.getOrElse("unknown") ::
      a.endpoint.protocol.getOrElse("unknown") ::
      a.getState.toString ::
      a.getEnabled.toString ::
      a.frontEnd.appConfig.instanceName.getOrElse("Unassigned") ::
      a.endpoint.channel.name.getOrElse("unknown") ::
      a.endpoint.channel.state.map { _.toString }.getOrElse("unknown") ::
      Nil
  }
}