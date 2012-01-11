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

  test("Allow all first through") {
    val cache = new MockObjectCache[Measurement]()
    val band = new DeadbandTrigger.AllowAll
    val t = new DeadbandTrigger(cache, band)

    val m = makeAnalog("test01", 4.234)

    t.apply(m, false) should equal(true)
    cache.putQueue.size should equal(1)
    cache.putQueue.dequeue should equal("test01", m)

    t.apply(m, true) should equal(true)
    cache.putQueue.size should equal(1)
    cache.putQueue.dequeue should equal("test01", m)
  }

  test("No duplicates") {
    val cache = new MockObjectCache[Measurement]()
    val band = new DeadbandTrigger.NoDuplicates
    val t = new DeadbandTrigger(cache, band)

    val m = makeAnalog("test01", 4.234)

    cache.update("test01", m)
    t.apply(m, false) should equal(false)
    cache.putQueue.size should equal(0)

    val m2 = makeAnalog("test01", 3.3)
    t.apply(m2, false) should equal(true)
    cache.putQueue.size should equal(1)
    cache.putQueue.dequeue should equal("test01", m2)
  }

  test("Isolate units") {
    val cache = new MockObjectCache[Measurement]()
    val band = new DeadbandTrigger.NoDuplicates
    val t = new DeadbandTrigger(cache, band)

    val m = makeAnalog("test01", 4.234)
    cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setUnit("other").build
    t.apply(m2, false) should equal(true)
    cache.putQueue.size should equal(1)
    cache.putQueue.dequeue should equal("test01", m2)
  }

  test("Isolate quality") {
    val cache = new MockObjectCache[Measurement]()
    val band = new DeadbandTrigger.NoDuplicates
    val t = new DeadbandTrigger(cache, band)

    val m = makeAnalog("test01", 4.234)
    cache.update("test01", m)

    val m2 = Measurement.newBuilder(m).setQuality(Quality.newBuilder.setDetailQual(DetailQual.newBuilder).setValidity(Validity.INVALID)).build
    t.apply(m2, false) should equal(true)
    cache.putQueue.size should equal(1)
    cache.putQueue.dequeue should equal("test01", m2)
  }
}