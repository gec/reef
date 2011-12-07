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
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.measproc.ProtoHelper

@RunWith(classOf[JUnitRunner])
class ActionFrameworkTest extends Suite with ShouldMatchers {
  import Action._
  import ProtoHelper._

  class TestRig {
    val evalCalls = mutable.Queue[Measurement]()

    def eval(ret: Measurement): Action.Evaluation = (m: Measurement) => {
      evalCalls enqueue m
      ret
    }

    def action(act: ActivationType, disabled: Boolean, ret: Measurement) =
      new BasicAction("action01", disabled, act, eval(ret))
  }

  def testDisabled {
    val r = new TestRig
    val input = makeAnalog("meas01", 5.3)
    val output = makeAnalog("meas01", 5300.0)
    val result = r.action(High, true, output).process(input, true, true)
    result should equal(input)
    r.evalCalls.length should equal(0)
  }

  def scenario(state: Boolean, prev: Boolean, act: ActivationType, works: Boolean) = {
    val r = new TestRig
    val input = makeAnalog("meas01", 5.3)
    val output = makeAnalog("meas01", 5300.0)
    val result = r.action(act, false, output).process(input, state, prev)
    if (works) {
      result should equal(output)
      r.evalCalls.length should equal(1)
      r.evalCalls.dequeue should equal(input)
    } else {
      result should equal(input)
      r.evalCalls.length should equal(0)
    }
  }

  def matrix(act: ActivationType, col: Tuple4[Boolean, Boolean, Boolean, Boolean]) = {
    scenario(true, true, act, col._1)
    scenario(true, false, act, col._2)
    scenario(false, true, act, col._3)
    scenario(false, false, act, col._4)
  }

  def testHigh = {
    //    prev:   true  false true   false
    //    now:    true  true  false  false
    matrix(High, (true, true, false, false))
  }
  def testLow = {
    //    prev:   true  false true   false
    //    now:    true  true  false  false
    matrix(Low, (false, false, true, true))
  }
  def testRising = {
    //    prev:     true  false true   false
    //    now:      true  true  false  false
    matrix(Rising, (false, true, false, false))
  }
  def testFalling = {
    //    prev:      true  false true   false
    //    now:       true  true  false  false
    matrix(Falling, (false, false, true, false))
  }
  def testTransition = {
    //    prev:         true  false true   false
    //    now:          true  true  false  false
    matrix(Transition, (false, true, true, false))
  }
}