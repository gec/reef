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
import com.weiglewilczek.slf4s.Logging

class EndpointConnectionWatcher(client: MeasurementProcessorServices, endpointUuid: ReefUUID, cache: LastMeasurementCacheManager, whiteList: MeasurementWhiteList) extends Logging {

  import org.totalgrid.reef.client.operations.scl.ScalaSubscription._

  val endpointResult = client.subscribeToEndpointConnection(endpointUuid).await

  endpointResult.getSubscription.onEvent {
    case Event(anyType, endpointConn) => {

      logger.info("Saw event " + anyType + " to endpoint " + endpointConn.getEndpoint.getName + ", resetting cache and white list.")

      cache.resetCache()

      // Redo the point lookup
      val endpoint = client.getEndpointByUuid(endpointUuid).await
      val expectedPoints = endpoint.getOwnerships.getPointsList.toList
      val points = client.getPointsByNames(expectedPoints).await

      whiteList.updatePointList(points)
    }
  }

  def cancel() {
    endpointResult.getSubscription.cancel()
  }
}
