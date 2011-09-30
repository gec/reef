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
package org.totalgrid.reef.services.framework

import org.squeryl.PrimitiveTypeMode

object ServiceTransactable {
  def doTransaction[ModelType <: BufferLike, Output](m: ModelType, fun: ModelType => Output): Output = {
    try {
      val result: Output = PrimitiveTypeMode.inTransaction {
        // Run logic inside sql transaction
        val resultInner: Output = fun(m)

        // Success, render all event notifications that were waiting for the end of the model
        // transaction to be able to read a consistent state, multi model adds for example
        m.flushInTransaction

        // TODO: re-enable chatty transaction logging
        //        val stats = reef.models.CountingSession.currentSession.stats
        //        if (stats.actions > 30) {
        //          println("Chatty Transaction: " + stats)
        //        }

        resultInner
      }

      // now we send all event notifications, knowing
      m.flushPostTransaction

      result
    } finally {

      // On failure, clear the event queue
      m.clear
    }
  }
}