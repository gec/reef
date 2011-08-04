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

import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.models.{ CommunicationEndpoint, FrontEndAssignment, MeasProcAssignment, ApplicationInstance }
import org.totalgrid.reef.services.framework.BasicServiceTransactable

/**
 * shunts all updates to the measurement coordinator to a single executor so we only ever have one transaction
 * on the coordinated components at a time avoiding race conditions when we are adding endpoints and applications
 * at the same time.
 */
class SingleThreadedMeasurementStreamCoordinator(real: SquerylBackedMeasurementStreamCoordinator, executor: Executor) extends MeasurementStreamCoordinator {

  private var workQueue = List.empty[MeasurementStreamCoordinator => Unit]
  private def handle(f: MeasurementStreamCoordinator => Unit): Unit = {
    workQueue ::= f
  }

  def onMeasProcAppChanged(app: ApplicationInstance, added: Boolean) = handle { _.onMeasProcAppChanged(app, added) }

  def onMeasProcAssignmentChanged(meas: MeasProcAssignment) = handle { _.onMeasProcAssignmentChanged(meas) }

  def onFepConnectionChange(sql: FrontEndAssignment, existing: FrontEndAssignment) = handle { _.onFepConnectionChange(sql, existing) }

  def onFepAppChanged(app: ApplicationInstance, added: Boolean) = handle { _.onFepAppChanged(app, added) }

  def onEndpointDeleted(ce: CommunicationEndpoint) = handle { _.onEndpointDeleted(ce) }

  def onEndpointUpdated(ce: CommunicationEndpoint) = handle { _.onEndpointUpdated(ce) }

  def onEndpointCreated(ce: CommunicationEndpoint) = handle { _.onEndpointCreated(ce) }

  def clear = { workQueue = Nil }

  def flushPostTransaction = {
    workQueue.reverse.foreach { f =>
      executor.request {
        BasicServiceTransactable.doTransaction(real, f)
      }
    }
  }

  def flushInTransaction = {}
}