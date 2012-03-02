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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.client.service.proto.Measurements.Measurement

@RunWith(classOf[JUnitRunner])
class MeasInputManagerTest extends FunSuite with ShouldMatchers {

  import CalcLibTestHelpers._

  class MockInputBucket(val variable: String, val getSnapshot: Option[List[Measurement]]) extends InputBucket {

    def onReceived(m: Measurement) = null
  }

  def meases(num: Int) = (0 to num).map { i => makeTraceMeas(i) }.toList

  def bucket(name: String, num: Int) = {
    val measList = if (num > 0) Some(meases(num)) else None
    new MockInputBucket(name, measList)
  }

  test("Aggregate getSnapshot with Nones") {
    MeasInputManager.getSnapshot(List(bucket("A", 0))) should equal(None)
    MeasInputManager.getSnapshot(List(bucket("A", 0), bucket("B", 1))) should equal(None)

    MeasInputManager.getSnapshot(List(bucket("A", 1), bucket("B", 0))) should equal(None)

  }

  test("Aggregate getSnapshot all valid") {
    MeasInputManager.getSnapshot(List(bucket("A", 1))) should equal(Some(Map("A" -> meases(1))))
    MeasInputManager.getSnapshot(List(bucket("A", 1), bucket("B", 2))) should equal(Some(Map("A" -> meases(1), "B" -> meases(2))))
  }
}
