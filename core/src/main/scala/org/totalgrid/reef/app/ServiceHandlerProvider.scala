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
package org.totalgrid.reef.app

import com.google.protobuf.GeneratedMessage

import org.totalgrid.reef.app.ServiceHandler._
import org.totalgrid.reef.messaging.ProtoRegistry

// Trait allows type to be negotiated between user and provider, and hidden from factory steps in between
class ServiceHandlerProvider(registry: ProtoRegistry, handler: ServiceHandler) extends SubscriptionProvider {

  def subscribe[A <: AnyRef](
    parseFrom: Array[Byte] => A,
    searchKey: A,
    respHandler: ResponseHandler[A],
    eventHandler: EventHandler[A]) = {
    handler.addService(registry, 5000, parseFrom, searchKey, respHandler, eventHandler)
  }
}
