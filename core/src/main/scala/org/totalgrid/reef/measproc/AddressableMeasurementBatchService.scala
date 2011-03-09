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
package org.totalgrid.reef.measproc

import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.api.ServiceTypes.Response
import org.totalgrid.reef.api.service.sync.SyncServiceBase

import org.totalgrid.reef.proto.Measurements.MeasurementBatch
import org.totalgrid.reef.api.{ Envelope, RequestEnv }

class AddressableMeasurementBatchService(measProc: ProcessingNode) extends SyncServiceBase[MeasurementBatch] {

  override val descriptor = Descriptors.measurementBatch

  override def delete(req: MeasurementBatch, env: RequestEnv) = noVerb("delete")
  override def get(req: MeasurementBatch, env: RequestEnv) = noVerb("get")

  override def post(req: MeasurementBatch, env: RequestEnv) = put(req, env)
  override def put(req: MeasurementBatch, env: RequestEnv) = {
    measProc.process(req)
    new Response(Envelope.Status.OK, req)
  }
}
