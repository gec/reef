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

import org.totalgrid.reef.util.Logging
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.sapi.service.{ ServiceResponseCallback, ServiceHelpers, AsyncService }
import org.totalgrid.reef.japi.{ ReefServiceException, Envelope, TypeDescriptor }
import org.totalgrid.reef.sapi.client.Response

class ServiceMiddleware[A <: AnyRef](contextSource: RequestContextSource, service: ServiceEntryPoint[A]) extends AsyncService[A] with Logging with ServiceHelpers[A] {

  val descriptor: TypeDescriptor[A] = service.descriptor

  def respond(req: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) = {
    try {
      handleRequest(req, env, callback)
    } catch {
      case px: ReefServiceException =>
        logger.error(px.getMessage, px)
        callback.onResponse(getFailure(req.getId, px.getStatus, px.getMessage))
      case x: Exception =>
        logger.error(x.getMessage, x)
        val msg = x.getMessage + "\n" + x.getStackTraceString
        callback.onResponse(getFailure(req.getId, Envelope.Status.INTERNAL_ERROR, msg))
    }
  }

  private def handleRequest(request: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) {

    def onResponse(response: Response[A]) = callback.onResponse(getResponse(request.getId, response))

    val value = descriptor.deserialize(request.getPayload.toByteArray)

    val contextSourceWithHeaders = new RequestContextSourceWithHeaders(contextSource, env)

    request.getVerb match {
      case Envelope.Verb.GET => service.getAsync(contextSourceWithHeaders, value)(onResponse)
      case Envelope.Verb.PUT => service.putAsync(contextSourceWithHeaders, value)(onResponse)
      case Envelope.Verb.DELETE => service.deleteAsync(contextSourceWithHeaders, value)(onResponse)
      case Envelope.Verb.POST => service.postAsync(contextSourceWithHeaders, value)(onResponse)
    }

  }

}

