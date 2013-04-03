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

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.OptionalProtos._

import org.totalgrid.reef.util.Table
import org.totalgrid.reef.client.service.proto.FEP.{ FrontEndProcessor, EndpointConnection }

object EndpointView {
  def printTable(endpoints: List[EndpointConnection]) = {
    Table.printTable(header, endpoints.map(row(_)))
  }

  def header = {
    "Endpoint" :: "Protocol" :: "Auto?" :: "State" :: "Enabled" :: "FrontEnd" :: "Port" :: "Port State" :: "MeasProc?" :: Nil
  }

  def row(a: EndpointConnection) = {
    a.endpoint.name.getOrElse("unknown") ::
      a.endpoint.protocol.getOrElse("unknown") ::
      a.endpoint.autoAssigned.map { _.toString }.getOrElse("-") ::
      a.getState.toString ::
      a.getEnabled.toString ::
      a.frontEnd.appConfig.instanceName.getOrElse("Unassigned") ::
      a.endpoint.channel.name.getOrElse("unknown") ::
      a.endpoint.channel.state.map { _.toString }.getOrElse("unknown") ::
      a.routing.serviceRoutingKey.map { s => true }.getOrElse(false).toString ::
      Nil
  }

  def printProtocolAdapters(adapters: List[FrontEndProcessor]) = {
    Table.printTable(headerAdapters, adapters.map(rowAdapter(_)))
  }

  private def headerAdapters = {
    "Name" :: "Protocol" :: "Online" :: "TimesOutAt" :: "Location" :: "Networks" :: Nil
  }

  private def rowAdapter(f: FrontEndProcessor) = {

    val a = f.getAppConfig

    a.getInstanceName ::
      f.getProtocolsList.toList.mkString(",") ::
      a.getOnline.toString ::
      EventView.timeString(Some(a.getTimesOutAt)) ::
      a.getLocation ::
      a.getNetworksList.toList.mkString(", ") ::
      Nil
  }
}