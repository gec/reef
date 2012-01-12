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

@RunWith(classOf[JUnitRunner])
class DeadbandTriggerTest extends FunSuite with ShouldMatchers {

  import ProtoHelper._

  class Fixture(band: DeadbandTrigger.Deadband) {
    val cache = new MockObjectCache[Measurement]()
    val t = new DeadbandTrigger(cache, band)
  }

  test("Allow all first through") {
    val f = new Fixture(new DeadbandTrigger.AllowAll)

    val m = makeAnalog("test01", 4.234)

    f.t.apply(m, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m)

    f.t.apply(m, true) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m)
  }

  test("No duplicates") {
    val f = new Fixture(new DeadbandTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)

    f.cache.update("test01", m)
    f.t.apply(m, false) should equal(false)
    f.cache.putQueue.size should equal(0)

    val m2 = makeAnalog("test01", 3.3)
    f.t.apply(m2, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m2)
  }

  test("Duplicates") {
    duplicateTest(makeAnalog("test01", 4.234))
    duplicateTest(makeInt("test01", 10))
    duplicateTest(makeBool("test01", true))
    duplicateTest(makeString("test01", "string"))
  }

  def duplicateTest(m: Measurement) {
    val f = new Fixture(new DeadbandTrigger.NoDuplicates)
    f.cache.update("test01", m)
    f.t.apply(m, false) should equal(false)
    f.cache.putQueue.size should equal(0)
  }

  test("Isolate units") {
    val f = new Fixture(new DeadbandTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)
    f.cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setUnit("other").build
    f.t.apply(m2, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m2)
  }

  test("Isolate quality") {
    val f = new Fixture(new DeadbandTrigger.NoDuplicates)

    val m = makeAnalog("test01", 4.234)
    f.cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setQuality(Quality.newBuilder.setDetailQual(DetailQual.newBuilder).setValidity(Validity.INVALID)).build
    f.t.apply(m2, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m2)
  }

  test("Ints") {
    val f = new Fixture(new DeadbandTrigger.IntDeadband(2))

    val m = makeInt("test01", 10)

    f.t.apply(m, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m)

    f.t.apply(makeInt("test01", 11), false) should equal(false)
    f.cache.putQueue.size should equal(0)

    f.t.apply(makeInt("test01", 12), false) should equal(false)
    f.cache.putQueue.size should equal(0)

    val m2 = makeInt("test01", 13)
    f.t.apply(m2, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m2)

    val m3 = makeInt("test01", 10)
    f.t.apply(m3, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m3)
  }

  test("Double") {
    val f = new Fixture(new DeadbandTrigger.DoubleDeadband(1.5))

    val m = makeAnalog("test01", 10.01)

    f.t.apply(m, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m)

    f.t.apply(makeAnalog("test01", 11.01), false) should equal(false)
    f.cache.putQueue.size should equal(0)

    f.t.apply(makeAnalog("test01", 11.51), false) should equal(false)
    f.cache.putQueue.size should equal(0)

    val m2 = makeAnalog("test01", 11.52)
    f.t.apply(m2, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m2)

    val m3 = makeAnalog("test01", 9.99)
    f.t.apply(m3, false) should equal(true)
    f.cache.putQueue.size should equal(1)
    f.cache.putQueue.dequeue should equal("test01", m3)
  }
}