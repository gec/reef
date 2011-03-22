/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.totalgrid.reef.api.ReefServiceException

/**
 * All requests (except for authorization token requests) require that the agent has acquired an
 * "Auth Token" and is sending it with every request. This auth token contains the user name and
 * all of the permissions available to that user. The token itself is a cryptographically secure string
 * that is an unguessable, unforgeable and must be kept secret by the client. Anyone with access to
 * that token will have the full capabilities of that user until it expires or is revoked. When a user
 * is finished using an auth token it should be deleted to minimize this danger.
 *
 * Every request to the services needs to have an auth token in the headers. They can be sent with each
 * client request, the ISession interface has overloads to attach headers to each request. It is easiest
 * to attach the auth token to the underlying session using the setAuthToken function.
 *
 * TODO: add setAuthToken function on ISession
 */
trait AuthTokenService {
  /**
   * create an authorization token for the user with all available permissions "checked out". If the password
   * or username is wrong this method will throw an exception without indicating which was wrong.
   * @return authToken string
   */
  @throws(classOf[ReefServiceException])
  def createNewAuthorizationToken(user: String, password: String): String

  /**
   * revoke the authToken string. This means all future requests using this auth token will fail.
   */
  @throws(classOf[ReefServiceException])
  def deleteAuthorizationToken(token: String)
}