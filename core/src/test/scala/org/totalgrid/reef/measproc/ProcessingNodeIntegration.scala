/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.measproc

import scala.collection.mutable
import org.totalgrid.reef.proto.Measurements._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Processing._
import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.FEP._
import org.totalgrid.reef.proto.Model._

import org.totalgrid.reef.app.SubscriptionProvider

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.totalgrid.reef.api.Envelope

@RunWith(classOf[JUnitRunner])
class ProcessingNodeIntegration extends Suite with ShouldMatchers {
  import ProtoHelper._

  class TestRig {
    val measQueue = mutable.Queue[Measurement]()
    val eventQueue = mutable.Queue[Event]()
    val measCache = new MockObjectCache[Measurement]
    val overCache = new MockObjectCache[Measurement]
    val stateCache = new MockObjectCache[Boolean]

    val endpointUid = "end01"
    val eventSubsystem = "sub01";

    var started = false

    var trigEventOpt: Option[(Envelope.Event, TriggerSet) => Unit] = None
    var trigRespOpt: Option[List[TriggerSet] => Unit] = None
    var overEventOpt: Option[(Envelope.Event, MeasOverride) => Unit] = None
    var overRespOpt: Option[List[MeasOverride] => Unit] = None

    val provider = new SubscriptionProvider {
      import org.totalgrid.reef.app.ServiceHandler._
      def subscribe[A <: AnyRef](parseFrom: Array[Byte] => A, searchKey: A, respHandler: ResponseHandler[A], eventHandler: EventHandler[A]) = {
        searchKey match {
          case key: TriggerSet => {
            trigEventOpt = Some(eventHandler.asInstanceOf[EventHandler[TriggerSet]])
            trigRespOpt = Some(respHandler.asInstanceOf[ResponseHandler[TriggerSet]])
          }
          case key: MeasOverride => {
            overEventOpt = Some(eventHandler.asInstanceOf[EventHandler[MeasOverride]])
            overRespOpt = Some(respHandler.asInstanceOf[ResponseHandler[MeasOverride]])
          }
        }
      }
    }

    val proc = ProcessingNode(
      measQueue.enqueue(_),
      Entity.newBuilder.setUuid(ReefUUID.newBuilder.setUuid(endpointUid)).build,
      provider,
      measCache,
      overCache,
      stateCache,
      eventSubsystem,
      eventQueue.enqueue(_), () => { started = true })

    assert(trigEventOpt != None)
    assert(trigRespOpt != None)
    assert(overEventOpt != None)
    assert(overRespOpt != None)

    started should equal(false)

    val trigEvent = trigEventOpt.get
    val overEvent = overEventOpt.get
    val trigResp = trigRespOpt.get
    val overResp = overRespOpt.get

    def process(m: Measurement) {
      proc.process(MeasurementBatch.newBuilder.setWallTime(0).addMeas(m).build)
    }
  }

  def set = {
    TriggerSet.newBuilder
      .setPoint(Point.newBuilder.setName("meas01"))
      .addTriggers(rlcLow("meas01"))
      .addTriggers(trans("meas01"))
      .build
  }
  def rlcLow(measName: String) = {
    Trigger.newBuilder
      .setTriggerName("rlclow")
      .setStopProcessingWhen(ActivationType.HIGH)
      .setUnit("raw")
      .setAnalogLimit(AnalogLimit.newBuilder.setLowerLimit(0).setDeadband(5))
      .addActions(
        Action.newBuilder
          .setActionName("strip")
          .setType(ActivationType.HIGH)
          .setStripValue(true))
        .addActions(
          Action.newBuilder
            .setActionName("qual")
            .setType(ActivationType.HIGH)
            .setQualityAnnotation(Quality.newBuilder.setValidity(Quality.Validity.QUESTIONABLE)))
          .addActions(
            Action.newBuilder
              .setActionName("eventrise")
              .setType(ActivationType.RISING)
              .setEvent(EventGeneration.newBuilder.setEventType("event01")))
            .addActions(
              Action.newBuilder
                .setActionName("eventfall")
                .setType(ActivationType.FALLING)
                .setEvent(EventGeneration.newBuilder.setEventType("event02")))
              .build
  }
  def trans(measName: String) = {
    Trigger.newBuilder
      .setTriggerName("trans")
      .setUnit("raw")
      .addActions(
        Action.newBuilder
          .setActionName("linear")
          .setType(ActivationType.HIGH)
          .setLinearTransform(LinearTransform.newBuilder.setScale(10).setOffset(50000)))
        .addActions(
          Action.newBuilder
            .setActionName("unit")
            .setType(ActivationType.HIGH)
            .setSetUnit("V"))
          .build
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

  def testStartup {
    val r = new TestRig
    r.trigResp(Nil)
    r.overResp(Nil)
    r.started should equal(true)
  }

  def testTypical {
    val r = new TestRig
    r.overResp(Nil)
    r.trigResp(List(set))
    r.started should equal(true)

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
    r.overResp(Nil)
    r.trigResp(List(set))
    r.started should equal(true)

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

  def makeNIS(name: String) = {
    MeasOverride.newBuilder.setPoint(Point.newBuilder.setName(name)).build
  }
  def makeOverride(name: String, value: Double, unit: String): MeasOverride = {
    MeasOverride.newBuilder
      .setPoint(Point.newBuilder.setName(name))
      .setMeas(Measurement.newBuilder
        .setTime(85)
        .setName(name)
        .setType(Measurement.Type.DOUBLE)
        .setDoubleVal(value)
        .setQuality(Quality.newBuilder.setDetailQual(DetailQual.newBuilder))
        .setUnit(unit))
      .build
  }

  def testNIS {
    val r = new TestRig
    r.overResp(Nil)
    r.trigResp(List(set))
    r.started should equal(true)

    val m = makeAnalog("meas01", 5.3, 0, "raw")
    r.process(m)

    r.measQueue.length should equal(1)
    val meas = r.measQueue.dequeue
    checkGood(meas)

    r.measCache.map("meas01") should equal(meas)

    r.overEvent(Envelope.Event.ADDED, makeNIS("meas01"))
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

    r.overEvent(Envelope.Event.REMOVED, makeNIS("meas01"))
    r.measQueue.length should equal(1)
    checkGood(r.measQueue.dequeue, 48)
    r.overCache.delQueue.length should equal(1)
    r.overCache.delQueue.dequeue should equal("meas01")
  }
}
