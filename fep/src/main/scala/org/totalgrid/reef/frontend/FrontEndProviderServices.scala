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

import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection, FrontEndProcessor }
import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.client.service.proto.Model.ReefUUID

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.rpc.util.RpcProvider
import org.totalgrid.reef.client.sapi.rpc.util.RpcProvider

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client.operations.scl.ServiceOperationsProvider
import org.totalgrid.reef.client.{ Promise, Client, SubscriptionResult }

// TODO: Move FrontEndProviderServices functions into service-client ProtocolAdapaterServices
trait FrontEndProviderServices extends AllScadaService {

  def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[EndpointConnection], EndpointConnection]]

  def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor]
}

object FrontEndProviderServices {
  val serviceInfo = RpcProvider(new FrontEndProviderServicesImpl(_), List(classOf[FrontEndProviderServices]))
}

class FrontEndProviderServicesImpl(client: Client)
    extends ServiceOperationsProvider(client) with FrontEndProviderServices with AllScadaServiceImpl {

  override def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[EndpointConnection], EndpointConnection]] = {
    ops.subscription(Descriptors.endpointConnection, "Couldn't subscribe for endpoints assigned to: " + fep.getAppConfig.getInstanceName) { (sub, client) =>
      client.get(EndpointConnection.newBuilder.setFrontEnd(fep).build, sub).map(_.many)
    }
  }

  override def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor] = {
    ops.operation("Failed registering application: " + applicationUuid.getValue + " as frontend") {
      _.put(FrontEndProcessor.newBuilder.setAppConfig(ApplicationConfig.newBuilder.setUuid(applicationUuid)).addAllProtocols(protocols).build).map(_.one)
    }
  }
}