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
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection
import scala.collection.immutable
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.persistence.{ ObjectCache, InMemoryObjectCache }
import org.totalgrid.reef.app.ServiceContext

object LastMeasurementCacheManager {
  class ResettableInMemoryObjectCache[A] extends InMemoryObjectCache[A] {
    def reset() {
      this.map = immutable.Map.empty[String, A]
    }
  }
}

class LastMeasurementCacheManager(endpoint: String) extends MeasProcServiceContext[EndpointConnection] {
  import LastMeasurementCacheManager._

  def add(obj: EndpointConnection) = {}
  def remove(obj: EndpointConnection) = {}
  def clear() = {}

  private val objCache = new ResettableInMemoryObjectCache[Measurement]
  def cache: ObjectCache[Measurement] = objCache

  override def modify(end: EndpointConnection) = {
    if (end.getEndpoint.getName == endpoint && (end.getEnabled == false || end.getState == EndpointConnection.State.COMMS_DOWN)) {
      objCache.reset()
    }
  }
}