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

import org.totalgrid.reef.client.sapi.service.AsyncService

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.Connection
import org.totalgrid.reef.services.framework.{ ServiceContainer, ServerSideProcess }
import net.agileautomata.executor4s.Executor
import org.totalgrid.reef.client.AnyNodeDestination

/**
 * sets up the "production" ServiceContainer for the service providers
 */
class ServiceContext(connection: Connection, executor: Executor) extends ServiceContainer with Logging {

  def addCoordinator(coord: ServerSideProcess) {
    coord.startProcess(executor)
  }

  def attachService(endpoint: AsyncService[_]): AsyncService[_] = {
    // bind to the "well known" public queue that is statically routed from the well known exchange
    connection.getServiceRegistration.bindService(endpoint, endpoint.descriptor, new AnyNodeDestination, true)
    endpoint
  }
}

