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
package org.totalgrid.reef.services.framework

import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders
import org.totalgrid.reef.client.sapi.service.{ ServiceResponseCallback, ServiceHelpers, AsyncService }
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.types.TypeDescriptor
import org.totalgrid.reef.client.sapi.client.Response

/**
 * parses a ServiceRequest and calls an asynchronous service handler
 */
class ServiceMiddleware[A <: AnyRef](contextSource: RequestContextSource, service: ServiceEntryPoint[A]) extends AsyncService[A] with Logging with ServiceHelpers[A] {

  val descriptor: TypeDescriptor[A] = service.descriptor

  def respond(req: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) = {
    ServiceHelpers.catchErrors(req, callback) {
      handleRequest(req, env, callback)
    }
  }

  private def handleRequest(request: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) {

    def onResponse(response: Response[A]) = callback.onResponse(getResponse(request.getId, response))

    val value = descriptor.deserialize(request.getPayload.toByteArray)

    val contextSourceWithHeaders = new RequestContextSourceWithHeaders(contextSource, env)

    service.respondAsync(request.getVerb, contextSourceWithHeaders, value)(onResponse)
  }

}

