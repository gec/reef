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
import org.totalgrid.reef.client.service.proto.Model.ConfigFile
import org.totalgrid.reef.client.service.proto.Measurements.{ Measurement, MeasurementBatch }
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection }
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.client.sapi.rpc.AllScadaService
import org.totalgrid.reef.protocol.api.{ NullCommandHandler, ChannelIgnoringProtocol, Publisher }
import org.totalgrid.reef.metrics.MetricsSink
import org.totalgrid.reef.calc.lib.eval.BasicOperations
import org.totalgrid.reef.calc.lib._

class CalculatorProtocol extends ChannelIgnoringProtocol {
  def name = "calculator"

  val metrics = MetricsSink.getInstance("calculator")

  var managers = Map.empty[String, (Cancelable, Publisher[EndpointConnection.State])]

  def addEndpoint(endpointName: String,
    channelName: String,
    config: List[ConfigFile],
    batchPublisher: Publisher[MeasurementBatch],
    endpointPublisher: Publisher[EndpointConnection.State], client: Client) = {

    val service = client.getRpcInterface(classOf[AllScadaService])

    val metricsPublisher = new CalculationMetricsSource(metrics.getStore(endpointName), true)

    val measPublisher = new OutputPublisher {
      def publish(m: Measurement) = {
        val batch = MeasurementBatch.newBuilder.setWallTime(System.currentTimeMillis).addMeas(m).build
        batchPublisher.publish(batch)
      }
    }

    val factory = new BasicCalculationFactory(client,
      BasicOperations.getSource,
      metricsPublisher,
      measPublisher,
      SystemTimeSource)

    val manager = new CalculationManager(factory)

    managers += endpointName -> (manager, endpointPublisher)

    val endpoint = service.getEndpointByName(endpointName).await

    val calcSubscription = service.subscribeToCalculationsSourcedByEndpointByUuid(endpoint.getUuid).await

    manager.setSubscription(calcSubscription)

    endpointPublisher.publish(EndpointConnection.State.COMMS_UP)

    NullCommandHandler
  }

  def removeEndpoint(endpointName: String) {
    managers.get(endpointName).foreach {
      case (manager, endpointPublisher) =>
        manager.cancel()
        endpointPublisher.publish(EndpointConnection.State.COMMS_DOWN)
        managers -= endpointName
    }
  }

}
