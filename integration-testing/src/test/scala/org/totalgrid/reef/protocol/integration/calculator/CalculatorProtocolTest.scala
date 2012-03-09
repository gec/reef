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
package org.totalgrid.reef.protocol.integration.calculator

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.loader.commons.{ LoaderServices, ModelDeleter }
import org.totalgrid.reef.loader.LoadManager
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State._
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ EndpointConnectionStateMap, ServiceClientSuite }
import net.agileautomata.commons.testing.SynchronizedVariable
import org.totalgrid.reef.client.service.proto.Measurements

@RunWith(classOf[JUnitRunner])
class CalculatorProtocolTest extends ServiceClientSuite {

  val calcModelFile = "../assemblies/assembly-common/filtered-resources/samples/calculations/config.xml"

  val stream = Some(Console.out)

  def loaderServices = session.getRpcInterface(classOf[LoaderServices])

  def services = session.getRpcInterface(classOf[AllScadaService])

  test("Clear system") {
    ModelDeleter.deleteEverything(loaderServices, false, stream)
  }

  test("Load calculator model") {
    LoadManager.loadFile(loaderServices, calcModelFile, false, false, false)
  }

  test("Wait for calculators to come online") {
    val endpoints = services.getEndpoints().await.toList

    endpoints.isEmpty should equal(false)

    val result = services.subscribeToEndpointConnections().await

    val map = new EndpointConnectionStateMap(result)

    // make sure everything starts comms_up and enabled
    map.checkAllState(true, COMMS_UP)
  }

  def makeMeas(name: String, time: Long, value: Int) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  test("Test A * B") {

    val current = services.getPointByName("Microgrid1.Output.Current").await
    val voltage = services.getPointByName("Microgrid1.Output.Voltage").await

    services.setPointOutOfService(current).await
    services.setPointOutOfService(voltage).await

    val calc = services.getCalculationForPointByName("Microgrid1.Output.Power").await
    calc.getFormula should equal("A * B")

    val meas = getCurrentValueWatcher(calc.getOutputPoint.getName)

    val outTime = System.currentTimeMillis()

    case class TestParams(current: Int, voltage: Int, output: Double, time: Long)
    val parameters = List(
      TestParams(10, 5, 5 * 10.0, outTime), TestParams(9, -6, -6 * 9.0, outTime + 5))

    parameters.foreach { p =>

      services.setPointOverride(current, makeMeas("Microgrid1.Output.Current", p.time - 10, p.current)).await
      services.setPointOverride(voltage, makeMeas("Microgrid1.Output.Voltage", p.time, p.voltage)).await

      meas.shouldHaveValue(p.output) within 500
    }
  }

  test("Reset system") {
    // test that we can remove the calc endpoint configuration
    ModelDeleter.deleteEverything(loaderServices, false, stream)

    // reload the standard model for other tests
    LoadManager.loadFile(loaderServices, modelFile, false, false, false)

    // wait for all endpoints to be up before continuing
    val result = client.subscribeToEndpointConnections().await
    val map = new EndpointConnectionStateMap(result)
    map.checkAllState(true, EndpointConnection.State.COMMS_UP)
  }

  private def getCurrentValueWatcher(name: String) = {
    val subResult = services.subscribeToMeasurementsByNames(List(name)).await
    val meas = new SynchronizedVariable[Measurement](subResult.getResult.head) {
      def shouldBecome(fun: Measurement => Boolean) = {
        def evaluate(success: Boolean, last: Measurement, timeout: Long) =
          if (!success) throw new Exception("Expected success within " + timeout + " ms but final value was " + last)
        new Become(fun(_))(evaluate)
      }

      def shouldHaveValueAndTime(value: Int, time: Long) = {
        def evaluate(success: Boolean, last: Measurement, timeout: Long) =
          if (!success) throw new Exception("Expected time: " + time + " value: " + value + " within " + timeout + " ms but final value was " + last)
        new Become(m => m.getIntVal == value && m.getTime == time)(evaluate)
      }
      def shouldHaveValue(value: Int) = {
        def evaluate(success: Boolean, last: Measurement, timeout: Long) =
          if (!success) throw new Exception("Expected value: " + value + " within " + timeout + " ms but final value was " + last)
        new Become(m => m.getIntVal == value)(evaluate)
      }
      def shouldHaveValue(value: Double) = {
        def evaluate(success: Boolean, last: Measurement, timeout: Long) =
          if (!success) throw new Exception("Expected value: " + value + " within " + timeout + " ms but final value was " + last)
        new Become(m => m.getDoubleVal == value)(evaluate)
      }
    }
    subResult.getSubscription.start(new SubscriptionEventAcceptor[Measurement] {
      def onEvent(event: SubscriptionEvent[Measurement]) {
        meas.set(event.getValue)
      }
    })
    meas
  }

}