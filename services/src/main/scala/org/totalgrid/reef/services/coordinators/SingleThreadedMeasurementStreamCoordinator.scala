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

import org.totalgrid.reef.models._
import org.totalgrid.reef.services.framework._

/**
 * shunts all updates to the measurement coordinator to a single executor so we only ever have one transaction
 * on the coordinated components at a time avoiding race conditions when we are adding endpoints and applications
 * at the same time.
 */
class SingleThreadedMeasurementStreamCoordinator(real: SquerylBackedMeasurementStreamCoordinator, contextSource: RequestContextSource) extends MeasurementStreamCoordinator {

  private def handle(context: RequestContext)(f: (MeasurementStreamCoordinator, RequestContext) => Unit): Unit = {
    context.operationBuffer.queuePostTransaction {
      // we have to actually give up our original transaction and wait for the lock on the coordinator
      // outside of a transaction. If not its possible to deadlock at the database level because the
      // original transactions may have acquired locks on the tables we are going to try to alter.
      // Postgres can't "see" the deadlock because it doesn't know that our thread
      contextSource.transaction { c =>
        this.synchronized {
          f(real, c)
        }
      }
    }
  }

  def onMeasProcAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean) =
    handle(context) { (r, c) => r.onMeasProcAppChanged(c, reloadApp(app), added) }

  def onMeasProcAssignmentChanged(context: RequestContext, meas: MeasProcAssignment) =
    handle(context) { (r, c) => r.onMeasProcAssignmentChanged(c, reloadMeas(meas)) }

  def onFepConnectionChange(context: RequestContext, sql: FrontEndAssignment, existing: FrontEndAssignment) =
    handle(context) { (r, c) => r.onFepConnectionChange(c, reloadFep(sql), existing) }

  def onFepAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean) =
    handle(context) { (r, c) => r.onFepAppChanged(c, reloadApp(app), added) }

  def onEndpointDeleted(context: RequestContext, ce: CommunicationEndpoint) =
    handle(context) { (r, c) => r.onEndpointDeleted(c, ce) }

  def onEndpointUpdated(context: RequestContext, ce: CommunicationEndpoint, existing: CommunicationEndpoint) =
    handle(context) { (r, c) => r.onEndpointUpdated(c, reloadCe(ce), existing) }

  def onEndpointCreated(context: RequestContext, ce: CommunicationEndpoint) =
    handle(context) { (r, c) => r.onEndpointCreated(c, reloadCe(ce)) }

  import org.totalgrid.reef.client.exception.InternalServiceException
  import org.squeryl.PrimitiveTypeMode._
  private def reloadApp(ce: ApplicationInstance): ApplicationInstance = ApplicationSchema.apps.lookup(ce.id).getOrElse(throw new InternalServiceException("row deleted!"))
  private def reloadMeas(ce: MeasProcAssignment) = ApplicationSchema.measProcAssignments.lookup(ce.id).getOrElse(throw new InternalServiceException("row deleted!"))
  private def reloadFep(ce: FrontEndAssignment) = ApplicationSchema.frontEndAssignments.lookup(ce.id).getOrElse(throw new InternalServiceException("row deleted!"))
  private def reloadCe(ce: CommunicationEndpoint) = ApplicationSchema.endpoints.lookup(ce.id).getOrElse(throw new InternalServiceException("row deleted!"))

}