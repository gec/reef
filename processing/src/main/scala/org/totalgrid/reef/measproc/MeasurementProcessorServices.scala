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

import org.totalgrid.reef.api.sapi.client.rpc.impl.AllScadaServiceImpl
import org.totalgrid.reef.messaging.sync.AMQPSyncFactory
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.sapi.request.framework.{ ClientSourceProxy, ReefServiceBaseClass }
import org.totalgrid.reef.promise.Promise
import org.totalgrid.reef.japi.client.SubscriptionResult
import org.totalgrid.reef.api.proto.Application.ApplicationConfig
import org.totalgrid.reef.api.proto.Descriptors
import org.totalgrid.reef.api.proto.Measurements.Measurement
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.api.proto.Model.Point
import org.totalgrid.reef.api.proto.Processing.{ MeasurementProcessingConnection, MeasOverride, TriggerSet }
import org.totalgrid.reef.sapi.{ EventOperations, AddressableDestination }
import org.totalgrid.reef.japi.Envelope

trait MeasurementProcessorServices extends AllScadaServiceImpl {
  def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig): Promise[SubscriptionResult[List[MeasurementProcessingConnection], MeasurementProcessingConnection]]

  def subscribeToTriggerSetsForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[TriggerSet], TriggerSet]]

  def subscribeToOverridesForConnection(conn: MeasurementProcessingConnection): Promise[SubscriptionResult[List[MeasOverride], MeasOverride]]

  def publishIndividualMeasurementAsEvent(meas: Measurement)

  def bindMeasurementProcessingNode(handler: MeasBatchProcessor, conn: MeasurementProcessingConnection): Cancelable

  def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long): Promise[MeasurementProcessingConnection]
}

class MeasurementProcessorServicesImpl(protected val clientSource: AllScadaServiceImpl, factory: AMQPSyncFactory, exe: Executor)
    extends MeasurementProcessorServices with ReefServiceBaseClass with ClientSourceProxy {

  override def subscribeToConnectionsForMeasurementProcessor(measProc: ApplicationConfig) = {
    ops.operation("Couldn't subscribe for endpoints assigned to: " + measProc.getInstanceName) { session =>
      useSubscription(session, Descriptors.measurementProcessingConnection().getKlass) { sub =>
        session.get(MeasurementProcessingConnection.newBuilder.setMeasProc(measProc).build, sub).map { _.expectMany() }
      }
    }
  }

  override def subscribeToTriggerSetsForConnection(conn: MeasurementProcessingConnection) = {
    ops.operation("Couldn't subscribe for triggers associated with endpoint: " + conn.getLogicalNode.getName) { session =>
      useSubscription(session, Descriptors.triggerSet.getKlass) { sub =>
        val point = Point.newBuilder.setLogicalNode(conn.getLogicalNode)
        session.get(TriggerSet.newBuilder.setPoint(point).build, sub).map { _.expectMany() }
      }
    }
  }

  override def subscribeToOverridesForConnection(conn: MeasurementProcessingConnection) = {
    ops.operation("Couldn't subscribe for measurement overrides associated with endpoint: " + conn.getLogicalNode.getName) { session =>
      useSubscription(session, Descriptors.measOverride.getKlass) { sub =>
        val point = Point.newBuilder.setLogicalNode(conn.getLogicalNode)
        session.get(MeasOverride.newBuilder.setPoint(point).build, sub).map { _.expectMany() }
      }
    }
  }

  override def bindMeasurementProcessingNode(handler: MeasBatchProcessor, conn: MeasurementProcessingConnection) = {
    val destination = AddressableDestination(conn.getRouting.getServiceRoutingKey)
    val service = new AddressableMeasurementBatchService(handler)

    val closeable = factory.bindService(Descriptors.measurementBatch.id, service.respond, exe, destination)

    new Cancelable {
      def cancel() = closeable.close()
    }
  }

  private def serializeMeas(meas: Measurement) = EventOperations.getEvent(Envelope.Event.MODIFIED, meas).toByteArray()
  private val measExchange = EventOperations.getExchange(Descriptors.measurement)
  private val measBroadcaster = factory.broadcast(factory.getChannel, serializeMeas)

  override def publishIndividualMeasurementAsEvent(meas: Measurement) {
    // TODO: replace hacky measBroadcaster with client impl
    measBroadcaster(meas, measExchange, meas.getName)
  }

  override def setMeasurementProcessingConnectionReadyTime(conn: MeasurementProcessingConnection, time: Long) = {
    ops.operation("Failed updating measproc: " + conn.getMeasProc.getUuid + " readyTime: " + time) {
      _.put(conn.toBuilder.setReadyTime(time).build).map { _.expectOne }
    }
  }
}