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
package org.totalgrid.reef.event

import org.totalgrid.reef.executor.Executor

import org.totalgrid.reef.util.Localizer

import org.totalgrid.reef.proto.Events._
import org.totalgrid.reef.proto.Alarms._
import org.totalgrid.reef.messaging.{ Connection, AMQPProtoFactory }
import org.totalgrid.reef.app.ServiceHandler
import org.totalgrid.reef.proto.{ Descriptors, RoutingKeys }

/**
 *  Process and route raw events based on the EventConfiguration.
 *  If it's a real event, persist it and post it to exchange: Processed Event.
 *  If it's a log, post it to the raw logs exchange.
 *
 *  @param amqp
 *  @param processedEventExchange   Destination for events
 *  @param processedLogExchange   Destination for logs
 *  @param rawEventExchange   Source of raw events
 *  @param registry
 *
 *  See EventRouterContext for details.
 *  // TODO: delete EventRouter reef_techdebt-8
 */
abstract class EventRouter(
  amqp: AMQPProtoFactory,
  processedEventExchange: String,
  processedLogExchange: String,
  rawEventExchanges: List[String],
  conn: Connection,
  logger: EventLogPublisher)
    extends Executor with ServiceHandler with Localizer {

  private val publishEvent = amqp.publish(processedEventExchange, RoutingKeys.event, Descriptors.event.serialize)
  private val publishLog = amqp.publish(processedLogExchange, RoutingKeys.log, Descriptors.log.serialize)

  val context = new EventRouterContext(publishEvent, publishLog, storeEvent)

  // don't call subscribe to the event stream until we have load the configuration 
  context.register(() => {
    // Subscribe to events and process them in the actor thread  
    rawEventExchanges.foreach(exchange =>
      amqp.listen(exchange + "_server", exchange, Event.parseFrom(_), (e: Event) => execute { context.process(e) }))
  })

  val subscribeMessage = EventConfig.newBuilder.setEventType("*").build
  // handle all of the service calls in the actor thread
  this.addServiceContext[EventConfig](conn, 5000, EventConfig.parseFrom, subscribeMessage, context)

  /**
   *  Store event in persistent store.
   */
  def storeEvent(e: Event) {
    import org.squeryl.PrimitiveTypeMode._
    import org.squeryl.Table
    import org.totalgrid.reef.models.{ ApplicationSchema, EventStore }

    val es = EventStore(
      e.getEventType,
      e.getAlarm,
      e.getTime,
      e.getDeviceTime,
      e.getSeverity,
      e.getSubsystem,
      e.getUserId,
      None,
      Array[Byte](), // TODO: stream out event args to byte array.
      "")

    transaction {
      ApplicationSchema.events.insert(es)
    }

  }
}