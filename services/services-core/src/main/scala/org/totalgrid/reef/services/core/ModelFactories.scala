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
package org.totalgrid.reef.services.core

import org.totalgrid.reef.services.framework.{ RequestContextSource }
import org.totalgrid.reef.services.coordinators.{ SingleThreadedMeasurementStreamCoordinator, SquerylBackedMeasurementStreamCoordinator }
import org.totalgrid.reef.services.{ DependenciesSource, ServiceDependencies }
import org.totalgrid.reef.measurementstore.MeasurementStore

class ModelFactories(measurementStore: MeasurementStore, contextSource: RequestContextSource) {

  def this(deps: ServiceDependencies) = this(deps.measurementStore, new DependenciesSource(deps))

  val accesses = new CommandLockServiceModel
  val userRequests = new UserCommandRequestServiceModel(accesses)
  val cmds = new CommandServiceModel(userRequests, accesses)
  accesses.setCommandModel(cmds)

  val coordinator = {
    // we have to make our own copies of the other service models to break the cyclic dependencies
    val measProc = new MeasurementProcessingConnectionServiceModel
    val fepModel = new CommunicationEndpointConnectionServiceModel
    val coord = new SquerylBackedMeasurementStreamCoordinator(measProc, fepModel, measurementStore)
    measProc.setCoordinator(coord)
    fepModel.setCoordinator(coord)
    val syncCoord = new SingleThreadedMeasurementStreamCoordinator(coord, contextSource)
    syncCoord
  }

  val fepConn = new CommunicationEndpointConnectionServiceModel
  val measProcConn = new MeasurementProcessingConnectionServiceModel

  measProcConn.setCoordinator(coordinator)
  fepConn.setCoordinator(coordinator)

  val fep = new FrontEndProcessorServiceModel(coordinator)
  val fepPort = new FrontEndPortServiceModel

  val overrides = new OverrideConfigServiceModel
  val triggerSets = new TriggerSetServiceModel
  val calculations = new CalculationConfigServiceModel
  val points = new PointServiceModel(triggerSets, overrides, calculations, measurementStore)

  val configFiles = new ConfigFileServiceModel
  val endpoints = new CommEndCfgServiceModel(cmds, configFiles, points, fepPort, coordinator)

  val alarms = new AlarmServiceModel
  val eventConfig = new EventConfigServiceModel
  val events = new EventServiceModel(eventConfig, alarms)
  alarms.eventModel = Some(events)

  val authTokens = new AuthTokenServiceModel
  val agents = new AgentServiceModel
  val permissionSets = new PermissionSetServiceModel

  val procStatus = new ProcessStatusServiceModel(coordinator)
  val appConfig = new ApplicationConfigServiceModel(procStatus)

  val edges = new EntityEdgeServiceModel
  val entities = new EntityServiceModel

  val attributes = new EntityAttributeServiceModel

}