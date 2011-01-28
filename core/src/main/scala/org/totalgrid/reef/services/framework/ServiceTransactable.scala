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
package org.totalgrid.reef.services.framework

import org.squeryl.PrimitiveTypeMode
import org.totalgrid.reef.services.{ ServiceSubscriptionHandler }

/**
 * Interface for components capable of constructing a service model.
 */
trait ModelFactory[+ModelType] {
  def model: ModelType
}

/**
 * Interface for components that perform transactional behavior, providing a stateful
 * model for use during the transaction.
 */
trait ServiceTransactable[+ModelType] {
  def transaction[R](fun: ModelType => R): R
  def messageType: Class[_]
}

/**
 *  Generic component that maintains a ServiceSubscriptionHandler resource and provides an implementation of 
 *  transactional/buffered behavior. Inherited classes provide the factory method for instantiating model objects
 *  given the ServiceSubscriptionHandler.
 */
trait BasicServiceTransactable[+ModelType <: BufferLike]
    extends ServiceTransactable[ModelType] {

  protected val subHandler: ServiceSubscriptionHandler
  def model: ModelType

  def transaction[R](fun: ModelType => R): R = {
    val m = model
    try {
      PrimitiveTypeMode.transaction {
        // Run logic inside sql transaction
        val result = fun(m)

        // Success, send all event notifications
        m.flush

        // TODO: re-enable chatty transaction logging
        //        val stats = reef.models.CountingSession.currentSession.stats
        //        if (stats.actions > 30) {
        //          // TODO: put into env variable
        //          println("Chatty Transaction: " + stats)
        //        }

        // Return result
        result
      }
    } finally {

      // On failure, clear the event queue
      m.clear
    }
  }
}