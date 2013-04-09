/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.app

import net.agileautomata.executor4s.Cancelable

import org.totalgrid.reef.client.proto.Envelope.SubscriptionEventType

import com.typesafe.scalalogging.slf4j.Logging
import org.totalgrid.reef.client.{ SubscriptionResult, SubscriptionEvent, SubscriptionEventAcceptor }

object ServiceContext {
  def attachToServiceContext[T <: List[U], U](result: SubscriptionResult[T, U], context: SubscriptionDataHandler[U]): Cancelable = {
    context.handleResponse(result.getResult)

    var canceled = false
    val sub = result.getSubscription

    sub.start(new SubscriptionEventAcceptor[U] with Logging {
      def onEvent(event: SubscriptionEvent[U]) = sub.synchronized {
        if (!canceled) context.handleEvent(event.getEventType, event.getValue)
        else logger.info("Discarding canceled subscription event: " + event.getValue().asInstanceOf[AnyRef].getClass.getSimpleName)
      }
    })

    new Cancelable {
      def cancel() = sub.synchronized {
        canceled = true
        sub.cancel
      }
    }
  }
}

/**
 * Implements a single resource service consumer.
 * Treats subscription and addition as the same (push).
 * Notifies registered observers when a new subscription
 * event occurs. Provides event handlers suitable for use
 * with ServiceHandler
 */
trait ServiceContext[A] extends SubscriptionDataHandler[A] {

  // Define these functions
  def add(obj: A)
  def remove(obj: A)
  def modify(obj: A)
  def subscribed(list: List[A])
  def clear()

  def handleResponse(result: List[A]) = {
    subscribed(result)
  }

  def handleEvent(event: SubscriptionEventType, result: A) = event match {
    case SubscriptionEventType.ADDED => add(result)
    case SubscriptionEventType.MODIFIED => modify(result)
    case SubscriptionEventType.REMOVED => remove(result)
  }

}

trait SubscriptionDataHandler[A] {
  def handleResponse(result: List[A])

  def handleEvent(event: SubscriptionEventType, result: A)
}
