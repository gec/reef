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
import org.totalgrid.reef.httpbridge.JsonBridgeConstants._
import org.mockito.{ Matchers, Mockito }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.sapi.client.impl.FixedPromise
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.service.proto.Measurements
import org.totalgrid.reef.httpbridge.servlets.apiproviders.AllScadaServiceApiCallLibrary

@RunWith(classOf[JUnitRunner])
class ApiServletTest extends BaseServletTest {

  var service: ApiServlet = null
  var client: Client = null
  var services: AllScadaService = null

  val fakeAuthToken = "FAKE_AUTH"

  override def beforeEach() {
    super.beforeEach()
    service = new ApiServlet(connection, new AllScadaServiceApiCallLibrary)
    client = Mockito.mock(classOf[Client], new MockitoStubbedOnly())
    services = Mockito.mock(classOf[AllScadaService], new MockitoStubbedOnly())

    Mockito.doReturn(services).when(client).getRpcInterface(Matchers.eq(classOf[AllScadaService]))
    Mockito.doReturn(client).when(connection).getAuthenticatedClient(fakeAuthToken)
    Mockito.doNothing().when(client).setHeaders(Matchers.any())

    request.addHeader(AUTH_HEADER, fakeAuthToken)
    request.addHeader(ACCEPT_HEADER, JSON_FORMAT)
  }

  def makeMeas(name: String, value: Int, time: Long) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  val measName = "meas01"

  val meas = makeMeas(measName, 10, 1000)
  val measAsJson = "{\"name\":\"" + measName + "\",\"type\":\"INT\",\"int_val\":10,\"quality\":{},\"time\":1000}"
  val multiMeasAsJson = "{\"results\":[" + measAsJson + "," + measAsJson + "]}"

  test("Unknown function") {

    request.setPathInfo("unknownApiFunction")

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("unknownApiFunction")
  }

  test("getMeasurementByName: missing pointName parameter") {

    request.setPathInfo("getMeasurementByName")

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("pointName")
  }

  test("getMeasurementByName: reef failure message") {

    request.setPathInfo("getMeasurementByName")

    request.addParameter("pointName", measName)

    val promise = new FixedPromise[Measurement](Failure(new BadRequestException("no measurement")))
    Mockito.doReturn(promise).when(services).getMeasurementByName(Matchers.eq(measName))

    service.doGet(request, response)

    response.getStatus should equal(400)
    response.getErrorMessage should include("no measurement")
  }

  test("getMeasurementByName: Success") {

    request.setPathInfo("getMeasurementByName")

    request.addParameter("pointName", measName)

    val promise = new FixedPromise(Success(meas))
    Mockito.doReturn(promise).when(services).getMeasurementByName(Matchers.eq(measName))

    service.doGet(request, response)

    response.getStatus should equal(200)
    response.getContentAsString should equal(measAsJson)
  }

  test("getMeasurementsByNames: Success") {

    request.setPathInfo("getMeasurementsByNames")

    request.addParameter("pointNames", List(measName, measName).toArray)

    val promise = new FixedPromise(Success(List(meas, meas)))
    Mockito.doReturn(promise).when(services).getMeasurementsByNames(Matchers.eq(List(measName, measName)))

    service.doGet(request, response)

    response.getStatus should equal(200)
    response.getContentAsString should equal(multiMeasAsJson)
  }

}