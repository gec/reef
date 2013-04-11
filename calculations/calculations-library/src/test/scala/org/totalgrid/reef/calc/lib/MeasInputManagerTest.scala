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
import org.mockito.Mockito
import org.totalgrid.reef.client.sapi.rpc.MeasurementService
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.sapi.client.ServiceTestHelpers._
import org.totalgrid.reef.client.operations.scl.Event
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType
import org.totalgrid.reef.calc.lib.InputBucket.{ LimitRangeBucket, SingleLatestBucket }

@RunWith(classOf[JUnitRunner])
class MeasInputManagerTest extends FunSuite with ShouldMatchers {

  import CalcLibTestHelpers._

  class MockInputBucket(val variable: String, val getSnapshot: Option[List[Measurement]]) extends InputBucket {

    def onReceived(m: Measurement) {}

    def getMeasRequest = null
  }

  class MockEventedTrigger extends EventedTriggerStrategy {

    var lastMeas = Option.empty[Measurement]

    def handle(m: Measurement) = lastMeas = Some(m)
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

  test("MeasInputManger handles single value bucket") {

    val service = Mockito.mock(classOf[MeasurementService], new MockitoStubbedOnly)
    val trigger = new MockEventedTrigger

    val manager = new MeasInputManager(service, new MockTimeSource(0))

    val initalMeas = makeTraceMeas(0)
    val subResultA = subSuccess(makeTraceMeas(0))

    Mockito.doReturn(subResultA).when(service).subscribeToMeasurementsByNames(List("PointA"))

    manager.initialize(makeTraceMeas(0), List(InputConfig("PointA", new SingleLatestBucket("A"))), Some(trigger))

    trigger.lastMeas should equal(Some(initalMeas))

    manager.getSnapshot should equal(Some(Map("A" -> List(initalMeas))))

    val subAcceptorA = subResultA.await.mockSub.acceptor
    subAcceptorA should not equal (None)

    val secondMeas = makeTraceMeas(1)
    subAcceptorA.get.onEvent(Event(SubscriptionEventType.MODIFIED, secondMeas))

    trigger.lastMeas should equal(Some(secondMeas))
    manager.getSnapshot should equal(Some(Map("A" -> List(secondMeas))))
  }

  test("MeasInputManger handles multi value bucket") {

    val service = Mockito.mock(classOf[MeasurementService], new MockitoStubbedOnly)
    val trigger = new MockEventedTrigger

    val manager = new MeasInputManager(service, new MockTimeSource(0))

    val initialResults = List(makeTraceMeas(0), makeTraceMeas(1))
    val subResultA = subSuccess(initialResults)

    Mockito.doReturn(subResultA).when(service).subscribeToMeasurementHistoryByName("PointA", 10)

    manager.initialize(makeTraceMeas(0), List(InputConfig("PointA", new LimitRangeBucket("A", 10))), Some(trigger))

    trigger.lastMeas should equal(Some(initialResults.last))

    manager.getSnapshot should equal(Some(Map("A" -> initialResults)))

    val subAcceptorA = subResultA.await.mockSub.acceptor
    subAcceptorA should not equal (None)

    val lastMeas = makeTraceMeas(2)
    subAcceptorA.get.onEvent(Event(SubscriptionEventType.MODIFIED, lastMeas))

    trigger.lastMeas should equal(Some(lastMeas))
    manager.getSnapshot should equal(Some(Map("A" -> (initialResults ::: List(lastMeas)))))
  }
}
