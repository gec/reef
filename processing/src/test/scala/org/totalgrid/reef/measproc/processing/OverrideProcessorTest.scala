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
package org.totalgrid.reef.measproc.processing

import org.scalatest.Suite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.mutable
import org.totalgrid.reef.api.proto.Measurements.Measurement
import org.totalgrid.reef.measproc.{ ProtoHelper, MockObjectCache }
import org.totalgrid.reef.api.proto.Processing.MeasOverride
import org.totalgrid.reef.api.proto.Model.Point
import org.totalgrid.reef.api.proto.Measurements
import org.totalgrid.reef.japi.Envelope

@RunWith(classOf[JUnitRunner])
class OverrideProcessorTest extends Suite with ShouldMatchers {
  import ProtoHelper._

  class TestRig {

    val measQueue = new mutable.Queue[Measurement]()

    val overCache = new MockObjectCache[Measurement]
    val measCache = new MockObjectCache[Measurement]
    val proc = new OverrideProcessor((m, b) => measQueue.enqueue(m), overCache, measCache.get(_))

    def configure(config: List[MeasOverride]) = proc.subscribed(config)
    def event(ev: Envelope.Event, proto: MeasOverride) = proc.handleEvent(ev, proto)

    def sendAndCheckMeas(m: Measurement) {
      proc.process(m)
      checkSame(m, measQueue.dequeue)
      overCache.putQueue.length should equal(0)
    }

    def sendAndCheckOver(m: Measurement) = {
      proc.process(m)
      checkSame(m, overCache.putQueue.dequeue._2)
      measQueue.length should equal(0)
    }
    def receiveAndCheckMeas(orig: Measurement) = {
      checkSameExceptTimeIsGreater(orig, measQueue.dequeue)
    }
    def receiveAndCheckOver(m: Measurement) = {
      checkSame(m, overCache.putQueue.dequeue._2)
    }
    def checkNISPublished(orig: Measurement) = {
      checkNIS(orig, measQueue.dequeue)
    }
    def checkReplacePublished(repl: Measurement) = {
      checkReplaced(repl, measQueue.dequeue)
    }
  }

  def makeOverride(name: String): MeasOverride = makeOverride(name, 0, "")

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
        .setQuality(Measurements.Quality.newBuilder.setDetailQual(Measurements.DetailQual.newBuilder))
        .setUnit(unit))
      .build
  }

  def checkSame(m: Measurement, meas: Measurement): Unit = {
    m.getName should equal(meas.getName)
    m.getType should equal(meas.getType)
    m.getDoubleVal should equal(meas.getDoubleVal)
    m.getUnit should equal(meas.getUnit)
    m.getTime should equal(meas.getTime)
    m.getQuality should equal(meas.getQuality)
  }

  def checkSameExceptTimeIsGreater(orig: Measurement, meas: Measurement): Unit = {
    meas.getName should equal(orig.getName)
    meas.getType should equal(orig.getType)
    meas.getDoubleVal should equal(orig.getDoubleVal)
    meas.getUnit should equal(orig.getUnit)
    meas.getQuality should equal(orig.getQuality)
    meas.getTime should be >= (orig.getTime)
  }

  def sameExceptQualityAndTimeIsGreater(orig: Measurement, pub: Measurement): Unit = {
    pub.getName should equal(orig.getName)
    pub.getType should equal(orig.getType)
    pub.getDoubleVal should equal(orig.getDoubleVal)
    pub.getUnit should equal(orig.getUnit)
    pub.getTime should be >= (orig.getTime)
  }
  def checkNIS(orig: Measurement, pub: Measurement): Unit = {
    sameExceptQualityAndTimeIsGreater(orig, pub)
    pub.getQuality.getDetailQual.getOldData should equal(true)
    pub.getQuality.getOperatorBlocked should equal(true)
  }

  def checkReplaced(repl: Measurement, pub: Measurement): Unit = {
    sameExceptQualityAndTimeIsGreater(repl, pub)
    pub.getQuality.getDetailQual.getOldData should equal(false)
    pub.getQuality.getSource should equal(Measurements.Quality.Source.SUBSTITUTED)
  }

  def testNullOverride {
    val r = new TestRig
    r.configure(Nil)
    r.sendAndCheckMeas(makeAnalog("meas01", 5.3))
  }

  def testPreconfig {
    val r = new TestRig
    val config = List(makeOverride("meas01", 89, "V"))
    r.configure(config)
    r.checkReplacePublished(config(0).getMeas)
    r.sendAndCheckOver(makeAnalog("meas01", 5.3))
  }

  def testMultiple {
    val config = List(makeOverride("meas01", 89, "V"), makeOverride("meas02", 44, "A"))
    val r = new TestRig
    r.configure(config)

    r.checkReplacePublished(config(0).getMeas)
    r.checkReplacePublished(config(1).getMeas)
    r.sendAndCheckOver(makeAnalog("meas01", 5.3))
    r.sendAndCheckOver(makeAnalog("meas02", 5.3))
    r.sendAndCheckMeas(makeAnalog("meas03", 5.3))
  }

  def testNISWithReplace {
    val r = new TestRig
    r.configure(Nil)
    val orig = makeAnalog("meas01", 5.3)
    r.measCache.update(orig.getName, orig)
    val over = makeOverride("meas01", 89, "V")
    r.event(Envelope.Event.ADDED, over)
    r.checkReplacePublished(over.getMeas)
    r.receiveAndCheckOver(orig)
    r.sendAndCheckOver(makeAnalog("meas01", 44))
  }

  def testNISThenReplace {
    val r = new TestRig
    r.configure(Nil)
    val orig = makeAnalog("meas01", 5.3)
    r.measCache.update(orig.getName, orig)
    val nis = makeNIS("meas01")
    r.event(Envelope.Event.ADDED, nis)
    r.checkNISPublished(orig)
    r.receiveAndCheckOver(orig)

    val replace = makeOverride("meas01", 89, "V")
    r.event(Envelope.Event.ADDED, replace)
    r.checkReplacePublished(replace.getMeas)

    r.sendAndCheckOver(makeAnalog("meas01", 44))
  }

  def doublePush(event: Envelope.Event) {
    val r = new TestRig
    r.configure(Nil)

    val orig = makeAnalog("meas01", 5.3)
    r.measCache.update(orig.getName, orig)
    val over = makeOverride("meas01", 89, "V")
    r.event(event, over)
    r.checkReplacePublished(over.getMeas)
    r.receiveAndCheckOver(orig)
    r.sendAndCheckOver(makeAnalog("meas01", 44))

    // On the second add, we shouldn't override the cached "real" value
    val over2 = makeOverride("meas01", 55, "V")
    r.event(event, over2)
    r.checkReplacePublished(over2.getMeas)
    r.overCache.putQueue.length should equal(0)
    r.sendAndCheckOver(makeAnalog("meas01", 23))
  }

  def testDoubleAdd {
    doublePush(Envelope.Event.ADDED)
  }
  def testDoubleModify {
    doublePush(Envelope.Event.MODIFIED)
  }

  def testRemoved {
    val config = List(makeOverride("meas01", 89, "V"))
    val r = new TestRig
    r.configure(config)

    r.checkReplacePublished(config(0).getMeas)
    val orig = makeAnalog("meas01", 5.3)
    r.overCache.update(orig.getName, orig)
    val nisRemove = makeNIS("meas01")
    r.event(Envelope.Event.REMOVED, nisRemove)
    r.receiveAndCheckMeas(orig)
    r.overCache.delQueue.length should equal(1)
    r.overCache.delQueue.dequeue should equal("meas01")
    r.sendAndCheckMeas(makeAnalog("meas01", 33))
  }
}

