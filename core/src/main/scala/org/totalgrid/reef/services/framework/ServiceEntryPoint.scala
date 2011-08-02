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

import org.totalgrid.reef.sapi.client.Response
import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.japi.{ ReefServiceException, Envelope }

import org.totalgrid.reef.sapi.service._
import org.totalgrid.reef.util.Logging

trait AsyncContextRestGet extends HasServiceType {
  def getAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noGet[ServiceType])
}

trait AsyncContextRestDelete extends HasServiceType {
  def deleteAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noDelete[ServiceType])
}

trait AsyncContextRestPost extends HasServiceType {
  def postAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noPost[ServiceType])
}

trait AsyncContextRestPut extends HasServiceType {
  def putAsync(context: RequestContext, req: ServiceType, env: RequestEnv)(callback: Response[ServiceType] => Unit): Unit = callback(RestResponses.noPut[ServiceType])
}

trait AsyncContextRestService extends AsyncContextRestGet with AsyncContextRestDelete with AsyncContextRestPost with AsyncContextRestPut

trait ServiceEntryPoint[A <: AnyRef] extends ServiceTypeIs[A] with ServiceDescriptor[A] with AsyncContextRestService with Logging with ServiceHelpers[A] with AsyncService[A] {

  def respond(req: Envelope.ServiceRequest, env: RequestEnv, callback: ServiceResponseCallback) = {
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

  private def handleRequest(request: Envelope.ServiceRequest, env: RequestEnv, callback: ServiceResponseCallback) {

    def onResponse(response: Response[ServiceType]) = callback.onResponse(getResponse(request.getId, response))

    val value = descriptor.deserialize(request.getPayload.toByteArray)

    val context = new SimpleRequestContext

    request.getVerb match {
      case Envelope.Verb.GET => getAsync(context, value, env)(onResponse)
      case Envelope.Verb.PUT => putAsync(context, value, env)(onResponse)
      case Envelope.Verb.DELETE => deleteAsync(context, value, env)(onResponse)
      case Envelope.Verb.POST => postAsync(context, value, env)(onResponse)
    }

  }

}