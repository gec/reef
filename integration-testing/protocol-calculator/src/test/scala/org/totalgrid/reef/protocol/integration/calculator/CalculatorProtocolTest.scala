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
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor }
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.sapi.rpc.impl.util.{ ServiceClientSuite }
import net.agileautomata.commons.testing.SynchronizedVariable
import org.totalgrid.reef.client.service.proto.Measurements

@RunWith(classOf[JUnitRunner])
class CalculatorProtocolTest extends ServiceClientSuite {

  override val modelFile = "../../assemblies/assembly-common/filtered-resources/samples/calculations/config.xml"

  def makeMeas(name: String, time: Long, value: Int) = {
    val meas = Measurements.Measurement.newBuilder
    meas.setName(name).setType(Measurements.Measurement.Type.INT).setIntVal(value)
    meas.setQuality(Measurements.Quality.newBuilder.build)
    meas.setTime(time)
    meas.build
  }

  test("Test A * B") {

    val current = client.getPointByName("Microgrid1.Output.Current")
    val voltage = client.getPointByName("Microgrid1.Output.Voltage")

    client.setPointOutOfService(current)
    client.setPointOutOfService(voltage)

    val calc = client.getCalculationForPointByName("Microgrid1.Output.Power")
    calc.getFormula should equal("A * B")

    val meas = getCurrentValueWatcher(calc.getOutputPoint.getName)

    val outTime = System.currentTimeMillis()

    case class TestParams(current: Int, voltage: Int, output: Double, time: Long)
    val parameters = List(
      TestParams(10, 5, 5 * 10.0, outTime), TestParams(9, -6, -6 * 9.0, outTime + 5))

    parameters.foreach { p =>

      client.setPointOverride(current, makeMeas("Microgrid1.Output.Current", p.time - 10, p.current))
      client.setPointOverride(voltage, makeMeas("Microgrid1.Output.Voltage", p.time, p.voltage))

      meas.shouldHaveValue(p.output) within 500
    }
  }

  private def getCurrentValueWatcher(name: String) = {
    val subResult = client.subscribeToMeasurementsByNames(List(name))
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