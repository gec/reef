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

import org.totalgrid.reef.reactor.Reactable
import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.api.{ RequestEnv, ServiceHandlerHeaders }

/**
 * Interface for "coordinator" functionality, allows classes to get given a reactor for delayed
 * actions and a "raw" AMQP factory to hook themselves to custom data channels
 */
trait ProtoServiceCoordinator {

  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Reactable)
}

class ServiceProviderHeaders(e: RequestEnv) extends ServiceHandlerHeaders(e) {

  def userName = e.getString("USER")

  def setUserName(s: String) = e.setHeader("USER", s)
}
object ServiceProviderHeaders {
  implicit def toServiceHeaders(e: RequestEnv) = new ServiceProviderHeaders(e)
}

