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

import org.totalgrid.reef.sapi.request.impl.AllScadaServiceImpl
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.sapi.request.framework.{ ClientSourceProxy, ReefServiceBaseClass }
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.japi.client.SubscriptionResult
import org.totalgrid.reef.proto.Application.ApplicationConfig
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.proto.Model.Point
import org.totalgrid.reef.sapi.AddressableDestination
import org.totalgrid.reef.proto.Processing.{ MeasurementProcessingConnection, MeasOverride, TriggerSet }

trait MeasurementProcessorServices extends AllScadaServiceImpl {
  def subscribeToEndpointConnectionsForMeasurementProcessor(measProc: ApplicationConfig): Promise[SubscriptionResult[List[MeasurementProcessingConnection], MeasurementProcessingConnection]]

  def subscribeToTriggerSetsForEndpoint(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[TriggerSet], TriggerSet]]

  def subscribeToOverridesForEndpoint(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[MeasOverride], MeasOverride]]

  def publishIndividualMeasurementAsEvent(meas: Measurement)

  def bindMeasurementProcessingNode(handler: MeasurementBatchProcessor, conn: MeasurementProcessingConnection): Cancelable
}

class MeasurementProcessorServicesImpl(protected val clientSource: AllScadaServiceImpl, factory: AMQPSyncFactory, exe: Executor)
    extends MeasurementProcessorServices with ReefServiceBaseClass with ClientSourceProxy {

  override def subscribeToEndpointConnectionsForMeasurementProcessor(measProc: ApplicationConfig) = {
    ops("Couldn't subscribe for endpoints assigned to: " + measProc.getInstanceName) { session =>
      useSubscription(session, Descriptors.measurementProcessingConnection().getKlass) { sub =>
        session.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measProc).build, sub).map { _.expectMany() }
      }
    }
  }

  override def subscribeToTriggerSetsForEndpoint(conn: MeasurementProcessingConnection) = {
    ops("Couldn't subscribe for triggers associated with endpoint: " + conn.getLogicalNode.getName) { session =>
      useSubscription(session, Descriptors.triggerSet.getKlass) { sub =>
        val point = Point.newBuilder.setLogicalNode(conn.getLogicalNode)
        session.get(TriggerSet.newBuilder.setPoint(point).build, sub).map { _.expectMany() }
      }
    }
  }

  override def subscribeToOverridesForEndpoint(conn: MeasurementProcessingConnection) = {
    ops("Couldn't subscribe for measurement overrides associated with endpoint: " + conn.getLogicalNode.getName) { session =>
      useSubscription(session, Descriptors.measOverride.getKlass) { sub =>
        val point = Point.newBuilder.setLogicalNode(conn.getLogicalNode)
        session.get(MeasOverride.newBuilder.setPoint(point).build, sub).map { _.expectMany() }
      }
    }
  }

  override def bindMeasurementProcessingNode(handler: MeasurementBatchProcessor, conn: MeasurementProcessingConnection) = {
    val destination = AddressableDestination(conn.getRouting.getServiceRoutingKey)
    val service = new AddressableMeasurementBatchService(handler)

    val closeable = factory.bindService(Descriptors.measurementBatch.id, service.respond, exe, destination)

    new Cancelable {
      def cancel() = closeable.close()
    }
  }

  override def publishIndividualMeasurementAsEvent(meas: Measurement) {
    // TODO: publish individual meas updates
  }
}