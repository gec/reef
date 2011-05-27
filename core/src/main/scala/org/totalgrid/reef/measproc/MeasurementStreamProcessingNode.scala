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

import org.totalgrid.reef.persistence.ObjectCache
import org.totalgrid.reef.util.{ Logging }
import org.totalgrid.reef.executor.{ Executor, Lifecycle }

import org.totalgrid.reef.messaging.{ AMQPProtoFactory, AMQPProtoRegistry }
import org.totalgrid.reef.app.{ ServiceHandlerProvider, ServiceHandler }
import org.totalgrid.reef.sapi.AddressableDestination
import org.totalgrid.reef.metrics.MetricsHookContainer
import org.totalgrid.reef.proto._

import Measurements._
import Processing._

/**
 * This class encapsulates all of the objects and functionality to process a stream of measurements from one endpoint.
 * A measurement processor node may have many processing nodes, some or all of the passed in resources can be shared
 * across some or all of those nodes.
 */
class MeasurementStreamProcessingNode(
  amqp: AMQPProtoFactory,
  registry: AMQPProtoRegistry,
  measCache: ObjectCache[Measurement],
  overCache: ObjectCache[Measurement],
  stateCache: ObjectCache[Boolean],
  connection: MeasurementProcessingConnection,
  reactor: Executor with Lifecycle)
    extends Logging with MetricsHookContainer {
  // the main actor 
  val provider = new ServiceHandlerProvider(registry, new ServiceHandler { def execute(fun: => Unit) = reactor.execute(fun) })

  val eventSink = amqp.publish(connection.getRouting.getRawEventDest, RoutingKeys.event, Descriptors.event.serialize)

  val processor = ProcessingNode(
    amqp.publish(connection.getRouting.getProcessedMeasDest, RoutingKeys.measurement, Descriptors.measurement.serialize),
    connection.getLogicalNode,
    provider,
    measCache,
    overCache,
    stateCache,
    "streamproc-" + connection.getLogicalNode.getName,
    eventSink,
    startProcessing)

  // once all the components have the pieces they need to process, we subscribe to measurements
  // we'll process them on the chainActor 
  private def startProcessing() {
    MeasurementStreamProcessingNode.attachNode(processor, connection, amqp, reactor)
    val client = registry.getClientSession()

    val connectionBuilder = connection.toBuilder.setReadyTime(System.currentTimeMillis)

    client.put(connectionBuilder.build).await().expectOne
  }

  def start() = reactor.start
  def stop() = reactor.stop

  addHookedObject(processor)
}

object MeasurementStreamProcessingNode extends Logging {
  def attachNode(processor: ProcessingNode, connection: MeasurementProcessingConnection, amqp: AMQPProtoFactory, reactor: Executor) {
    //    val queue = connection.getRouting.getMeasBatchQueue
    //    info("Configured, listening to measurements on queue: " + queue)
    //    amqp.listen(queue, MeasurementBatch.parseFrom, { batch: MeasurementBatch =>
    //      reactor.execute(processor.process(batch))
    //    })
    val measBatchService = new AddressableMeasurementBatchService(processor)
    val exchange = measBatchService.descriptor.id
    logger.info("Attached " + exchange + " key: " + connection.getRouting.getServiceRoutingKey)
    amqp.bindService(exchange, measBatchService.respond, AddressableDestination(connection.getRouting.getServiceRoutingKey), false, Some(reactor))
  }
}