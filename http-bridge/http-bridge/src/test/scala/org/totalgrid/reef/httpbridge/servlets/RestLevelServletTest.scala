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

import org.totalgrid.reef.httpbridge.JsonBridgeConstants._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.exception.{ UnauthorizedException, ServiceIOException }
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.service.proto.Model.Entity
import org.mockito.{ ArgumentCaptor, Matchers, Mockito }
import org.totalgrid.reef.client.proto.Envelope.{ Status, Verb }
import org.totalgrid.reef.client.operations.scl.ScalaRequestHeaders._
import org.totalgrid.reef.client.operations.scl.ScalaResponse
import org.totalgrid.reef.client.{ RequestHeaders, Client }
import org.totalgrid.reef.client.operations.impl.TestPromises

@RunWith(classOf[JUnitRunner])
class RestLevelServletTest extends BaseServletTest {

  var service: RestLevelServlet = null
  var client: Client = null
  var requestDelegate: ServiceRequestDelegate = null

  override def beforeEach() {
    super.beforeEach()
    requestDelegate = Mockito.mock(classOf[ServiceRequestDelegate], new MockitoStubbedOnly())
    service = new RestLevelServlet(connection, builderLocator, requestDelegate)
    client = Mockito.mock(classOf[Client], new MockitoStubbedOnly())
    Mockito.doReturn(client).when(connection).getAuthenticatedClient(fakeAuthToken)
    Mockito.doReturn(None).when(connection).getSharedBridgeAuthToken()
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)
  }

  val fakeAuthToken = "FAKE_AUTH"
  val entityRequestJson = "{\"name\": \"Name\"}"
  val entityRequestProto = Entity.newBuilder().setName("Name").build
  val entityResult1Proto = Entity.newBuilder().setName("Name1").addTypes("Type1").build
  val entityResult2Proto = Entity.newBuilder().setName("Name2").addTypes("Type2").build
  val twoEntityResultJson = "{\"results\":[{\"types\":[\"Type1\"],\"name\":\"Name1\"},{\"types\":[\"Type2\"],\"name\":\"Name2\"}]}"

  test("POST: No Auth Token") {

    service.doPost(request, response)

    response.getStatus should equal(401)
    response.getErrorMessage should include(AUTH_HEADER)
  }

  test("POST: No Verb") {

    request.addHeader(AUTH_HEADER, fakeAuthToken)

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(VERB_HEADER)
  }

  test("POST: No Verb (auth in url)") {

    request.addParameter(AUTH_HEADER, fakeAuthToken)

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(VERB_HEADER)
  }

  test("POST: No Verb (shared auth)") {

    Mockito.doReturn(Some(fakeAuthToken)).when(connection).getSharedBridgeAuthToken()

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include(VERB_HEADER)
  }

  test("POST: Illegal Verb") {

    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(VERB_HEADER, "HEAD")

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("HEAD")
    response.getErrorMessage should include("GET")
  }

  test("POST: No type after /rest") {

    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(VERB_HEADER, "GET")

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("Unknown service type")
  }

  test("POST: Unknown service type") {

    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(VERB_HEADER, "GET")

    request.setPathInfo("/magic_unknown_type")

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("magic_unknown_type")
  }

  test("POST: Bad JSON Data") {

    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(VERB_HEADER, "GET")

    request.setPathInfo("/entity")
    request.setContent("{".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("Error parsing json")
  }

  def goodRequest() {
    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(VERB_HEADER, "GET")

    request.setPathInfo("/entity")
    request.setContent(entityRequestJson.getBytes("UTF-8"))
  }

  test("POST: Good request, no connection to reef") {

    goodRequest()

    Mockito.doThrow(new ServiceIOException("intentional io failure")).when(connection).getAuthenticatedClient(fakeAuthToken)

    service.doPost(request, response)

    response.getStatus should equal(500)
    response.getErrorMessage should include("intentional io failure")
  }

  test("POST: Good request, expired auth token") {

    goodRequest()

    Mockito.doThrow(new UnauthorizedException("intentional auth failure")).when(connection).getAuthenticatedClient(fakeAuthToken)

    service.doPost(request, response)

    response.getStatus should equal(401)
    response.getErrorMessage should include("intentional auth failure")
  }

  test("POST: Failure message from Reef") {

    goodRequest()

    val promise = TestPromises.fixed(ScalaResponse.failure(Status.BAD_REQUEST, "entity not known"))
    Mockito.doReturn(promise).when(requestDelegate).makeRequest(Matchers.anyObject(), Matchers.eq(Verb.GET), Matchers.eq(entityRequestProto), Matchers.anyObject())

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("entity not known")
  }

  test("POST: Success message from Reef") {

    goodRequest()

    val promise = TestPromises.fixed(ScalaResponse.success(Status.OK, List(entityResult1Proto, entityResult2Proto)))
    Mockito.doReturn(promise).when(requestDelegate).makeRequest(Matchers.anyObject(), Matchers.eq(Verb.GET), Matchers.eq(entityRequestProto), Matchers.anyObject())

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getContentAsString should equal(twoEntityResultJson)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
  }

  def captureHeaders(): RequestHeaders = {
    val argument = ArgumentCaptor.forClass(classOf[RequestHeaders])

    Mockito.doThrow(new RuntimeException("intentional io failure")).when(requestDelegate).makeRequest(Matchers.anyObject(), Matchers.eq(Verb.GET), Matchers.eq(entityRequestProto), argument.capture())

    service.doPost(request, response)

    argument.getValue
  }

  test("POST: Default Headers") {

    goodRequest()

    val headers = captureHeaders()
    headers.resultLimit should equal(None)
    headers.timeout should equal(None)
  }

  test("POST: Editted Header Timeout") {

    goodRequest()

    request.addHeader(TIMEOUT_HEADER, "9999")

    val headers = captureHeaders()
    headers.resultLimit should equal(None)
    headers.timeout should equal(Some(9999))
  }

  test("POST: Editted Header ResultLimit") {

    goodRequest()

    request.addHeader(RESULT_LIMIT_HEADER, "8888")

    val headers = captureHeaders()
    headers.resultLimit should equal(Some(8888))
    headers.timeout should equal(None)
  }

  test("POST: Editted Header ResultLimit and Timeout") {

    goodRequest()

    request.addHeader(RESULT_LIMIT_HEADER, "8888")
    request.addHeader(TIMEOUT_HEADER, "9999")

    val headers = captureHeaders()
    headers.resultLimit should equal(Some(8888))
    headers.timeout should equal(Some(9999))
  }

  test("POST: Badly formatted ResultLimit") {

    goodRequest()

    request.addHeader(RESULT_LIMIT_HEADER, "asdasd")

    service.doPost(request, response)

    response.getErrorMessage should include("number")
    response.getErrorMessage should include("Header")
    response.getErrorMessage should include(RESULT_LIMIT_HEADER)
    response.getErrorMessage should include("asdasd")
    response.getStatus should equal(400)
  }
}