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

import org.totalgrid.reef.models.ApplicationSchema

import org.totalgrid.reef.api.service.sync.ServiceDescriptor

import org.squeryl.PrimitiveTypeMode._

import org.totalgrid.reef.services.ServiceProviderHeaders._
import org.totalgrid.reef.api.{ Envelope, RequestEnv }
import org.totalgrid.reef.metrics.MetricsHooks

/// the metrics collected on any single service request
class AuthTokenMetrics(baseName: String = "") extends MetricsHooks {
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
class AuthTokenVerifier[A](real: ServiceDescriptor[A], exchange: String, metrics: AuthTokenMetrics) extends ServiceDescriptor[A] {

  override val descriptor = real.descriptor

  def respond(req: Envelope.ServiceRequest, env: RequestEnv): Envelope.ServiceResponse = {
    metrics.countHook(1)
    val authFailed = metrics.timerHook {
      transaction {
        checkAuth(req, env)
      }
    }
    if (authFailed.isDefined) {
      metrics.failHook(1)
      return authFailed.get
    }
    return real.respond(req, env)
  }

  /// we either return a failure response or None if it passed all of the auth checks 
  private def checkAuth(req: Envelope.ServiceRequest, env: RequestEnv): Option[Envelope.ServiceResponse] = {

    val authTokens = env.authTokens

    if (authTokens.size == 0) {
      return failMessage(req, Envelope.Status.BAD_REQUEST, "No auth tokens in envelope header")
    }

    // lookup the tokens that are not expired
    val now = System.currentTimeMillis

    val tokens = ApplicationSchema.authTokens.where(t => t.token in authTokens and t.expirationTime.~ > now).toList
    if (tokens.size == 0) {
      return failMessage(req, Envelope.Status.UNAUTHORIZED, "All tokens unknown or expired")
    }

    val verb = req.getVerb.toString.toLowerCase
    val permissions = tokens.map(token => token.permissionSets.value.toList.map(ps => ps.permissions.value).flatten).flatten.distinct
    // select only the permissions that either say this resource + verb exactly or are wildcarded
    val relevant = permissions.filter(p => (p.resource == "*" || p.resource == exchange) && (p.verb == "*" || p.verb == verb))

    val userName = tokens.head.agent.value.name

    if (relevant.size == 0) {
      return failMessage(req, Envelope.Status.UNAUTHORIZED, "Access to resource: " + req.getVerb + ":" + exchange + " by agent: " + userName + " not allowed.")
    }

    val denied = relevant.find(p => p.allow == false)
    if (denied.isDefined) {
      return failMessage(req, Envelope.Status.UNAUTHORIZED, "Access to resource: " + req.getVerb + ":" + exchange + " explictly denied by permission: " + denied.get)
    }
    env.setUserName(userName)
    // passed all checks, therefore its an authorized request
    None
  }

  private def failMessage(req: Envelope.ServiceRequest, status: Envelope.Status, msg: String) = {
    val rsp = Envelope.ServiceResponse.newBuilder.setId(req.getId)
    rsp.setStatus(status)
    rsp.setErrorMessage(msg)
    Some(rsp.build)
  }
}