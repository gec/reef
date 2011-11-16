/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.simulator.random

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.api.protocol.api.Publisher
import org.totalgrid.reef.proto.Measurements.{ Measurement, MeasurementBatch }
import org.totalgrid.reef.proto.SimMapping
import net.agileautomata.executor4s.testing.MockExecutor
import net.agileautomata.commons.testing._
import net.agileautomata.executor4s._
import collection.mutable.Queue

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class DefaultSimulatorTestSuite extends FunSuite with ShouldMatchers {

  class CachingPublisher extends Publisher[MeasurementBatch] {
    val queue = Queue.empty[MeasurementBatch]
    def publish(b: MeasurementBatch) = queue.enqueue(b)
  }

  def getConfig: SimMapping.SimulatorMapping = {
    def getIntMeasSim = SimMapping.MeasSim.newBuilder().
      setChangeChance(1.0).
      setInitial(0.0).
      setMax(10.0).
      setMin(-10.0).
      setMaxDelta(1.0).
      setName("integer").setType(Measurement.Type.INT).setUnit("unit").build()
    def getDoubleMeasSim = SimMapping.MeasSim.newBuilder().
      setChangeChance(1.0).
      setInitial(0.0).
      setMax(10.0).
      setMin(-10.0).
      setMaxDelta(1.0).
      setName("double").setType(Measurement.Type.DOUBLE).setUnit("unit").build()

    SimMapping.SimulatorMapping.newBuilder().addMeasurements(getIntMeasSim).addMeasurements(getDoubleMeasSim).setDelay(1).build()
  }

  def fixture(test: (DefaultSimulator, MockExecutor, CachingPublisher) => Unit) = {
    val exe = new MockExecutor
    val pub = new CachingPublisher
    def register(sim: DefaultSimulator) = new Cancelable { def cancel() {} }
    val factory = new DefaultSimulatorFactory(register)
    val sim = factory.create("test", exe, pub, getConfig)
    test(sim, exe, pub)
  }

  test("does publish on startup") {
    fixture { (sim, exe, pub) =>
      exe.numQueuedActions should equal(1)
      exe.runUntilIdle()
      pub.queue.headOption.map(_.getMeasCount) should equal(Some(2))
    }
  }

  test("initial publishes starts a timer") {
    fixture { (sim, exe, pub) =>
      exe.numQueuedActions should equal(1)
      exe.runUntilIdle()
      exe.numQueuedTimers should equal(1)
    }
  }

  test("publishes random values continuously") {
    fixture { (sim, exe, pub) =>
      exe.runUntilIdle()
      1000.times(exe.tick(1.milliseconds))
      pub.queue.size should be > 1

      pub.queue.foreach { mb =>

        mb.getMeasList.toList.foreach { m =>
          val output = m.getType match {
            case Measurement.Type.INT => m.getIntVal.toDouble
            case Measurement.Type.DOUBLE => m.getDoubleVal.toDouble
          }
          output should be >= -10.0
          output should be <= 10.0
        }
      }
    }
  }

}
