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
package org.totalgrid.reef.httpbridge.servlets

import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.totalgrid.reef.client.proto.SimpleAuth
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.httpbridge.{ ManagedConnection, JsonBridgeConstants }
import org.totalgrid.reef.httpbridge.servlets.helpers.ServletBase

/**
 * handle login requests. Accepts both GETs that are url encoded or POSTs with JSON data format.
 *
 * We want to support the GETs mainly for debugging purposes, makes it easy to grab an auth token
 * using a browser window to test a "non-logging in" application. We also include the auth token
 * as a response Header which may be the easiest way to use it.
 *
 * GET /login?name="system"&password="system" =>
 *   AUTH-TOKEN-UUID
 *
 * POST /login {"name": "system", "password": "system"} =>
 *  {"token":"AUTH-TOKEN-UUID"}
 */
class LoginServlet(connection: ManagedConnection) extends ServletBase {

  import JsonBridgeConstants._

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {
    val userName = Option(req.getParameter("name")).getOrElse(throw new BadRequestException("Must include name parameter."))
    val userPassword = Option(req.getParameter("password")).getOrElse(throw new BadRequestException("Must include password parameter."))

    val authToken = connection.getNewAuthToken(userName, userPassword)

    resp.setHeader(AUTH_HEADER, authToken)
    resp.getOutputStream.print(authToken)
    resp.setHeader(CONTENT_TYPE_HEADER, TEXT_FORMAT)
    resp.setStatus(200)
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = handleErrors(resp) {

    // we use the simple auth proto because it has the exact fields we need in a proto object so
    // we can use the same JSON reading code
    val authRequest = parseInput(req, req.getInputStream, SimpleAuth.AuthRequest.newBuilder).build

    if (!authRequest.hasName || !authRequest.hasPassword) {
      throw new BadRequestException("Must include name and password in posted JSON. " +
        "Expected format: {\"name\":\"user_name\",\"password\":\"user_password\"}")
    }

    val authToken = connection.getNewAuthToken(authRequest.getName, authRequest.getPassword)

    // make a simple proto with only token set so we can use same formatter
    val proto = SimpleAuth.AuthRequest.newBuilder.setToken(authToken).build

    resp.setHeader(AUTH_HEADER, authToken)
    printSingleOutput(req, resp, proto)
    resp.setStatus(200)
  }

}