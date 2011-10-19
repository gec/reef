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

import org.totalgrid.reef.api.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.api.sapi.service.SyncServiceBase
import org.totalgrid.reef.api.sapi.impl.Descriptors
import org.totalgrid.reef.api.sapi.client.{ Response, BasicRequestHeaders }
import org.totalgrid.reef.api.japi.Envelope

trait MeasBatchProcessor {
  def process(m: MeasurementBatch)
}

class AddressableMeasurementBatchService(measProc: MeasBatchProcessor) extends SyncServiceBase[MeasurementBatch] {

  override val descriptor = Descriptors.measurementBatch

  override def post(req: MeasurementBatch, env: BasicRequestHeaders) = put(req, env)
  override def put(req: MeasurementBatch, env: BasicRequestHeaders) = {
    measProc.process(req)
    Response(Envelope.Status.OK, req :: Nil)
  }
}
