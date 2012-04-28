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

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.client.service.proto.Measurements.Measurement
import org.totalgrid.reef.client.service.proto.Processing._

import org.totalgrid.reef.client.service.proto.Descriptors
import org.totalgrid.reef.client.proto.Envelope

import org.totalgrid.reef.client.sapi.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.client.service.proto.Model.{ ReefUUID, Point }
import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointConnection }
import org.totalgrid.reef.client.registration.EventPublisher
import org.totalgrid.reef.client.operations.scl.ServiceOperationsProvider
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._
import org.totalgrid.reef.client._

trait MeasurementProcessorServices extends AllScadaServiceImpl {
  def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig): Promise[SubscriptionResult[List[MeasurementProcessingConnection], MeasurementProcessingConnection]]

  def subscribeToEndpointConnection(endpointUuid: ReefUUID): Promise[SubscriptionResult[List[EndpointConnection], EndpointConnection]]

  def subscribeToTriggerSetsForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[TriggerSet], TriggerSet]]

  def subscribeToOverridesForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[MeasOverride], MeasOverride]]

  def publishIndividualMeasurementAsEvent(meas: Measurement)

  def bindMeasurementProcessingNode(handler: MeasBatchProcessor, conn: MeasurementProcessingConnection): Promise[SubscriptionBinding]

  def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long): Promise[MeasurementProcessingConnection]
}

class MeasurementProcessorServicesImpl(client: Client, eventPub: EventPublisher)
    extends ServiceOperationsProvider(client) with MeasurementProcessorServices {

  override def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig) = {
    ops.subscription(Descriptors.measurementProcessingConnection, "Couldn't subscribe for endpoints assigned to: " + measProc.getInstanceName) { (sub, client) =>
      client.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measProc).build, sub).map { _.many }
    }
  }

  override def subscribeToEndpointConnection(endpointUuid: ReefUUID) = {
    ops.subscription(Descriptors.endpointConnection, "Couldn't subscribe to endpoint connection for endpoint: " + endpointUuid) { (sub, client) =>
      client.get(EndpointConnection.newBuilder.setEndpoint(Endpoint.newBuilder.setUuid(endpointUuid)).build, sub).map(_.many)
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
    val service = new AddressableMeasurementBatchService(handler)

    ops.clientSideService(service, "Couldn't register as measurement processor for stream: " + conn.getRouting.getServiceRoutingKey) { (binding, session) =>
      import org.totalgrid.reef.client.service.proto.FEP.{ Endpoint, EndpointConnection, CommandHandlerBinding }

      val bindingProto = MeasurementStreamBinding.newBuilder.setMeasurementQueue(binding.getId).setProcessingConnection(conn).build

      session.post(bindingProto).map { _.one }
    }
  }

  override def publishIndividualMeasurementAsEvent(meas: Measurement) {
    eventPub.publishEvent(Envelope.SubscriptionEventType.MODIFIED, meas, meas.getName)
  }

  override def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long) = {
    ops.operation("Failed updating measproc: " + conn.getMeasProc.getUuid + " readyTime: " + time) {
      _.put(conn.toBuilder.setReadyTime(time).build).map { _.one }
    }
  }
}