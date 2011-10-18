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

import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.japi.client.SubscriptionResult
import org.totalgrid.reef.api.proto.FEP.{ CommEndpointConnection, FrontEndProcessor }
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.api.protocol.api.CommandHandler
import org.totalgrid.reef.api.sapi.client.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.api.proto.Model.ReefUUID

import org.totalgrid.reef.api.proto.Application.ApplicationConfig
import org.totalgrid.reef.api.proto.Descriptors

import scala.collection.JavaConversions._

import org.totalgrid.reef.sapi.request.framework.{ ClientSourceProxy, ReefServiceBaseClass }
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.sapi.AddressableDestination
import org.totalgrid.reef.executor.Executor

trait FrontEndProviderServices extends AllScadaServiceImpl {
  def bindCommandHandler(connection: CommEndpointConnection, commandHandler: CommandHandler): Cancelable

  def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]]

  def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor]
}

class FrontEndProviderServicesImpl(protected val clientSource: AllScadaServiceImpl, factory: AMQPSyncFactory, exe: Executor)
    extends FrontEndProviderServices with ReefServiceBaseClass with ClientSourceProxy {

  def bindCommandHandler(connection: CommEndpointConnection, commandHandler: CommandHandler): Cancelable = {
    val destination = AddressableDestination(connection.getRouting.getServiceRoutingKey)
    val service = new SingleEndpointCommandService(commandHandler)

    val closeable = factory.bindService(service.descriptor.id, service.respond, exe, destination)

    new Cancelable {
      def cancel() = closeable.close()
    }
  }

  def subscribeToEndpointConnectionsForFrontEnd(fep: FrontEndProcessor): Promise[SubscriptionResult[List[CommEndpointConnection], CommEndpointConnection]] = {
    ops.operation("Couldn't subscribe for endpoints assigned to: " + fep.getAppConfig.getInstanceName) { session =>
      useSubscription(session, Descriptors.commEndpointConnection.getKlass) { sub =>
        session.get(CommEndpointConnection.newBuilder.setFrontEnd(fep).build, sub).map { _.expectMany() }
      }
    }
  }

  def registerApplicationAsFrontEnd(applicationUuid: ReefUUID, protocols: List[String]): Promise[FrontEndProcessor] = {
    ops.operation("Failed registering application: " + applicationUuid.getUuid + " as frontend") {
      _.put(FrontEndProcessor.newBuilder.setAppConfig(ApplicationConfig.newBuilder.setUuid(applicationUuid)).addAllProtocols(protocols).build).map { _.expectOne }
    }
  }
}