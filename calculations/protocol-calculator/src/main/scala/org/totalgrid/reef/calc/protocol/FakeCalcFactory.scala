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
package org.totalgrid.reef.calc.protocol

import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.client.service.proto.Calculations.Calculation
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.client.service.proto.Measurements.{ DetailQual, Quality, Measurement }
import net.agileautomata.executor4s._

class FakeCalcFactory(rootClient: Client) {
  def make(calc: Calculation) = {
    val client = rootClient.spawn()

    val service = client.getRpcInterface(classOf[AllScadaService])

    val startTime = System.currentTimeMillis()

    // makes a fake measurement with the amount of time since we started calculating
    def makeMeas(pointName: String) = {
      val now = System.currentTimeMillis()
      Measurement.newBuilder.setName(pointName)
        .setIntVal((now - startTime) / 1000).setType(Measurement.Type.INT)
        .setUnit("fake").setTime(now)
        .setQuality(Quality.newBuilder.setValidity(Quality.Validity.QUESTIONABLE)
          .setDetailQual(DetailQual.newBuilder.setBadReference(false)).setSource(Quality.Source.SUBSTITUTED)).build
    }

    val timer = client.scheduleWithFixedOffset(1000.milliseconds) {
      service.publishMeasurements(makeMeas(calc.getOutputPoint.getName) :: Nil).await
    }

    new Cancelable {
      def cancel() {
        timer.cancel()
      }
    }
  }
}