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
package org.totalgrid.reef.httpbridge.servlets.helpers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{ BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.client.proto.Envelope.RequestHeader
import org.totalgrid.reef.client.sapi.client.ServiceTestHelpers._
import org.springframework.mock.web.MockHttpServletRequest
import org.totalgrid.reef.httpbridge.servlets.helpers.argumentsources.ParameterArgumentSource
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.mockito.Mockito
import org.totalgrid.reef.client.service.proto.Utils.Attribute
import org.totalgrid.reef.client.sapi.rpc.impl.builders.EntityAttributesBuilders
import org.totalgrid.reef.client.exception.BadRequestException

class MockService {

  private def promise[A](a: A) = success(a)

  private def kv(key: String, value: String) = RequestHeader.newBuilder.setKey(key).setValue(value).build

  def makeHeader(key: String, value: String) =
    promise(kv(key, value))

  def maybeMakeHeader(keyValue: String, make: Boolean) =
    promise(if (make) Some(kv(keyValue, keyValue)) else None)

  def listHeaders(keyValue: String, duplications: Int) =
    promise((0 to duplications).map { i => kv(keyValue, keyValue) }.toList)

  def overloaded(key: String, value: String) =
    promise(kv(key, value))

  def overloaded(key: String, value: String, secondValue: String) =
    promise(kv(key, value + secondValue))

  def overloaded(key: String, value: String, duplications: Int) =
    promise(kv(key, value * duplications))

  def overloadTypes(name: String, value: String) =
    promise(EntityAttributesBuilders.stringAttribute(name, value))

  def overloadTypes(name: String, value: Long) =
    promise(EntityAttributesBuilders.longAttribute(name, value))

  def overloadTypes(name: String, value: Double) =
    promise(EntityAttributesBuilders.doubleAttribute(name, value))

  def overloadTypes(name: String, value: Boolean) =
    promise(EntityAttributesBuilders.boolAttribute(name, value))
}

class MockProvider extends ApiCallLibrary[MockService] {
  def serviceClass = classOf[MockService]

  single("makeHeader", classOf[RequestHeader], args => {
    val a1 = args.getString("key")
    val a2 = args.getString("value")
    (c) => c.makeHeader(a1, a2)
  })

  optional("maybeMakeHeader", classOf[RequestHeader], args => {
    val a1 = args.getString("keyValue")
    val a2 = args.getBoolean("make")
    (c) => c.maybeMakeHeader(a1, a2)
  })

  multi("listHeaders", classOf[RequestHeader], args => {
    val a1 = args.getString("keyValue")
    val a2 = args.getInt("duplications")
    (c) => c.listHeaders(a1, a2)
  })

  single("overloaded", classOf[RequestHeader], args => {
    val a1 = args.getString("key")
    val a2 = args.getString("value")
    (c) => c.overloaded(a1, a2)
  })

  single("overloaded", classOf[RequestHeader], args => {
    val a1 = args.getString("key")
    val a2 = args.getString("value")
    val a3 = args.getString("secondValue")
    (c) => c.overloaded(a1, a2, a3)
  })

  single("overloaded", classOf[RequestHeader], args => {
    val a1 = args.getString("key")
    val a2 = args.getString("value")
    val a3 = args.getInt("duplications")
    (c) => c.overloaded(a1, a2, a3)
  })

  single("overloadTypes", classOf[Attribute], args => {
    val a1 = args.getString("name")
    val a2 = args.getBoolean("value")
    (c) => c.overloadTypes(a1, a2)
  })
  single("overloadTypes", classOf[Attribute], args => {
    val a1 = args.getString("name")
    val a2 = args.getLong("value")
    (c) => c.overloadTypes(a1, a2)
  })
  single("overloadTypes", classOf[Attribute], args => {
    val a1 = args.getString("name")
    val a2 = args.getDouble("value")
    (c) => c.overloadTypes(a1, a2)
  })
  single("overloadTypes", classOf[Attribute], args => {
    val a1 = args.getString("name")
    val a2 = args.getString("value")
    (c) => c.overloadTypes(a1, a2)
  })
}

@RunWith(classOf[JUnitRunner])
class ApiProviderTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  val api = new MockProvider()
  val service = new MockService()
  val client = Mockito.mock(classOf[Client])
  Mockito.doReturn(service).when(client).getRpcInterface(classOf[MockService])

  var request: MockHttpServletRequest = null
  var args: ParameterArgumentSource = null

  override def beforeEach() {
    request = new MockHttpServletRequest()
    args = new ParameterArgumentSource(request)
  }

  def executeSingle(functionName: String): RequestHeader = {
    api.prepareApiCall(functionName, args).asInstanceOf[SingleResultApiCall[RequestHeader]].executeFunction(client).await
  }
  def executeOptional(functionName: String): Option[RequestHeader] = {
    api.prepareApiCall(functionName, args).asInstanceOf[OptionalResultApiCall[RequestHeader]].executeFunction(client).await
  }
  def executeMulti(functionName: String): List[RequestHeader] = {
    api.prepareApiCall(functionName, args).asInstanceOf[MultiResultApiCall[RequestHeader]].executeFunction(client).await
  }

  test("Simple single method") {
    request.setParameter("key", "KEY")
    request.setParameter("value", "VALUE")

    executeSingle("makeHeader") should equal(service.makeHeader("KEY", "VALUE").await)
  }

  test("Simple optional handling works") {
    request.setParameter("keyValue", "KEY")
    request.setParameter("make", "false")

    executeOptional("maybeMakeHeader") should equal(service.maybeMakeHeader("KEY", false).await)

    request.setParameter("make", "true")
    executeOptional("maybeMakeHeader") should equal(service.maybeMakeHeader("KEY", true).await)
  }

  test("Simple list handling works") {
    request.setParameter("keyValue", "KEY")
    request.setParameter("duplications", "3")

    executeMulti("listHeaders") should equal(service.listHeaders("KEY", 3).await)

    request.setParameter("duplications", "0")
    executeMulti("listHeaders") should equal(service.listHeaders("KEY", 0).await)
  }

  test("Calling function with overloaded names") {
    request.setParameter("key", "KEY")
    request.setParameter("value", "VALUE")

    executeSingle("overloaded") should equal(service.overloaded("KEY", "VALUE").await)

    request.setParameter("secondValue", "VALUE2")
    executeSingle("overloaded") should equal(service.overloaded("KEY", "VALUE", "VALUE2").await)

    request.setParameter("duplications", "3")
    executeSingle("overloaded") should equal(service.overloaded("KEY", "VALUE", 3).await)
  }

  def getAttr: Attribute = {
    api.prepareApiCall("overloadTypes", args).asInstanceOf[SingleResultApiCall[Attribute]].executeFunction(client).await
  }

  test("Overloaded arguments choose most specific type") {
    request.setParameter("name", "KEY")

    request.setParameter("value", "false")
    getAttr should equal(service.overloadTypes("KEY", false).await)

    request.setParameter("value", "100.0")
    getAttr should equal(service.overloadTypes("KEY", 100.0).await)

    request.setParameter("value", "100")
    getAttr should equal(service.overloadTypes("KEY", 100).await)

    request.setParameter("value", "stringy!")
    getAttr should equal(service.overloadTypes("KEY", "stringy!").await)
  }

  test("None overloaded method error includes valid arguments") {
    request.setParameter("key", "")

    val message = intercept[BadRequestException] { executeSingle("makeHeader") }.getMessage
    message should include("key")
    message should include("String")
    message should include("value")
    message should include("blank")
  }

  test("Overloaded method error includes valid arguments") {
    request.setParameter("name", "KEY")

    val message = intercept[BadRequestException] { getAttr }.getMessage
    message should include("Boolean")
    message should include("Double")
    message should include("Long")
    message should include("String")
  }
}
