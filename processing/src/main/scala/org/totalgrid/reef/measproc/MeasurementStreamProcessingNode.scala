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

import scala.collection.JavaConversions._

import org.totalgrid.reef.metrics.MetricsHookContainer
import com.weiglewilczek.slf4s.Logging
import org.totalgrid.reef.proto.Processing.MeasurementProcessingConnection
import org.totalgrid.reef.proto.Events.Event
import org.totalgrid.reef.client.exceptions.ReefServiceException

import org.totalgrid.reef.proto.Measurements.Measurement
import org.totalgrid.reef.measproc.pipeline.MeasProcessingPipeline

/**
 * This class encapsulates all of the objects and functionality to process a stream of measurements from one endpoint.
 * A measurement processor node may have many processing nodes, some or all of the passed in resources can be shared
 * across some or all of those nodes.
 */
class MeasurementStreamProcessingNode(
  client: MeasurementProcessorServices,
  caches: MeasProcObjectCaches,
  connection: MeasurementProcessingConnection)
    extends Logging with MetricsHookContainer {

  def publishEvent(event: Event.Builder) = try {
    event.setUserId("system")
    event.setSubsystem("measproc")
    client.publishEvent(event.build)
  } catch {
    case rse: ReefServiceException =>
      logger.warn("Couldn't publish event: " + rse.getMessage, rse)
  }

  def measSink(meas: Measurement) = try {
    client.publishIndividualMeasurementAsEvent(meas)
  } catch {
    case rse: ReefServiceException =>
      logger.warn("Couldn't publish measurement: " + meas.getName + " message: " + rse.getMessage, rse)
  }

  val endpoint = client.getEndpointByUuid(connection.getLogicalNode.getUuid).await
  val expectedPoints = endpoint.getOwnerships.getPointsList.toList

  val processingPipeline = new MeasProcessingPipeline(caches, measSink _, publishEvent _, expectedPoints)

  addHookedObject(processingPipeline)

  val overrideResult = client.subscribeToOverridesForConnection(connection).await
  val overrideSub = processingPipeline.overProc.setSubscription(overrideResult)

  val triggerResult = client.subscribeToTriggerSetsForConnection(connection).await
  val triggerSub = processingPipeline.triggerProc.setSubscription(triggerResult)

  val binding = client.bindMeasurementProcessingNode(processingPipeline, connection)

  client.setMeasurementProcessingConnectionReadyTime(connection, System.currentTimeMillis()).await

  def cancel() = {
    binding.cancel
    triggerSub.cancel
    overrideSub.cancel
  }
}
