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
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection, FrontEndProcessor }
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.api.protocol.api.CommandHandler
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.proto.Model.ReefUUID

import org.totalgrid.reef.proto.Application.ApplicationConfig

import scala.collection.JavaConversions._

import org.totalgrid.reef.clientapi.sapi.client.Promise
import org.totalgrid.reef.clientapi.{ SubscriptionResult, AddressableDestination }

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.clientapi.sapi.client.rest.Client
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.ApiBase
import org.totalgrid.reef.client.sapi.rpc.AllScadaService

trait FrontEndProviderServices extends AllScadaService {
  def bindCommandHandler(connection: CommEndpointConnection, commandHandler: CommandHandler): Cancelable

  def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]]

  def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor]
}

class FrontEndProviderServicesImpl(client: Client)
    extends ApiBase(client) with FrontEndProviderServices with AllScadaServiceImpl {

  def bindCommandHandler(connProto: CommEndpointConnection, commandHandler: CommandHandler): Cancelable = {
    val destination = new AddressableDestination(connProto.getRouting.getServiceRoutingKey)
    val service = new SingleEndpointCommandService(commandHandler)

    val closeable = client.bindService(service, client, destination, false)

    new Cancelable {
      def cancel() = closeable.cancel()
    }
  }

  def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]] = {
    ops.subscription(Descriptors.commEndpointConnection, "Couldn't subscribe for endpoints assigned to: " + fep.getAppConfig.getInstanceName) { (sub, client) =>
      client.get(CommEndpointConnection.newBuilder.setFrontEnd(fep).build, sub).map(_.many)
    }
  }

  def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor] = {
    ops.operation("Failed registering application: " + applicationUuid.getValue + " as frontend") {
      _.put(FrontEndProcessor.newBuilder.setAppConfig(ApplicationConfig.newBuilder.setUuid(applicationUuid)).addAllProtocols(protocols).build).map(_.one)
    }
  }
}