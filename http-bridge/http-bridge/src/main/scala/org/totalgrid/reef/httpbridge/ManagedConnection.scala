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
package org.totalgrid.reef.httpbridge

import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.exception.ReefServiceException

/**
 * simple trait the servlet needs around a reconnecting connection factory
 */
trait ManagedConnection {

  /**
   * gets an authenticated client if we have a valid connection to the server, throws an exception otherwise
   */
  @throws(classOf[ReefServiceException])
  def getAuthenticatedClient(authToken: String): Client

  /**
   * get a new auth token
   */
  @throws(classOf[ReefServiceException])
  def getNewAuthToken(userName: String, userPassword: String): String

  /**
   * Get the shared auth token (if configured) for un authenticated users
   */
  def getSharedBridgeAuthToken(): Option[String]
}