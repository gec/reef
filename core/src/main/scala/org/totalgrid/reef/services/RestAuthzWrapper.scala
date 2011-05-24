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

import org.totalgrid.reef.sapi.RequestEnv
import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.sapi.auth.AuthService
import org.totalgrid.reef.sapi.service.{ AsyncService, ServiceResponseCallback }
import org.totalgrid.reef.sapi.auth.AuthDenied
import org.totalgrid.reef.metrics.MetricsHooks

/// the metrics collected on any single service request
class RestAuthzMetrics(baseName: String = "") extends MetricsHooks {
  /// how many requests handled
  lazy val countHook = counterHook(baseName + "Auths")
  /// errors counted
  lazy val failHook = counterHook(baseName + "AuthFails")
  /// time of service requests
  lazy val timerHook = timingHook[Option[Envelope.ServiceResponse]](baseName + "AuthLookup")
}

/**
 * wraps the request to the service with a function that looks up the permissions for the agent
 * based on the auth_tokens in the envelope and allows/denies based on the permissions the agent has
 */
class RestAuthzWrapper[A](service: AsyncService[A], metrics: RestAuthzMetrics, auth: AuthService) extends AsyncService[A] {

  override val descriptor = service.descriptor

  def respond(req: Envelope.ServiceRequest, env: RequestEnv, callback: ServiceResponseCallback) {
    metrics.countHook(1)
    metrics.timerHook {
      checkAuth(req, env)
    } match {
      case Some(rsp) => callback.onResponse(rsp) //callback immediately with the failure
      case None => service.respond(req, env, callback) // invoke normally
    }
  }

  /// we either return a failure response or None if it passed all of the auth checks 
  private def checkAuth(req: Envelope.ServiceRequest, env: RequestEnv): Option[Envelope.ServiceResponse] = {
    auth.isAuthorized(service.descriptor.id, req.getVerb.toString.toLowerCase, env) match {
      case Some(AuthDenied(reason, status)) =>
        val rsp = Envelope.ServiceResponse.newBuilder.setId(req.getId)
        rsp.setStatus(status)
        rsp.setErrorMessage(reason)
        Some(rsp.build)
      case None => None
    }
  }
}