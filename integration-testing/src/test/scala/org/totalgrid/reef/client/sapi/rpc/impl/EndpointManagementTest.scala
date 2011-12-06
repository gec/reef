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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.util.SyncVar
import org.totalgrid.reef.client.service.proto.Model.ReefUUID

import EndpointConnection.State._
import org.totalgrid.reef.client.SubscriptionResult

import org.totalgrid.reef.client.sapi.rpc.impl.util.{ SubscriptionEventAcceptorShim, ClientSessionSuite }

@RunWith(classOf[JUnitRunner])
class EndpointManagementTest
    extends ClientSessionSuite("EndpointManagement.xml", "EndpointManagement",
      <div>
        <p>
          The EndpointManagement service provides hooks to view and edit communication endpoints.
        </p>
      </div>)
    with ShouldMatchers {

  test("Endpoint operations") {

    recorder.addExplanation("Get all endpoints", "")
    val endpoints = client.getEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    recorder.addExplanation("Get all endpoint connections", "")
    val result = client.subscribeToEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    map.checkAllState(true, COMMS_UP)

    endpoints.foreach { e =>
      val endpointUuid = e.getUuid

      client.disableEndpointConnection(endpointUuid).await

      map.checkState(endpointUuid, false, COMMS_UP)
      map.checkState(endpointUuid, false, COMMS_DOWN)

      client.enableEndpointConnection(endpointUuid).await

      map.checkState(endpointUuid, true, COMMS_DOWN)
      map.checkState(endpointUuid, true, COMMS_UP)
    }

  }

  test("Disable All Endpoints") {

    val endpoints = client.getEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    endpoints.foreach { e => client.disableEndpointConnection(e.getUuid).await }

    map.checkAllState(false, COMMS_DOWN)

    endpoints.foreach { e => client.enableEndpointConnection(e.getUuid).await }

    map.checkAllState(true, COMMS_UP)

  }

  test("Enable and disable as fast as possible") {

    // this test is a stress test on the coordinator and fep and benchmark protocol and also
    // indirectly tests the exclusive update support in the endpoint_connection_service
    val endpoints = client.getEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    val result = client.subscribeToEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)

    (1 to 10).foreach { i =>
      // cycle the enabled field as fast as possible
      endpoints.foreach { e => client.disableEndpointConnection(e.getUuid).await }

      endpoints.foreach { e => client.enableEndpointConnection(e.getUuid).await }
    }

    // cant use original map because it will probably have sem multiple transitions to true, COMMS_UP
    // we need to verify that the endpoints are all going to end up good starting now.
    val postMap = new EndpointConnectionStateMap(client.subscribeToEndpointConnections().await)
    // eventually we should get back to enabled COMMS_UP
    postMap.checkAllState(true, COMMS_UP)
  }

  class EndpointConnectionStateMap(result: SubscriptionResult[List[EndpointConnection], EndpointConnection]) {

    private def makeEntry(e: EndpointConnection) = {
      //println(e.getEndpointByUuid.getName + " s: " + e.getState + " e: " + e.getEnabled + " a:" + e.getFrontEnd.getUuid.getUuid + " at: " + e.getLastUpdate)
      e.getEndpoint.getUuid -> e
    }

    val endpointStateMap = result.getResult.map { makeEntry(_) }.toMap
    val syncVar = new SyncVar(endpointStateMap)

    result.getSubscription.start(new SubscriptionEventAcceptorShim(ea => syncVar.atomic(m => m + makeEntry(ea.getValue))))

    def checkAllState(enabled: Boolean, state: EndpointConnection.State) {
      syncVar.waitFor(x => x.values.forall(e => e.getEnabled == enabled && e.getState == state), 20000)
    }
    def checkState(uuid: ReefUUID, enabled: Boolean, state: EndpointConnection.State) {
      syncVar.waitFor(x => x.get(uuid).map(e => e.getEnabled == enabled && e.getState == state).getOrElse(false), 20000)
    }
  }
}