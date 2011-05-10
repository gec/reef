/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.services

import org.totalgrid.reef.api.service.IServiceAsync
import org.totalgrid.reef.api.auth.IAuthService

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.proto.ReefServicesList
import org.totalgrid.reef.reactor.{ ReactActor, LifecycleManager }
import org.totalgrid.reef.util.{ Logging }

import org.totalgrid.reef.util.BuildEnv.ConnInfo

import org.totalgrid.reef.measurementstore.MeasurementStoreFinder

/**
 * sets up the "production" ServiceContainer for the service providers
 */
class ServiceContext(amqp: AMQPProtoFactory, measInfo: ConnInfo, serviceConfiguration: ServiceOptions, auth: IAuthService) extends LifecycleManager with ServiceContainer with Logging {

  private val components = ServiceBootstrap.bootstrapComponents(amqp)
  private val container = new AuthAndMetricsServiceWrapper(components, serviceConfiguration)

  // default lifecycles to add
  this.add(List(amqp, components.heartbeatActor))

  private val measStore = MeasurementStoreFinder.getInstance(measInfo, this.add)

  // all the actual services are created here
  private val providers = new ServiceProviders(components, measStore, serviceConfiguration, auth)

  val services = this.attachServices(providers.services)

  this.addCoordinator(providers.coordinators)

  def addCoordinator(coord: ProtoServiceCoordinator) {
    val reactor = new ReactActor {}
    this.add(reactor)
    coord.addAMQPConsumers(components.amqp, reactor)
  }

  def attachService(endpoint: IServiceAsync[_]): IServiceAsync[_] = {
    val exchange = ReefServicesList.getServiceInfo(endpoint.descriptor.getKlass).exchange
    val instrumentedEndpoint = container.instrumentCallback(exchange, endpoint)

    // each service gets its own actor so a slow service can't block a fast service but
    // a slow query will block the next query to that service
    val serviceReactor = new ReactActor {}
    this.add(serviceReactor)

    // bind to the "well known" public queue that is statically routed from the well known exchange
    components.amqp.bindService(exchange, instrumentedEndpoint.respond, competing = true, reactor = Some(serviceReactor))
    instrumentedEndpoint
  }

}
