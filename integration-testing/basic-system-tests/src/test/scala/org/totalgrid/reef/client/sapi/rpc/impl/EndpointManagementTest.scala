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
package org.totalgrid.reef.client.sapi.rpc.impl

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State._
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ EndpointConnectionStateMap, ServiceClientSuite }

@RunWith(classOf[JUnitRunner])
class EndpointManagementTest extends ServiceClientSuite {

  test("Endpoint operations") {

    val endpoints = client.getEndpoints().toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections()

    val map = new EndpointConnectionStateMap(result)

    map.checkAllState(true, COMMS_UP)

    endpoints.foreach { e =>
      val endpointUuid = e.getUuid

      client.disableEndpointConnection(endpointUuid)

      map.checkState(endpointUuid, false, COMMS_UP)
      map.checkState(endpointUuid, false, COMMS_DOWN)

      client.enableEndpointConnection(endpointUuid)

      map.checkState(endpointUuid, true, COMMS_DOWN)
      map.checkState(endpointUuid, true, COMMS_UP)
    }

  }

  test("Disable All Endpoints") {

    val endpoints = client.getEndpoints().toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections()

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    endpoints.foreach { e => client.disableEndpointConnection(e.getUuid) }

    map.checkAllState(false, COMMS_DOWN)

    endpoints.foreach { e => client.enableEndpointConnection(e.getUuid) }

    map.checkAllState(true, COMMS_UP)

  }

  /*
  Disabling this test because it causes failures in subsequent tests, the 10 restarts continue
  after the suite is over.

  test("Enable and disable as fast as possible") {

    // this test is a stress test on the coordinator and fep and benchmark protocol and also
    // indirectly tests the exclusive update support in the endpoint_connection_service
    val endpoints = client.getEndpoints().toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections()

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    (1 to 10).foreach { i =>
      // cycle the enabled field as fast as possible
      endpoints.foreach { e => client.disableEndpointConnection(e.getUuid) }

      endpoints.foreach { e => client.enableEndpointConnection(e.getUuid) }
    }

    // cant use original map because it will probably have sem multiple transitions to true, COMMS_UP
    // we need to verify that the endpoints are all going to end up good starting now.
    val postMap = new EndpointConnectionStateMap(client.subscribeToEndpointConnections())
    // eventually we should get back to enabled COMMS_UP
    postMap.checkAllState(true, COMMS_UP)
  }

  test("Eventually endpoints all up") {

    // since we were churning the endpoints its possible that we saw an intermediate state
    // we want to wait here to make sure the endpoints have settled down
    Thread.sleep(2000)

    val postMap = new EndpointConnectionStateMap(client.subscribeToEndpointConnections())
    postMap.checkAllState(true, COMMS_UP)
  }
*/
}