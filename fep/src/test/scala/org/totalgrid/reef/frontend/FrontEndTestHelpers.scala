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

import org.totalgrid.reef.client.service.proto.Model.{ ReefID, ReefUUID }
import org.totalgrid.reef.client.service.proto.FEP.{ CommEndpointRouting, Endpoint, CommChannel, EndpointConnection }
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.app.SubscriptionHandler
import org.totalgrid.reef.client.SubscriptionResult

object FrontEndTestHelpers {

  private def makeUuid(str: String) = ReefUUID.newBuilder.setValue(str).build
  implicit def makeUuidFromString(str: String): ReefUUID = makeUuid(str)

  private def makeId(str: String) = ReefID.newBuilder.setValue(str).build
  implicit def makeIdFromString(str: String): ReefID = makeId(str)

  def getConnectionProto(enabled: Boolean, routingKey: Option[String]) = {
    val pt = CommChannel.newBuilder.setUuid("port").setName("port")
    val cfg = Endpoint.newBuilder.setProtocol("mock").setUuid("config").setChannel(pt).setName("endpoint1")
    val b = EndpointConnection.newBuilder.setId("connection").setEndpoint(cfg).setEnabled(enabled)
    routingKey.foreach { s => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(s).build) }
    b.build
  }

  class MockSubscriptionHandler[A] extends SubscriptionHandler[A] with Cancelable {
    var sub = Option.empty[SubscriptionResult[List[A], A]]
    var canceled = false

    def setSubscription(result: SubscriptionResult[List[A], A]) = {
      sub = Some(result)
      this
    }
    def cancel() = {
      canceled = true
    }
  }
}