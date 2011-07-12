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
package org.totalgrid.reef.japi.request

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.EmptySyncVar

import org.totalgrid.reef.proto.FEP.CommEndpointConnection

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

    client.addExplanation("Get all endpoints", "")
    val endpoints = client.getAllEndpoints()

    endpoints.isEmpty should equal(false)

    val syncVar = new EmptySyncVar[CommEndpointConnection]

    client.addExplanation("Get all endpoint connections", "")
    val result = client.subscribeToAllEndpointConnections()
    val connections = result.getResult
    val sub = result.getSubscription

    val connectionEndpointUuids = connections.map { _.getEndpoint.getUuid.getUuid }.sorted
    val endpointUuids = endpoints.map { _.getUuid.getUuid }.sorted

    connectionEndpointUuids should equal(endpointUuids)

    // make sure everything starts comms_up and enabled
    connections.map { _.getState } should equal(connections.map { x => CommEndpointConnection.State.COMMS_UP })
    connections.map { _.getEnabled } should equal(connections.map { x => true })

    // pick one endpoint to test enabling/disabling
    val endpointUuid = endpoints.head.getUuid

    sub.start(new SubscriptionEventAcceptorShim(ea => syncVar.update(ea.getValue())))

    def checkState(enabled: Boolean, state: CommEndpointConnection.State) {
      syncVar.waitFor(x => x.getEnabled == enabled &&
        x.getState == state &&
        x.getEndpoint.getUuid.getUuid == endpointUuid.getUuid)
    }

    client.disableEndpointConnection(endpointUuid)

    checkState(false, CommEndpointConnection.State.COMMS_UP)
    checkState(false, CommEndpointConnection.State.COMMS_DOWN)

    client.enableEndpointConnection(endpoints.head.getUuid)

    checkState(true, CommEndpointConnection.State.COMMS_DOWN)
    checkState(true, CommEndpointConnection.State.COMMS_UP)

  }

}