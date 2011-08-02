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
package org.totalgrid.reef.services

import org.totalgrid.reef.sapi.service.AsyncService

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.executor.{ ReactActorExecutor, LifecycleManager }
import org.totalgrid.reef.util.{ Logging }

/**
 * sets up the "production" ServiceContainer for the service providers
 */
class ServiceContext(manager: LifecycleManager, amqp: AMQPProtoFactory, metrics: MetricsServiceWrapper) extends ServiceContainer with Logging {

  def addCoordinator(coord: ProtoServiceCoordinator) {
    val reactor = new ReactActorExecutor {}
    manager.add(reactor)
    coord.addAMQPConsumers(amqp, reactor)
  }

  def attachService(endpoint: AsyncService[_]): AsyncService[_] = {

    val instrumentedEndpoint = metrics.instrumentCallback(endpoint)

    // each service gets its own actor so a slow service can't block a fast service but
    // a slow query will block the next query to that service
    val serviceReactor = new ReactActorExecutor {}
    manager.add(serviceReactor)

    // bind to the "well known" public queue that is statically routed from the well known exchange
    amqp.bindService(endpoint.descriptor.id, instrumentedEndpoint.respond, competing = true, reactor = Some(serviceReactor))
    instrumentedEndpoint
  }
}

