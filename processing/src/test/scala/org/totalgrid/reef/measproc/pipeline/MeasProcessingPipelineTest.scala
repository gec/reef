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
package org.totalgrid.reef.measproc.pipeline

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.mutable

import org.totalgrid.reef.api.proto.Events.Event
import org.totalgrid.reef.api.proto.Measurements.{ Quality, MeasurementBatch, Measurement }
import org.totalgrid.reef.measproc.{ MeasProcObjectCaches, MockObjectCache, ProtoHelper }

@RunWith(classOf[JUnitRunner])
class MeasProcessingPipelineTest extends Suite with ShouldMatchers {
  import ProtoHelper._

  class TestRig {
    val measQueue = mutable.Queue[Measurement]()
    val eventQueue = mutable.Queue[Event]()
    val measCache = new MockObjectCache[Measurement]
    val overCache = new MockObjectCache[Measurement]
    val stateCache = new MockObjectCache[Boolean]

    val proc = new MeasProcessingPipeline(
      MeasProcObjectCaches(measCache, overCache, stateCache),
      measQueue.enqueue(_),
      { b => eventQueue.enqueue(b.build) })

    def process(m: Measurement) {
      proc.process(MeasurementBatch.newBuilder.setWallTime(0).addMeas(m).build)
    }
  }

  def testTypical {
    val r = new TestRig
    r.proc.triggerProc.add(triggerSet)

    val m = makeAnalog("meas01", 5.3, 0, "raw")
    r.process(m)

    r.measQueue.length should equal(1)
    val meas = r.measQueue.dequeue
    checkGood(meas)

    r.measCache.putQueue.length should equal(1)
    r.measCache.putQueue.dequeue should equal(("meas01", meas))

    r.eventQueue.length should equal(0)
    r.stateCache.putQueue.length should equal(2)
    r.stateCache.putQueue.dequeue should equal(("meas01.rlclow", false))
    r.stateCache.putQueue.dequeue should equal(("meas01.trans", true))
  }

  def testFailure {
    val r = new TestRig
    r.proc.triggerProc.add(triggerSet)

    val m = makeAnalog("meas01", -5.3, 0, "raw")

    // First bad RLC check
    r.process(m)
    r.measQueue.length should equal(1)
    checkStripped(r.measQueue.dequeue)
    r.eventQueue.length should equal(1)
    r.eventQueue.dequeue.getEventType should equal("event01")

    // Check second bad value doesn't generate event
    r.process(m)
    r.measQueue.length should equal(1)
    checkStripped(r.measQueue.dequeue)
    r.eventQueue.length should equal(0)

    // Check deadband still bad
    val m2 = makeAnalog("meas01", 4.2, 0, "raw")
    r.process(m2)
    r.measQueue.length should equal(1)
    checkStripped(r.measQueue.dequeue)
    r.eventQueue.length should equal(0)

    // Check return to normal
    val m3 = makeAnalog("meas01", 5.3, 0, "raw")
    r.process(m3)
    r.measQueue.length should equal(1)
    checkGood(r.measQueue.dequeue)
    r.eventQueue.length should equal(1)
    r.eventQueue.dequeue.getEventType should equal("event02")
  }

  def testNIS {
    val r = new TestRig
    r.proc.triggerProc.add(triggerSet)

    val m = makeAnalog("meas01", 5.3, 0, "raw")
    r.process(m)

    r.measQueue.length should equal(1)
    val meas = r.measQueue.dequeue
    checkGood(meas)

    r.measCache.map("meas01") should equal(meas)

    r.proc.overProc.add(makeNIS("meas01"))
    r.overCache.putQueue.length should equal(1)
    r.overCache.putQueue.dequeue._2 should equal(meas)

    r.measQueue.length should equal(1)
    val nised = r.measQueue.dequeue
    nised.getQuality.getOperatorBlocked should equal(true)
    nised.getQuality.getDetailQual.getOldData should equal(true)

    val m2 = makeAnalog("meas01", 48, 0, "raw")
    r.process(m2)
    r.measQueue.length should equal(0)
    r.overCache.putQueue.length should equal(1)
    r.overCache.putQueue.dequeue._2 should equal(m2)

    r.proc.overProc.remove(makeNIS("meas01"))
    r.measQueue.length should equal(1)
    checkGood(r.measQueue.dequeue, 48)
    r.overCache.delQueue.length should equal(1)
    r.overCache.delQueue.dequeue should equal("meas01")
  }

  def checkGood(m: Measurement, value: Double = 5.3) {
    m.getName should equal("meas01")
    m.getUnit should equal("V")
    m.getType should equal(Measurement.Type.DOUBLE)
    m.getDoubleVal should equal(value * 10 + 50000)
  }
  def checkStripped(m: Measurement) {
    m.getName should equal("meas01")
    m.getUnit should equal("raw")
    m.getType should equal(Measurement.Type.NONE)
    m.hasDoubleVal should equal(false)
    m.getQuality.getValidity should equal(Quality.Validity.QUESTIONABLE)
  }
}
