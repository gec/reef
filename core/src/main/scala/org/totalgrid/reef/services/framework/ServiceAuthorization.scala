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

package org.totalgrid.reef.services.framework

import org.totalgrid.reef.api.auth.{ IAuthService, AuthDenied }
import org.totalgrid.reef.api.{ RequestEnv, UnauthorizedException }

trait HasComponentId {
  val componentId: String
}

trait HasAuthActions {
  def actions: List[String] = Nil
}

trait HasAuthService {
  protected val authService: IAuthService
}

trait AuthorizesCreate extends HasCreate with HasAuthActions with HasComponentId with HasAuthService {

  override def actions = actionForCreate :: super.actions

  /**
   * this is overridable, I.E. controls could use the action "EXECUTE"
   */
  protected val actionForCreate = "CREATE"

  abstract override protected def preCreate(proto: ServiceType, headers: RequestEnv): ServiceType = {

    authService.isAuthorized(componentId, actionForCreate, headers) match {
      case Some(AuthDenied(reason, _)) => throw new UnauthorizedException(reason)
      case None => super.preCreate(proto, headers)
    }

  }

}