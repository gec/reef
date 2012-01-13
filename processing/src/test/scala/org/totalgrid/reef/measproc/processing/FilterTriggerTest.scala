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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.totalgrid.reef.measproc.{ ProtoHelper, MockObjectCache }
import org.totalgrid.reef.client.service.proto.Measurements.{ DetailQual, Quality, Measurement }
import org.totalgrid.reef.client.service.proto.Measurements.Quality.Validity
import org.totalgrid.reef.client.service.proto.Events.Event
import org.totalgrid.reef.client.service.proto.Processing.{ Trigger => TriggerProto, Filter => FilterProto, Action => ActionProto, ActivationType }

@RunWith(classOf[JUnitRunner])
class FilterTriggerTest extends FunSuite with ShouldMatchers {

  import ProtoHelper._

  test("Factory") {
    val cache = new MockObjectCache[Measurement]()
    def publish(ev: Event.Builder): Unit = {}

    val fac = new TriggerProcessingFactory(publish, cache)

    val stateCache = new MockObjectCache[Boolean]

    val proto = TriggerProto.newBuilder
      .setTriggerName("testTrigger")
      .setPriority(100)
      .setFilter(
        FilterProto.newBuilder
          .setType(FilterProto.FilterType.DUPLICATES_ONLY))
        .addActions(
          ActionProto.newBuilder
            .setSuppress(true)
            .setActionName("action01")
            .setType(ActivationType.LOW))
          .build

    val trig = fac.buildTrigger(proto, "point01")

    val m = makeInt("test01", 10)
    trig.process(m, stateCache) should equal((m, false))

    trig.process(m, stateCache) should equal((m, true))

  }

  class Fixture(band: FilterTrigger.Filter) {
    val cache = new MockObjectCache[Measurement]()
    val t = new FilterTrigger(cache, band)

    def sendAndBlock(m: Measurement) {
      t.apply(m, false) should equal(false)
      cache.putQueue.size should equal(0)
    }
    def sendAndReceive(m: Measurement) {
      t.apply(m, false) should equal(true)
      cache.putQueue.size should equal(1)
      cache.putQueue.dequeue should equal(m.getName, m)
    }
  }

  test("Duplicates first through") {
    val f = new Fixture(new FilterTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)

    f.sendAndReceive(m)

    f.sendAndBlock(m)
  }

  test("No duplicates") {
    val f = new Fixture(new FilterTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)

    f.cache.update("test01", m)

    f.sendAndBlock(m)

    f.sendAndReceive(makeAnalog("test01", 3.3))
  }

  test("Duplicates") {
    duplicateTest(makeAnalog("test01", 4.234))
    duplicateTest(makeInt("test01", 10))
    duplicateTest(makeBool("test01", true))
    duplicateTest(makeString("test01", "string"))
  }

  def duplicateTest(m: Measurement) {
    val f = new Fixture(new FilterTrigger.NoDuplicates)
    f.cache.update("test01", m)
    f.sendAndBlock(m)
  }

  test("Isolate units") {
    val f = new Fixture(new FilterTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)
    f.cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setUnit("other").build
    f.sendAndReceive(m2)
  }

  test("Isolate quality") {
    val f = new Fixture(new FilterTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)
    f.cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setQuality(Quality.newBuilder.setDetailQual(DetailQual.newBuilder).setValidity(Validity.INVALID)).build
    f.sendAndReceive(m2)
  }

  test("Ints") {
    val f = new Fixture(new FilterTrigger.Deadband(2))

    val m = makeInt("test01", 10)

    f.sendAndReceive(m)

    f.sendAndBlock(makeInt("test01", 11))

    f.sendAndBlock(makeInt("test01", 12))

    f.sendAndReceive(makeInt("test01", 13))

    f.sendAndReceive(makeInt("test01", 10))
  }

  test("Double") {
    val f = new Fixture(new FilterTrigger.Deadband(1.5))

    f.sendAndReceive(makeAnalog("test01", 10.01))

    f.sendAndBlock(makeAnalog("test01", 11.01))

    f.sendAndBlock(makeAnalog("test01", 11.51))

    f.sendAndReceive(makeAnalog("test01", 11.52))

    f.sendAndReceive(makeAnalog("test01", 9.99))
  }
}