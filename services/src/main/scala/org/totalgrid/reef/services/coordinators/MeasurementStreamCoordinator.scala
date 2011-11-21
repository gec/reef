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
 * defines the callbacks from each of the other services used in assigning a front end processor
 * and measurement processor for each endpoint.
 */
trait MeasurementStreamCoordinator {

  /**
   * whenever an endpoint is created we then create 2 objects representing the endpoint -> meas proc and
   * endpoint -> fep connections. We assign a measproc if we can instantly but always delay fep assignement
   * because the measproc cannot be ready yet.
   */
  def onEndpointCreated(context: RequestContext, ce: CommunicationEndpoint)

  /**
   * whenever the endpoint definition is updated we check the meas proc assignment and forcefully
   * reassign each fep
   */
  def onEndpointUpdated(context: RequestContext, ce: CommunicationEndpoint, existing: CommunicationEndpoint)

  /**
   * endpoints should only be deleted when they are not enabled so we can simply delete the assignment protos
   */
  def onEndpointDeleted(context: RequestContext, ce: CommunicationEndpoint)

  /**
   * when an fep comes or goes we check either all unassigned endpoints when an fep has been added
   * or try to reassign all the endpoints that were using an FEP thats being removed
   */
  def onFepAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean)

  /**
   * called when the protocol updates the communication status (goes online or offline) or the enabled
   * flag is updated
   */
  def onFepConnectionChange(context: RequestContext, sql: FrontEndAssignment, existing: FrontEndAssignment)

  /**
   * whenever the meas proc table is updated we want to sure we re-evaluate the fep assignments
   * to either enable or disable them
   */
  def onMeasProcAssignmentChanged(context: RequestContext, meas: MeasProcAssignment)

  /**
   * when a new measurement processor is added or removed we want to check all of the meas proc connection objects
   * that were unassigned (added==true) or were assigned to a now defunct measProc (added==false)
   */
  def onMeasProcAppChanged(context: RequestContext, app: ApplicationInstance, added: Boolean)

}

