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
package org.totalgrid.reef.services.coordinators

import org.totalgrid.reef.messaging.AMQPProtoFactory
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.services.ProtoServiceCoordinator
import org.totalgrid.reef.japi.ReefServiceException
import org.totalgrid.reef.util.Logging

import org.totalgrid.reef.proto.Events.Event
import org.totalgrid.reef.services.core.EventServiceModel
import org.totalgrid.reef.services.framework._

/**
 * simple thunker that takes "raw events" from the bus and calls the "put" method from the EventService
 * to put the event into the database.
 */
class EventStreamThunker(eventModel: ServiceTransactable[EventServiceModel], rawEventExchanges: List[String]) extends ProtoServiceCoordinator with Logging {

  def addAMQPConsumers(amqp: AMQPProtoFactory, reactor: Executor) {
    // shift the processing of the event onto another thread
    val context = new SimpleRequestContext
    val callback = (e: Event) => reactor.execute { handleEventMessage(context, e) }
    rawEventExchanges.foreach(exchange => amqp.listen(exchange + "_server", exchange, Event.parseFrom(_), callback))
  }

  def handleEventMessage(context: RequestContext, msg: Event) {
    try {
      // notice we are skipping the event service preCreate step that strips time and userId
      // because our local trusted service components have already set those values correctly
      BasicServiceTransactable.doTransaction(context.events, { buffer: OperationBuffer =>
        eventModel.transaction { _.createFromProto(context, msg) }
      })
    } catch {
      case e: ReefServiceException =>
        logger.warn("Service Exception thunking event: " + e.getMessage)
    }
  }
}