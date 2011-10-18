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
package org.totalgrid.reef.sapi.auth

import org.totalgrid.reef.api.japi.Envelope
import org.totalgrid.reef.api.sapi.client.BasicRequestHeaders


case class AuthDenied(reason: String, status: Envelope.Status)

/**
 *   Interface that acts as a Policy Decision Point (PDP) that can be invoked from multiple locations
 */
trait AuthService {

  /**
   *     @param component Unique system-wide name for component making authorization request
   *     @param action - Component specific action. Could be anything like Rest Verb or CRUD verb
   *     @return None if authorization is given, Some(AuthDenied) otherwise
   */
  def isAuthorized(componentId: String, actionId: String, headers: BasicRequestHeaders): Either[AuthDenied, BasicRequestHeaders]
}

/**
 * Useful for testing where we don't want to worry about authorization
 */
object NullAuthService extends AuthService {

  final override def isAuthorized(componentId: String, actionId: String, headers: BasicRequestHeaders) = Right(headers)

}
