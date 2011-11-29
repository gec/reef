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

import org.totalgrid.reef.client.sapi.rpc.impl.util.ClientSessionSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions._

import org.totalgrid.reef.loader.commons.{ LoaderServices, LoaderClient }
import org.totalgrid.reef.proto.Model.{ PointType, Point }
import org.totalgrid.reef.util.Timing
import org.totalgrid.reef.proto.Measurements.{ Quality, Measurement }
import org.totalgrid.reef.proto.FEP.{ EndpointConnection, EndpointOwnership, Endpoint }

@RunWith(classOf[JUnitRunner])
class PointDeleteTest extends ClientSessionSuite("PointDelete.xml", "PointDelete", <div></div>) {

  val numberOfPoints = 10
  val multiplier = 1000

  test("Add " + numberOfPoints + " points") {
    LoaderClient.prepareClient(session)

    client.setHeaders(client.getHeaders.setTimeout(100000))

    val loaderServices = session.getRpcInterface(classOf[LoaderServices])

    val names = (0 to numberOfPoints).map { i => "TestPoint" + i }

    val points = names.map { n => loaderServices.addPoint(Point.newBuilder.setName(n).setType(PointType.ANALOG).setUnit("raw").build).await }

    val owner = EndpointOwnership.newBuilder.addAllPoints(names)

    val putEndpoint = Endpoint.newBuilder.setName("TestEndpoint").setProtocol("null").setOwnerships(owner).build
    val endpoint = loaderServices.addEndpoint(putEndpoint).await

    var connection = client.getEndpointConnectionByUuid(endpoint.getUuid).await

    var count = 0
    while (connection.getRouting.getServiceRoutingKey == "" && count < 5) {
      println("Waiting for meas processor to come online.")
      Thread.sleep(100)
      count += 1
      client.enableEndpointConnection(endpoint.getUuid).await
      connection = client.getEndpointConnectionByUuid(endpoint.getUuid).await
    }
    connection.getRouting.getServiceRoutingKey should not equal ("")

    (0 to numberOfPoints).map { i =>
      val measPublished = i * multiplier
      Timing.time("publish: " + measPublished) {
        publishNMeasurements(measPublished, names.get(i))
      }
    }

    client.disableEndpointConnection(endpoint.getUuid).await
    loaderServices.delete(endpoint).await

    (0 to numberOfPoints).foreach { i => Timing.time("delete: " + (i * multiplier)) { loaderServices.delete(points.get(i).toBuilder.clearEndpoint.build).await } }
  }

  private def publishNMeasurements(numMeas: Int, name: String) {
    if (numMeas > 0) {
      def makeMeas(value: Double) = Measurement.newBuilder.setName(name).setType(Measurement.Type.DOUBLE).setUnit("raw").setDoubleVal(value).setQuality(Quality.newBuilder).build
      val meases = (1 to numMeas).map { i => makeMeas(i.toDouble) }.toList
      client.publishMeasurements(meases).await
    }
  }
}