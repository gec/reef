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
package org.totalgrid.reef.protocol.integration

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions._

import org.totalgrid.reef.client.sapi.rpc.impl.util.{ EndpointConnectionStateMap, ServiceClientSuite }
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.service.proto.FEP.{ FrontEndProcessor, EndpointConnection }
import org.totalgrid.reef.loader.commons.{ EndpointStopper, LoaderServices }

@RunWith(classOf[JUnitRunner])
class EndpointAssignmentTest extends ServiceClientSuite {

  test("Check Endpoints Start Online and Assigned") {

    val result = client.subscribeToEndpointConnections()
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.COMMS_UP)

    val benchmarkAdapter = findProtocolAdapter(_ == "benchmark")

    val connections = client.getEndpointConnections()
    endpointsAssignedTo(connections, benchmarkAdapter)
    endpointStateIs(connections, EndpointConnection.State.COMMS_UP)
  }

  test("Try claiming endpoint that is auto assigned") {

    val notBenchmarkAdapter = findProtocolAdapter(_ != "benchmark")

    val endpoints = client.getEndpoints()
    endpoints.foreach { endpoint =>
      intercept[BadRequestException] {
        client.setEndpointConnectionAssignedProtocolAdapter(endpoint.getUuid, notBenchmarkAdapter.getUuid)
      }
    }
  }

  test("Switch endpoints to manually assigned") {

    val endpoints = client.getEndpoints()

    endpoints.map { _.getAutoAssigned }.distinct should equal(List(true))

    endpoints.foreach { e => client.setEndpointAutoAssigned(e.getUuid, false) }

    val connections = client.getEndpointConnections()
    connections.map { _.hasFrontEnd } should equal(connections.map { c => false })
    endpointStateIs(connections, EndpointConnection.State.COMMS_DOWN)
  }

  test("Claim endpoints for the wrong protocol adapter and check for errors") {

    val notBenchmarkAdapter = findProtocolAdapter(_ == "calculator")

    val endpoints = client.getEndpoints()
    endpoints.foreach { endpoint =>
      client.setEndpointConnectionAssignedProtocolAdapter(endpoint.getUuid, notBenchmarkAdapter.getUuid)
    }

    val result = client.subscribeToEndpointConnections()
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.ERROR)
  }

  test("Switch endpoints back to auto assigned") {

    val endpoints = client.getEndpoints()

    endpoints.map { _.getAutoAssigned }.distinct should equal(List(false))

    endpoints.foreach { e => client.setEndpointAutoAssigned(e.getUuid, true) }

    val result = client.subscribeToEndpointConnections()
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.COMMS_UP)
  }

  test("Disable Endpoints and then set COMMS_UP and delete") {

    val endpoints = client.getEndpoints()
    endpoints.foreach { e => client.disableEndpointConnection(e.getUuid) }

    val result = client.subscribeToEndpointConnections()
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(false, EndpointConnection.State.COMMS_DOWN)

    endpoints.foreach { e => client.alterEndpointConnectionStateByEndpoint(e.getUuid, EndpointConnection.State.COMMS_UP) }

    val loaderServices = session.getService(classOf[LoaderServices])

    EndpointStopper.stopEndpoints(loaderServices, endpoints, Some(Console.out), true, 1000)
  }

  private def findProtocolAdapter(fun: String => Boolean): FrontEndProcessor = {
    val adapters = client.getProtocolAdapters()

    val adapter = adapters.find(_.getProtocolsList.toList.find(fun).isDefined)
    adapter should not equal (None)
    adapter.get
  }
  private def endpointsAssignedTo(connections: List[EndpointConnection], fep: FrontEndProcessor) {
    connections.map { _.getFrontEnd.getAppConfig.getInstanceName } should equal(connections.map { c => fep.getAppConfig.getInstanceName })
  }
  private def endpointStateIs(connections: List[EndpointConnection], state: EndpointConnection.State) {
    connections.map { _.getState } should equal(connections.map { c => state })
  }
}