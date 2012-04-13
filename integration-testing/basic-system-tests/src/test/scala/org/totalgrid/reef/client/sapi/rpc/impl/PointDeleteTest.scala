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

import org.totalgrid.reef.client.sapi.rpc.impl.util.ServiceClientSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions._

import org.totalgrid.reef.loader.commons.LoaderServices
import org.totalgrid.reef.client.service.proto.Model.{ PointType, Point }
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointOwnership, Endpoint }

@RunWith(classOf[JUnitRunner])
class PointDeleteTest extends ServiceClientSuite {

  val numberOfPoints = 10

  test("Add " + numberOfPoints + " points on test endpoint") {

    client.setHeaders(client.getHeaders.setTimeout(100000))

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])

    val names = (0 to numberOfPoints).map { i => "TestPoint" + i }

    val points = names.map { n => loaderServices.addPoint(Point.newBuilder.setName(n).setType(PointType.ANALOG).setUnit("raw").build).await }

    val owner = EndpointOwnership.newBuilder.addAllPoints(names)

    val putEndpoint = Endpoint.newBuilder.setName("TestEndpoint").setProtocol("null").setOwnerships(owner).build
    val endpoint = loaderServices.addEndpoint(putEndpoint).await

    var connection = client.getEndpointConnectionByUuid(endpoint.getUuid)

    var count = 0
    while (connection.getRouting.getServiceRoutingKey == "" && count < 5) {
      println("Waiting for meas processor to come online.")
      Thread.sleep(100)
      count += 1
      client.enableEndpointConnection(endpoint.getUuid)
      connection = client.getEndpointConnectionByUuid(endpoint.getUuid)
    }
    connection.getRouting.getServiceRoutingKey should not equal ("")

    client.disableEndpointConnection(endpoint.getUuid)
    loaderServices.delete(endpoint).await

    (0 to numberOfPoints).foreach { i => loaderServices.delete(points.get(i)).await }
  }

}