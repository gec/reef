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
package org.totalgrid.reef.metrics

import org.totalgrid.reef.proto.{ Measurements }

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class NonOperationalDataPublisherTest extends FunSuite with ShouldMatchers {

  test("Non Operational String published") {
    var called = false;
    def tf(m: Measurements.MeasurementBatch) {
      m.getMeasCount should equal(1)
      m.getMeas(0).getStringVal should equal("TestString")
      m.getMeas(0).getName should equal("source.variable")
      called = true
    }
    val nop = new NonOperationalDataPublisher(tf)
    nop.nonOp("source", "variable", "TestString")
    assert(called)
  }

  test("Non Operational Int published") {
    var called = false;
    def tf(m: Measurements.MeasurementBatch) {
      m.getMeasCount should equal(1)
      m.getMeas(0).getIntVal should equal(999)
      m.getMeas(0).getName should equal("source.variable")
      called = true
    }
    val nop = new NonOperationalDataPublisher(tf)
    nop.nonOp("source", "variable", 999)
    assert(called)
  }

}
