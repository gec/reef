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
package org.totalgrid.reef.frontend

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.totalgrid.reef.app.ClearableMap
import org.mockito.Mockito
import org.totalgrid.reef.proto.FEP.{ CommChannel, CommEndpointRouting, CommEndpointConnection, CommEndpointConfig }
import org.totalgrid.reef.proto.Model.ReefUUID

@RunWith(classOf[JUnitRunner])
class EndpointConnectionSubscriptionFilterTest extends FunSuite with ShouldMatchers {
  test("Adds and modifies are populated") {
    val map = Mockito.mock(classOf[ClearableMap[CommEndpointConnection]])
    val populator = Mockito.mock(classOf[EndpointConnectionPopulatorAction])
    val filter = new EndpointConnectionSubscriptionFilter(map, populator)

    val populated = getConfig(true, Some("routing2"))
    val config = getConfig(true, Some("routing"))
    Mockito.doReturn(populated).when(populator).populate(config)

    filter.add(config)
    Mockito.verify(map).add(populated)

    filter.modify(config)
    Mockito.verify(map).modify(populated)
  }

  test("Ignores Disabled or unrouted endpoints") {
    val map = Mockito.mock(classOf[ClearableMap[CommEndpointConnection]])
    val populator = Mockito.mock(classOf[EndpointConnectionPopulatorAction])
    val filter = new EndpointConnectionSubscriptionFilter(map, populator)

    def testAddOrModifyBecomesRemove(obj: CommEndpointConnection) {
      filter.add(obj)
      filter.modify(obj)
      Mockito.verify(map, Mockito.times(2)).remove(obj)
    }

    testAddOrModifyBecomesRemove(getConfig(false, None))
    testAddOrModifyBecomesRemove(getConfig(false, Some("routing")))
    testAddOrModifyBecomesRemove(getConfig(true, None))

    val remove = getConfig(true, Some("routing"))
    filter.remove(remove)
    Mockito.verify(map).remove(remove)

    Mockito.verifyZeroInteractions(populator)
    Mockito.verifyNoMoreInteractions(map)
  }

  test("Handles subscription and canceling") {
    val map = Mockito.mock(classOf[ClearableMap[CommEndpointConnection]])
    val populator = Mockito.mock(classOf[EndpointConnectionPopulatorAction])
    val filter = new EndpointConnectionSubscriptionFilter(map, populator)

    val populated = getConfig(true, Some("routing2"))
    val config = getConfig(true, Some("routing"))
    Mockito.doReturn(populated).when(populator).populate(config)

    val result = new MockSubscriptionResult(config)

    filter.setSubscription(result)
    Mockito.verify(map).add(populated)

    intercept[IllegalArgumentException] {
      filter.setSubscription(result)
    }

    result.mockSub.canceled should equal(false)
    filter.cancel()
    result.mockSub.canceled should equal(true)
    Mockito.verify(map).clear()
  }

  private def makeUuid(str: String) = ReefUUID.newBuilder.setUuid(str).build
  implicit def makeUuidFromString(str: String): ReefUUID = makeUuid(str)

  def getConfig(enabled: Boolean, routingKey: Option[String]) = {
    val pt = CommChannel.newBuilder.setUuid("port")
    val cfg = CommEndpointConfig.newBuilder.setUuid("config").setChannel(pt).setName("endpoint1")
    val b = CommEndpointConnection.newBuilder.setUid("connection").setEndpoint(cfg).setEnabled(enabled)
    routingKey.foreach { s => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(s).build) }
    b.build
  }
}