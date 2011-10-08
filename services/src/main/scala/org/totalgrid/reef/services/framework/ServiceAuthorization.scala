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

import org.totalgrid.reef.sapi.auth.{ AuthService, AuthDenied, NullAuthService }
import org.totalgrid.reef.japi.UnauthorizedException
import org.totalgrid.reef.sapi.BasicRequestHeaders
import org.totalgrid.reef.sapi.service.HasComponentId

trait HasAuthActions {
  def actions: List[String] = Nil
}

trait HasAuthService {
  var authService: AuthService = NullAuthService
}

trait AuthTranslator extends HasAuthActions with HasComponentId with HasAuthService {

  protected def authorize(context: RequestContext, componentId: String, action: String, headers: BasicRequestHeaders): BasicRequestHeaders = {
    authService.isAuthorized(componentId, action, headers) match {
      case Left(AuthDenied(reason, _)) => throw new UnauthorizedException(reason)
      case Right(headers) => headers
    }
  }
}

trait AuthorizesCreate extends CanAuthorizeCreate with AuthTranslator {

  override def actions = actionForCreate :: super.actions
  protected val actionForCreate = "create"

  final override def authorizeCreate(context: RequestContext, request: ServiceType): ServiceType = {
    context.modifyHeaders(authorize(context, componentId, actionForCreate, _))
    request
  }
}

trait AuthorizesRead extends CanAuthorizeRead with AuthTranslator {

  override def actions = actionForRead :: super.actions
  protected val actionForRead = "read"

  final override def authorizeRead(context: RequestContext, request: ServiceType): ServiceType = {
    authorize(context, componentId, actionForRead, context.getHeaders)
    request
  }
}

trait AuthorizesUpdate extends CanAuthorizeUpdate with AuthTranslator {

  override def actions = actionForUpdate :: super.actions
  protected val actionForUpdate = "update"

  final override def authorizeUpdate(context: RequestContext, request: ServiceType): ServiceType = {
    authorize(context, componentId, actionForUpdate, context.getHeaders)
    request
  }
}

trait AuthorizesDelete extends CanAuthorizeDelete with AuthTranslator {

  override def actions = actionForDelete :: super.actions
  protected val actionForDelete = "delete"

  final override def authorizeDelete(context: RequestContext, request: ServiceType): ServiceType = {
    authorize(context, componentId, actionForDelete, context.getHeaders)
    request
  }
}