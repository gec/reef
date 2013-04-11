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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.client.service.proto.Model.ReefUUID
import processing.MeasurementWhiteList
import org.totalgrid.reef.client.operations.scl.Event
import scala.collection.JavaConversions._
import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType

class EndpointConnectionWatcher(client: MeasurementProcessorServices, endpointUuid: ReefUUID, cache: LastMeasurementCacheManager, whiteList: MeasurementWhiteList) extends Logging {

  import org.totalgrid.reef.client.operations.scl.ScalaSubscription._

  val endpointResult = client.subscribeToEndpointConnection(endpointUuid).await

  endpointResult.getSubscription.onEvent {
    case Event(SubscriptionEventType.REMOVED, endpointConn) => {
      logger.info("Saw event REMOVED to endpoint " + endpointConn.getEndpoint.getName + ", resetting cache.")
      cache.resetCache()
    }
    case Event(SubscriptionEventType.MODIFIED, endpointConn) => {
      logger.info("Saw event MODIFIED to endpoint " + endpointConn.getEndpoint.getName + ", resetting cache.")
      cache.resetCache()
    }
    case Event(SubscriptionEventType.ADDED, endpointConn) => {
      logger.info("Saw event ADDED to endpoint " + endpointConn.getEndpoint.getName + ", updating white list.")
      updateWhiteList()
    }
  }

  private def updateWhiteList() {
    // Redo the point lookup
    val endpoint = client.getEndpointByUuid(endpointUuid).await
    val expectedPoints = endpoint.getOwnerships.getPointsList.toList
    val points = client.getPointsByNames(expectedPoints).await

    whiteList.updatePointList(points)
  }

  def cancel() {
    endpointResult.getSubscription.cancel()
  }
}
