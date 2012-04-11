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
package org.totalgrid.reef.httpbridge.servlets.helpers.argumentsources

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.springframework.mock.web.MockHttpServletRequest
import org.scalatest.{ BeforeAndAfterEach, FunSuite }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }

@RunWith(classOf[JUnitRunner])
class ParameterArgumentSourceTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var request: MockHttpServletRequest = null
  var args: ParameterArgumentSource = null

  override def beforeEach() {
    request = new MockHttpServletRequest()
    args = new ParameterArgumentSource(request)
  }

  test("Basic Type parsing") {
    request.addParameter("string", "value")
    request.addParameter("int", "100")
    request.addParameter("long", "9999999999")
    request.addParameter("double1", "100.0")
    request.addParameter("double2", "-999.9")
    request.addParameter("double3", "66")

    request.addParameter("booleanT", "true")
    request.addParameter("booleanF", "false")

    args.getString("string") should equal("value")
    args.getInt("int") should equal(100)
    args.getLong("long") should equal(9999999999L)
    args.getDouble("double1") should equal(100.0)
    args.getDouble("double2") should equal(-999.9)
    args.getDouble("double3") should equal(66.0)

    args.getBoolean("booleanT") should equal(true)
    args.getBoolean("booleanF") should equal(false)
  }

  test("Type Error handling") {
    request.addParameter("string", "") // blank string is not allowed
    request.addParameter("int", "notANumber")
    request.addParameter("long", "notADouble")
    request.addParameter("double", "notADouble")
    request.addParameter("boolean", "notABoolean")

    intercept[BadRequestException] { args.getString("unknownArgument") }.getMessage should include("Missing required")

    intercept[BadRequestException] { args.getString("string") }.getMessage should include("blank")
    intercept[BadRequestException] { args.getInt("int") }.getMessage should include("Integer")
    intercept[BadRequestException] { args.getLong("long") }
    intercept[BadRequestException] { args.getDouble("double") }
    intercept[BadRequestException] { args.getBoolean("boolean") }
  }

  test("ReefUuid") {
    request.addParameter("blankUuid", "")
    request.addParameter("uuid", "78979879879")

    intercept[BadRequestException] { args.getUuid("blankUuid") }.getMessage should include("blank")

    args.getUuid("uuid") should equal(ReefUUID.newBuilder.setValue("78979879879").build)
  }

  test("ReefId") {
    request.addParameter("blankId", "")
    request.addParameter("id", "78979879879")

    intercept[BadRequestException] { args.getUuid("blankId") }.getMessage should include("blank")

    args.getId("id") should equal(ReefID.newBuilder.setValue("78979879879").build)
  }

}
