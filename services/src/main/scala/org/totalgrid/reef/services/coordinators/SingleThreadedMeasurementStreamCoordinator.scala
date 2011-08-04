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
import org.totalgrid.reef.models._
import org.totalgrid.reef.services.framework._

/**
 * shunts all updates to the measurement coordinator to a single executor so we only ever have one transaction
 * on the coordinated components at a time avoiding race conditions when we are adding endpoints and applications
 * at the same time.
 */
class SingleThreadedMeasurementStreamCoordinator(real: SquerylBackedMeasurementStreamCoordinator, contextSource: RequestContextSource, executor: Executor) extends MeasurementStreamCoordinator {

  private def handle(context: RequestContext)(f: (MeasurementStreamCoordinator, RequestContext) => Unit): Unit = {
    context.operationBuffer.queuePostTransaction {
      executor.request {
        contextSource.transaction { c2 =>
          f(real, c2)
        }
      }
    }
  }

  def onMeasProcAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean) = handle(context) { (r, c) => r.onMeasProcAppChanged(c, app, added) }

  def onMeasProcAssignmentChanged(context: RequestContext, meas: MeasProcAssignment) = handle(context) { (r, c) => r.onMeasProcAssignmentChanged(c, meas) }

  def onFepConnectionChange(context: RequestContext, sql: FrontEndAssignment, existing: FrontEndAssignment) = handle(context) { (r, c) => r.onFepConnectionChange(c, sql, existing) }

  def onFepAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean) = handle(context) { (r, c) => r.onFepAppChanged(c, app, added) }

  def onEndpointDeleted(context: RequestContext, ce: CommunicationEndpoint) = handle(context) { (r, c) => r.onEndpointDeleted(c, ce) }

  def onEndpointUpdated(context: RequestContext, ce: CommunicationEndpoint) = handle(context) { (r, c) => r.onEndpointUpdated(c, ce) }

  def onEndpointCreated(context: RequestContext, ce: CommunicationEndpoint) = handle(context) { (r, c) => r.onEndpointCreated(c, ce) }

}