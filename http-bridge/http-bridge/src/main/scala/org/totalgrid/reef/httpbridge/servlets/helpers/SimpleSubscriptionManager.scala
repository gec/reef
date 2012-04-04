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
package org.totalgrid.reef.httpbridge.servlets.helpers

import com.google.protobuf.Message
import scala.collection.mutable
import org.totalgrid.reef.client.proto.Envelope
import org.totalgrid.reef.client.exception.BadRequestException
import org.totalgrid.reef.client.{ SubscriptionEvent, SubscriptionEventAcceptor, Subscription }
import com.weiglewilczek.slf4s.Logging

/**
 * simplest possible SubscriptionHandler that just stores all the subscription events in a buffer
 * until they are retrieved by the client.
 */
class SimpleSubscriptionManager extends SubscriptionHandler with Logging {

  private val MAX_EVENT_SIZE = 1000

  class SubscriptionValueHolder[A <: Message](sub: Subscription[A]) extends SubscriptionEventAcceptor[A] {
    private val queue = mutable.Queue.empty[(Envelope.SubscriptionEventType, Message)]

    sub.start(this)

    def onEvent(subEvent: SubscriptionEvent[A]) = this.synchronized {
      queue.enqueue((subEvent.getEventType, subEvent.getValue))
      if (queue.length > MAX_EVENT_SIZE) {
        logger.error(sub.getId + " subscription overflowed, canceling")
        cancel()
      }
    }

    def poll(maxEvents: Int): List[(Envelope.SubscriptionEventType, Message)] = this.synchronized {
      val ret = queue.take(maxEvents).toList
      ret.foreach { i => queue.dequeue() }
      ret
    }

    def cancel() = this.synchronized {
      sub.cancel()
      queue.clear()
    }
  }

  private var subscriptionQueues = Map.empty[String, SubscriptionValueHolder[_ <: Message]]

  def addSubscription[A <: Message](subscription: Subscription[A]) = this.synchronized {
    // make a token for the subscription user (we'll use the one provided by qpid for now) but
    // we may change that later
    val subToken = subscription.getId

    subscriptionQueues += subToken -> new SubscriptionValueHolder(subscription)

    subToken
  }

  def getValueHolder(id: String): SubscriptionValueHolder[_ <: Message] = this.synchronized {
    subscriptionQueues.get(id).getOrElse(
      throw new BadRequestException("Unknown subscription id: " + id + ", it may have already been canceled."))
  }

  def removeValueHolder(id: String) = this.synchronized {
    subscriptionQueues.get(id) match {
      case Some(holder) =>
        subscriptionQueues = subscriptionQueues - id
        holder.cancel()
      case None => // do nothing its already not in map, we have probably already deleted it
    }
  }
}