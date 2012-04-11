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
package org.totalgrid.reef.calc.lib

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.client.service.proto.Measurements.{ Quality, Measurement }

@RunWith(classOf[JUnitRunner])
class InputBucketTest extends FunSuite with ShouldMatchers {
  import InputBucket._

  import CalcLibTestHelpers._

  test("SinceLastBucket") {
    val buck = new SingleLatestBucket("test01")
    buck.getSnapshot should equal(None)

    val first = makeTraceMeas(1)
    buck.onReceived(first)
    buck.getSnapshot should equal(Some(List(first)))

    val second = makeTraceMeas(2)
    buck.onReceived(second)
    buck.getSnapshot should equal(Some(List(second)))
  }

  test("Limit Bucket for last two values") {
    val buck = new LimitRangeBucket("test01", 2, 2)
    buck.getSnapshot should equal(None)

    val first = makeTraceMeas(1)
    buck.onReceived(first)
    buck.getSnapshot should equal(None)

    val second = makeTraceMeas(2)
    buck.onReceived(second)
    buck.getSnapshot should equal(Some(List(first, second)))

    val third = makeTraceMeas(3)
    buck.onReceived(third)
    buck.getSnapshot should equal(Some(List(second, third)))
  }

  test("Limit Bucket with upto 100 values") {
    val buck = new LimitRangeBucket("test01", 100)
    buck.getSnapshot should equal(None)

    val values = (0 to 199).map { i => makeTraceMeas(i) }

    (0 to 99).foreach { i =>
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(0, i + 1)))
    }
    (100 to 199).foreach { i =>
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(i - 99, i + 1)))
    }
  }

  test("Time Bucket with upto 100 values") {
    val timeSource = new MockTimeSource(0)
    val buck = new FromRangeBucket(timeSource, "test01", -1000, 100)
    buck.getSnapshot should equal(None)

    val values = (0 to 199).map { i => makeTraceMeas(i, 0) }

    (0 to 99).foreach { i =>
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(0, i + 1)))
    }
    (100 to 199).foreach { i =>
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(i - 99, i + 1)))
    }
  }

  test("Time Bucket with expiring measurements") {
    val timeSource = new MockTimeSource(0)
    val buck = new FromRangeBucket(timeSource, "test01", -100, 10000)
    buck.getSnapshot should equal(None)

    val values = (0 to 199).map { i => makeTraceMeas(i, i) }

    (0 to 99).foreach { i =>
      timeSource.time = i
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(0, i + 1)))
    }
    (100 to 199).foreach { i =>
      timeSource.time = i
      buck.onReceived(values(i))
      buck.getSnapshot should equal(Some(values.slice(i - 99, i + 1)))
    }

    timeSource.time = 250
    buck.getSnapshot should equal(Some(values.slice(151, 200)))

    timeSource.time = 298
    buck.getSnapshot should equal(Some(values.slice(199, 200)))

    timeSource.time = 299
    buck.getSnapshot should equal(None)
  }

  test("No Storage Bucket") {
    val buck = new NoStorageBucket("test01", 10000)
    buck.getSnapshot should equal(None)

    val first = makeTraceMeas(1)
    buck.onReceived(first)
    buck.getSnapshot should equal(Some(List(first)))
    buck.getSnapshot should equal(None)
  }
}
