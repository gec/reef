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
package org.totalgrid.reef.app

import org.totalgrid.reef.japi.Envelope
import org.totalgrid.reef.util.{ Cancelable, Observable }
import org.totalgrid.reef.japi.client.{ SubscriptionEvent, SubscriptionEventAcceptor, SubscriptionResult }
import org.totalgrid.reef.executor.Executor

object ServiceContext {
  // TODO: remove exe when clients are fully stranded
  def attachToServiceContext[T <: List[U], U](result: SubscriptionResult[T, U], context: ServiceContext[U], exe: Executor): Cancelable = {
    context.handleResponse(result.getResult)

    val sub = result.getSubscription

    sub.start(new SubscriptionEventAcceptor[U] {
      def onEvent(event: SubscriptionEvent[U]) {
        exe.execute {
          context.handleEvent(event.getEventType, event.getValue)
        }
      }
    })

    new Cancelable {
      def cancel() = sub.cancel
    }
  }
}

trait O

/**
 * Implements a single resource service consumer.
 * Treats subscription and addition as the same (push).
 * Notifies registered observers when a new subscription
 * event occurs. Provides event handlers suitable for use
 * with ServiceHandler
 */
trait ServiceContext[A] extends Observable {

  // Define these functions
  def add(obj: A)
  def remove(obj: A)
  def modify(obj: A)
  def subscribed(list: List[A])

  def handleResponse(result: List[A]) = {
    subscribed(result)
    notifyObservers()
  }

  def handleEvent(event: Envelope.Event, result: A) = event match {
    case Envelope.Event.ADDED => add(result)
    case Envelope.Event.MODIFIED => modify(result)
    case Envelope.Event.REMOVED => remove(result)
  }

}
