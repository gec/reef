/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import builders.PointRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.BlockingQueue

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

    val queue = new BlockingQueue[CommEndpointConnection]
    val sub = client.creatEndpointConnectionSubscription(new IEventAcceptorShim(ea => queue.push(ea.result)))

    client.addExplanation("Get all endpoint connections", "")
    val connections = client.getAllEndpointConnections(sub)

    val connectionEndpointUuids = connections.map { _.getEndpoint.getUuid.getUuid }.sorted
    val endpointUuids = endpoints.map { _.getUuid.getUuid }.sorted

    connectionEndpointUuids should equal(endpointUuids)

    // make sure everything starts comms_up and enabled
    connections.map { _.getState } should equal(connections.map { x => CommEndpointConnection.State.COMMS_UP })
    connections.map { _.getEnabled } should equal(connections.map { x => true })

    val endpointUuid = endpoints.head.getUuid

    client.disableEndpointConnection(endpointUuid)

    val update1 = queue.pop(5000)
    update1.getEndpoint.getUuid should equal(endpointUuid)
    update1.getEnabled should equal(false)
    update1.getState should equal(CommEndpointConnection.State.COMMS_UP)

    val update2 = queue.pop(5000)
    update2.getEndpoint.getUuid should equal(endpointUuid)
    update2.getEnabled should equal(false)
    update2.getState should equal(CommEndpointConnection.State.COMMS_DOWN)

    client.enableEndpointConnection(endpoints.head.getUuid)

    val update3 = queue.pop(5000)
    update3.getEndpoint.getUuid should equal(endpointUuid)
    update3.getEnabled should equal(true)
    update3.getState should equal(CommEndpointConnection.State.COMMS_DOWN)

    val update4 = queue.pop(5000)
    update4.getEndpoint.getUuid should equal(endpointUuid)
    update4.getEnabled should equal(true)
    update4.getState should equal(CommEndpointConnection.State.COMMS_UP)

  }

}