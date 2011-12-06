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

import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.Measurements.Measurement
import net.agileautomata.executor4s.Cancelable
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection, MeasOverride, TriggerSet }

import org.totalgrid.reef.client.sapi.client.rpc.framework.ApiBase
import org.totalgrid.reef.client.sapi.client.Promise
import org.totalgrid.reef.client.sapi.client.rest.Client
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.client.{ SubscriptionResult, AddressableDestination }

trait MeasurementProcessorServices extends AllScadaServiceImpl {
  def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig): Promise[SubscriptionResult[List[MeasurementProcessingConnection], MeasurementProcessingConnection]]

  def subscribeToTriggerSetsForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[TriggerSet], TriggerSet]]

  def subscribeToOverridesForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[MeasOverride], MeasOverride]]

  def publishIndividualMeasurementAsEvent(meas: Measurement)

  def bindMeasurementProcessingNode(handler: MeasBatchProcessor, conn: MeasurementProcessingConnection): Cancelable

  def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long): Promise[MeasurementProcessingConnection]
}

class MeasurementProcessorServicesImpl(client: Client)
    extends ApiBase(client) with MeasurementProcessorServices {

  override def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig) = {
    ops.subscription(Descriptors.measurementProcessingConnection, "Couldn't subscribe for endpoints assigned to: " + measProc.getInstanceName) { (sub, client) =>
      client.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measProc).build, sub).map { _.many }
    }
  }

  override def subscribeToTriggerSetsForConnection(conn: MeasurementProcessingConnection) = {
    ops.subscription(Descriptors.triggerSet, "Couldn't subscribe for triggers associated with endpoint: " + conn.getLogicalNode.getName) { (sub, client) =>
      val point = Point.newBuilder.setEndpoint(conn.getLogicalNode)
      client.get(TriggerSet.newBuilder.setPoint(point).build, sub).map { _.many }
    }
  }

  override def subscribeToOverridesForConnection(conn: MeasurementProcessingConnection) = {
    ops.subscription(Descriptors.measOverride, "Couldn't subscribe for measurement overrides associated with endpoint: " + conn.getLogicalNode.getName) { (sub, client) =>
      val point = Point.newBuilder.setEndpoint(conn.getLogicalNode)
      client.get(MeasOverride.newBuilder.setPoint(point).build, sub).map { _.many }
    }
  }

  override def bindMeasurementProcessingNode(handler: MeasBatchProcessor, conn: MeasurementProcessingConnection) = {
    val destination = new AddressableDestination(conn.getRouting.getServiceRoutingKey)
    val service = new AddressableMeasurementBatchService(handler)

    val closeable = client.bindService(service, client, destination, false)

    new Cancelable {
      def cancel() = closeable.cancel()
    }
  }

  override def publishIndividualMeasurementAsEvent(meas: Measurement) {
    client.publishEvent(Envelope.SubscriptionEventType.MODIFIED, meas, meas.getName)
  }

  override def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long) = {
    ops.operation("Failed updating measproc: " + conn.getMeasProc.getUuid + " readyTime: " + time) {
      _.put(conn.toBuilder.setReadyTime(time).build).map { _.one }
    }
  }
}