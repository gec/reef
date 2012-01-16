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

import builders.MeasurementRequestBuilders
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.client.service.proto.Measurements.{ DetailQual, Quality, Measurement }
import org.totalgrid.reef.client.service.proto.Processing.TriggerSet
import org.totalgrid.reef.client.service.proto.Model.Point
import util.{ EndpointConnectionStateMap, ServiceClientSuite }
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State

@RunWith(classOf[JUnitRunner])
class MeasurementFilterTest extends ServiceClientSuite {

  def makeNominalQuality() = {
    //Quality.newBuilder.setDetailQual(DetailQual.newBuilder).build
    Quality.newBuilder.build
  }

  def makeAnalog(name: String, value: Double, time: Long = System.currentTimeMillis): Measurement = {
    val m = Measurement.newBuilder
    m.setTime(time)
    m.setName(name)
    m.setType(Measurement.Type.DOUBLE)
    m.setDoubleVal(value)
    m.setQuality(makeNominalQuality)
    m.setUnit("A")
    m.build
  }

  def putMeas(m: Measurement) = client.publishMeasurements(m :: Nil).await

  test("Filter Deadband Test") {
    Thread.sleep(5000)

    val pointName = "StaticSubstation.Line02.Current"

    //println(session.get(TriggerSet.newBuilder.setPoint(Point.newBuilder.setName(pointName)).build).await.expectOne())

    putMeas(makeAnalog(pointName, 14))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(14)

    putMeas(makeAnalog(pointName, 14.2))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(14)

    putMeas(makeAnalog(pointName, 14.6))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(14.6)
  }

  test("Filter Endpoint Reset") {
    //Thread.sleep(5000)

    val pointName = "StaticSubstation.Line02.Current"

    putMeas(makeAnalog(pointName, 15.3))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(15.3)

    putMeas(makeAnalog(pointName, 15.2))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(15.3)
    putMeas(makeAnalog(pointName, 15.2))
    client.getMeasurementByName(pointName).await.getDoubleVal should equal(15.3)

    val endSub = client.subscribeToEndpointConnections().await
    val states = new EndpointConnectionStateMap(endSub)

    val uuid = client.getEndpointByName("StaticEndpoint").await.getUuid

    client.disableEndpointConnection(uuid).await
    states.checkState(uuid, false, State.COMMS_DOWN)

    client.enableEndpointConnection(uuid).await
    states.checkState(uuid, true, State.COMMS_UP)

    client.getMeasurementByName(pointName).await.getDoubleVal should equal(15.0)

  }

}
