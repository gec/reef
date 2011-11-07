/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.clientapi.sapi.service

import org.totalgrid.reef.clientapi.proto.Envelope
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.clientapi.sapi.client.{ BasicRequestHeaders, Response }

trait AsyncServiceBase[A <: AnyRef] extends AsyncService[A]
    with ServiceHelpers[A]
    with AsyncRestService
    with ServiceTypeIs[A]
    with Logging {

  /* overloaded helpers */

  def getAsync(req: A)(callback: Response[A] => Unit): Unit = getAsync(req, BasicRequestHeaders.empty)(callback)

  def putAsync(req: A)(callback: Response[A] => Unit): Unit = putAsync(req, BasicRequestHeaders.empty)(callback)

  def deleteAsync(req: A)(callback: Response[A] => Unit): Unit = deleteAsync(req, BasicRequestHeaders.empty)(callback)

  def postAsync(req: A)(callback: Response[A] => Unit): Unit = postAsync(req, BasicRequestHeaders.empty)(callback)

  /* Implement AsyncService */

  def respond(req: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) = {
    ServiceHelpers.catchErrors(req, callback) {
      handleRequest(req, env, callback)
    }
  }

  private def handleRequest(request: Envelope.ServiceRequest, env: BasicRequestHeaders, callback: ServiceResponseCallback) {

    def onResponse(response: Response[A]) = callback.onResponse(getResponse(request.getId, response))

    val value = descriptor.deserialize(request.getPayload.toByteArray)

    request.getVerb match {
      case Envelope.Verb.GET => getAsync(value, env)(onResponse)
      case Envelope.Verb.PUT => putAsync(value, env)(onResponse)
      case Envelope.Verb.DELETE => deleteAsync(value, env)(onResponse)
      case Envelope.Verb.POST => postAsync(value, env)(onResponse)
    }
  }

}
