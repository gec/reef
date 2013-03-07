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

import org.totalgrid.reef.event.SystemEventSink
import org.totalgrid.reef.client.{ RequestHeaders, Client }
import org.totalgrid.reef.services.authz.AuthzService
import scala.collection.mutable
import org.totalgrid.reef.models.Agent
import org.totalgrid.reef.client.exception.UnauthorizedException
import org.totalgrid.reef.client.registration.{ EventPublisher, ServiceRegistration }
import com.weiglewilczek.slf4s.Logging

/**
 * the request context is handed through the service call chain. It allows us to make the services and models
 * stateless objects by concentrating all of the per-request state in one object. It also provides the sinks for
 * common operations that all services use (subscription publishing, operation enqueuing and event generation).
 *
 * TODO: refactor auth service to use requestContext
 */
trait RequestContext {

  /**
   * the operation buffer is used to delay the creation and publishing of service events (ADDED,MODIFIED,DELETED) until
   * the appropriate time.
   */
  def operationBuffer: OperationBuffer

  /**
   * subscription handler that handles the publish and bind calls. Differs from the original eventPublisher since it will
   * accept any service event and lookup the exchange rather than needing a different publisher for each object type
   */
  def eventPublisher: EventPublisher

  /**
   * for publishing system messages (System.LogOn, Subsystem.Starting) etc, publishes these messages immediately and
   * even if the rest of the transaction rolls back
   */
  def eventSink: SystemEventSink

  def client: Client

  def serviceRegistration: ServiceRegistration

  /**
   * request headers as received from the client
   */
  def getHeaders: RequestHeaders

  /**
   * Change the request headers and return the modified version
   */
  def modifyHeaders(modify: RequestHeaders => RequestHeaders): RequestHeaders

  // per-request store for cached objects
  private lazy val requestObjects = mutable.Map.empty[String, Object]

  /**
   * store a value in the per request store
   */
  def set(key: String, value: Object) = requestObjects.put(key, value)

  /**
   * pull a value out of the store and cast it to a particular class
   */
  def get[A](key: String) = requestObjects.get(key).map { _.asInstanceOf[A] }

  /**
   * auth service
   */
  def auth: AuthzService

  /**
   * get agent associated with this request or throw an unauthorized exception
   * if not authorized
   */
  def agent: Agent = get[Agent]("agent").getOrElse(throw new UnauthorizedException("Not logged in"))
}

/**
 * a RequestContextSource provides a transaction function which will generate a new RequestContext and once the client
 * function has completed make sure to cleanup after the transaction (publish subscription messages etc). transaction
 * should be called as high up in the call chain as possible
 */
trait RequestContextSource {
  def transaction[A](f: RequestContext => A): A
}

/**
 * wrapper class that takes a source and merges in some extra RequestEnv headers before the transaction
 */
class RequestContextSourceWithHeaders(contextSource: RequestContextSource, headers: RequestHeaders)
    extends RequestContextSource {
  def transaction[A](f: (RequestContext) => A) = {
    contextSource.transaction { context =>
      context.modifyHeaders(_.merge(headers))
      context.auth.prepare(context)
      f(context)
    }
  }
}

