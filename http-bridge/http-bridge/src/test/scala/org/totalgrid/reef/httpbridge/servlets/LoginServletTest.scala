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

import org.mockito.Mockito

import org.totalgrid.reef.httpbridge.JsonBridgeConstants._
import org.totalgrid.reef.client.exception.UnauthorizedException
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LoginServletTest extends BaseServletTest {

  var service: LoginServlet = null

  override def beforeEach() {
    super.beforeEach()
    service = new LoginServlet(connection)
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)
  }

  val goodJSONRequest = "{\"name\":\"" + userName + "\",\"password\":\"" + password + "\"}"

  test("GET: Missing parameters") {

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(NAME_PARAMETER)
  }

  test("GET: Missing name parameter") {

    request.setParameter(PASSWORD_PARAMETER, password)

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(NAME_PARAMETER)
  }

  test("GET: Missing pasword parameter") {

    request.setParameter(NAME_PARAMETER, userName)

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(PASSWORD_PARAMETER)
  }

  test("GET: Good paremeters, failed login") {

    request.setParameter(NAME_PARAMETER, userName)
    request.setParameter(PASSWORD_PARAMETER, password)

    Mockito.doThrow(new UnauthorizedException("intentional failure")).when(connection).getNewAuthToken(userName, password)

    service.doGet(request, response)

    response.getStatus should equal(401)
    response.getErrorMessage should include("intentional failure")
  }

  test("GET: Correct login") {

    request.setParameter(NAME_PARAMETER, userName)
    request.setParameter(PASSWORD_PARAMETER, password)

    val fakeAuthToken = "AUTH"
    Mockito.doReturn(fakeAuthToken).when(connection).getNewAuthToken(userName, password)

    service.doGet(request, response)

    response.getStatus should equal(200)
    response.getHeader(AUTH_HEADER) should equal(fakeAuthToken)
    response.getContentAsString should equal(fakeAuthToken)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(TEXT_FORMAT)
  }

  test("POST: No data") {

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("No JSON data")
  }

  test("POST: Malformed JSON data 1") {

    request.setContent("{".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("Error parsing json data")
  }

  test("POST: Malformed JSON data 2") {

    // add an extra close brace to request to make it badly formatted
    request.setContent((goodJSONRequest + "}").getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("Error parsing json data")
  }

  test("POST: Empty JSON data") {

    request.setContent("{}".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(NAME_PARAMETER)
    response.getErrorMessage should include(PASSWORD_PARAMETER)
  }

  test("POST: Missing name field") {

    request.setContent("{\"password\":\"asdasd\"}".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(NAME_PARAMETER)
    response.getErrorMessage should include(PASSWORD_PARAMETER)
  }

  test("POST: Missing password field") {

    request.setContent("{\"name\":\"asdasd\"}".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(NAME_PARAMETER)
    response.getErrorMessage should include(PASSWORD_PARAMETER)
  }

  test("POST: Well formatted request, reef failure") {

    request.setContent(goodJSONRequest.getBytes("UTF-8"))

    Mockito.doThrow(new UnauthorizedException("intentional failure")).when(connection).getNewAuthToken(userName, password)

    service.doPost(request, response)

    response.getStatus should equal(401)
    response.getErrorMessage should include("intentional failure")
  }

  test("POST: Correct Login") {

    request.setContent(goodJSONRequest.getBytes("UTF-8"))

    val fakeAuthToken = "AUTH"
    Mockito.doReturn(fakeAuthToken).when(connection).getNewAuthToken(userName, password)

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(AUTH_HEADER) should equal(fakeAuthToken)
    response.getContentAsString should equal("{\"token\":\"" + fakeAuthToken + "\"}")
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
  }
}