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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.Model.Entity
import org.totalgrid.reef.httpbridge.JsonBridgeConstants._

@RunWith(classOf[JUnitRunner])
class ConverterServletTest extends BaseServletTest {

  var service: ConverterServlet = null

  override def beforeEach() {
    super.beforeEach()
    service = new ConverterServlet(builderLocator)

    request.setPathInfo("/entity")
  }

  val entityJson = "{\"types\":[\"Type1\"],\"name\":\"Name1\"}"
  val entityProto = Entity.newBuilder().setName("Name1").addTypes("Type1").build

  test("Missing CONTENT_TYPE_HEADER headers defaults to JSON") {
    request.setContent(entityJson.getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
  }

  test("Missing ACCEPT header defaults to JSON") {
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)

    request.setContent(entityJson.getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
  }

  test("Roundtrip JSON") {
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)

    request.setContent(entityJson.getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
    response.getContentAsString should equal(entityJson)
  }

  test("Convert Protobuf -> JSON") {
    request.addHeader(CONTENT_TYPE_HEADER, PROTOBUF_FORMAT)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)

    request.setContent(entityProto.toByteArray)

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(JSON_FORMAT)
    response.getContentAsString should equal(entityJson)
  }

  test("Convert JSON -> Protobuf") {
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)
    request.addHeader(ACCEPT_HEADER, PROTOBUF_FORMAT)

    request.setContent(entityJson.getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(200)
    response.getHeader(CONTENT_TYPE_HEADER) should equal(PROTOBUF_FORMAT)
    response.getContentAsByteArray should equal(entityProto.toByteArray)
  }

  ignore("JSON unknown fields") {
    request.addHeader(CONTENT_TYPE_HEADER, JSON_FORMAT)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)

    request.setContent("{\"types\":[\"Type1\"],\"name\":\"Name1\",\"dfasjkdajslkdajslkd\":\"asdasdasda\"}".getBytes("UTF-8"))

    service.doPost(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("Unknown fields")
  }

  test("GET proto descriptor /convert/entity") {
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)

    service.doGet(request, response)

    response.getStatus should equal(200)
    response.getContentAsString should include("LABEL_OPTIONAL")
    response.getContentAsString should include("Entity")
    response.getContentAsString should include("TYPE_STRING")
  }

  test("GET service types /convert") {
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)
    request.setPathInfo("")

    service.doGet(request, response)

    response.getStatus should equal(200)
    response.getContentAsString should include("types")
    response.getContentAsString should include("entity")
    response.getContentAsString should include("measurement_snapshot")
  }
}